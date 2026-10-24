package com.dangdang.item;

import com.dangdang.TestcontainersConfiguration;
import com.dangdang.bid.Bid;
import com.dangdang.category.Category;
import com.dangdang.member.Member;
import com.dangdang.support.SqlCaptor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.loader.MultipleBagFetchException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * fetch join 으로 못 푸는 경우(컬렉션 2개)를 default_batch_fetch_size 로 해결.
 * batch size 를 전역으로 켜면 ItemNPlusOneTest 의 "N+1 재현" 쿼리 수가 바뀌므로 이 클래스에서만 켠다.
 */
@Slf4j
@Transactional
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_batch_fetch_size=" + ItemBatchFetchTest.BATCH_SIZE)
class ItemBatchFetchTest {

    private static final int ITEM_COUNT = 10;
    private static final int BID_PER_ITEM = 3;
    static final int BATCH_SIZE = 100;

    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired ItemRepository itemRepository;

    Statistics statistics;

    Category category;
    Member bidder;

    @BeforeEach
    void setUp() {
        category = new Category("노트북", null);
        em.persist(category);

        bidder = new Member("bidder@test.com", "입찰자");
        em.persist(bidder);

        createItems(0, ITEM_COUNT);
        flushAndClear();
    }

    /** 상품마다 판매자 1명, 이미지 2장, 입찰 BID_PER_ITEM 건 */
    private void createItems(int from, int to) {
        for (int i = from; i < to; i++) {
            Member seller = new Member("seller" + i + "@test.com", "판매자" + i);
            em.persist(seller);

            Item item = new Item(seller, category, "맥북 " + i, "설명", 100_000,
                    LocalDateTime.now().plusDays(1));
            item.addImage("https://img/" + i + "/0.jpg");
            item.addImage("https://img/" + i + "/1.jpg");
            em.persist(item);

            // Item.bids 에는 cascade 가 없어서 입찰은 따로 저장
            for (int b = 1; b <= BID_PER_ITEM; b++) {
                em.persist(new Bid(item, bidder, 100_000L + b * 1_000L));
            }
        }
    }

    private void flushAndClear() {
        em.flush();
        em.clear();

        statistics = emf.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        SqlCaptor.clear();
    }

    @Test
    @DisplayName("[문제] List 컬렉션 2개(images, bids)를 동시에 fetch join 하면 MultipleBagFetchException")
    void fetchJoinTwoBags() {
        // repository 의 @Query 로 만들면 애플리케이션 시작 시 검증하다 컨텍스트가 뜨지 않으므로 테스트 안에서 직접 생성
        assertThatThrownBy(() -> em.createQuery(
                        "select i from Item i join fetch i.seller left join fetch i.images left join fetch i.bids",
                        Item.class)
                .getResultList())
                .satisfies(e -> assertThat(rootCause(e)).isInstanceOf(MultipleBagFetchException.class));
    }

    @Test
    @DisplayName("[해결] batch size 로 판매자/이미지/입찰을 각각 in 쿼리 한 번씩 조회")
    void solveWithBatchSize() {
        List<Item> items = itemRepository.findAll(); // 상품 1번

        for (Item item : items) {
            item.getSeller().getNickname(); // 첫 접근 때 판매자 10명을 in 으로 한 번에
            item.getImages().size(); // 첫 접근 때 상품 10개의 이미지를 한 번에
            item.getBids().size(); // 첫 접근 때 상품 10개의 입찰을 한 번에
        }

        SqlCaptor.sqls().forEach(sql -> log.debug("[batch size] SQL = {}", sql));

        // 지연 로딩 그대로면 1 + 10 + 10 + 10 = 31번 -> 1 + 1 + 1 + 1 = 4번
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
        assertThat(items).allSatisfy(it -> {
            assertThat(it.getImages()).hasSize(2);
            assertThat(it.getBids()).hasSize(BID_PER_ITEM);
        });
    }

    @Test
    @DisplayName("[batch size 초과] 상품 150개면 판매자/이미지/입찰이 각각 100 + 50 두 번으로 나뉨")
    void exceedBatchSize() {
        createItems(ITEM_COUNT, 150);
        flushAndClear();

        List<Item> items = itemRepository.findAll();
        for (Item item : items) {
            item.getSeller().getNickname();
            item.getImages().size();
            item.getBids().size();
        }

        SqlCaptor.sqls().forEach(sql -> log.debug("[batch 초과] SQL = {}", sql));

        // 상품 1 + (판매자 2 + 이미지 2 + 입찰 2) = 7번. 지연 로딩이었다면 1 + 150 x 3 = 451번
        // 두 번째 in 쿼리는 id 가 50개인데도 ? 가 100개 (Hibernate 6+ 는 남는 자리를 null 로 채워
        // in 절 길이를 batch size 로 고정 → SQL 문자열이 하나라 PreparedStatement 캐시 재사용)
        assertThat(items).hasSize(150);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("[권장 조합] 판매자는 fetch join, 컬렉션은 batch size → 페이징이 DB limit 로 처리되고 쿼리 4번")
    void fetchJoinToOneWithBatchSizePaging() {
        Page<Item> page = itemRepository.findPageWithSeller(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id")));

        for (Item item : page.getContent()) {
            item.getSeller().getNickname(); // 이미 fetch join 으로 로딩됨 → 추가 쿼리 없음
            item.getImages().size();
            item.getBids().size();
        }

        SqlCaptor.sqls().forEach(sql -> log.debug("[권장 조합] SQL = {}", sql));

        String contentSql = SqlCaptor.sqls().getFirst();
        // 상품+판매자 1 + count 1 + 이미지 1 + 입찰 1 = 4번
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
        assertThat(contentSql).containsIgnoringCase("limit");
        assertThat(page.getTotalElements()).isEqualTo(ITEM_COUNT);
        assertThat(page.getContent()).hasSize(5)
                .allSatisfy(it -> {
                    assertThat(it.getImages()).hasSize(2);
                    assertThat(it.getBids()).hasSize(BID_PER_ITEM);
                });
    }

    private static Throwable rootCause(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
