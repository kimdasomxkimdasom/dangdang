package com.dangdang.item;

import com.dangdang.TestcontainersConfiguration;
import com.dangdang.category.Category;
import com.dangdang.member.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Transactional
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ItemNPlusOneTest {

    private static final int ITEM_COUNT = 10;

    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired ItemRepository itemRepository;

    Statistics statistics;

    @BeforeEach
    void setUp() {
        Category category = new Category("노트북", null);
        em.persist(category);

        for (int i = 0; i < ITEM_COUNT; i++) {
            Member seller = new Member("seller" + i + "@test.com", "판매자" + i);
            em.persist(seller);

            Item item = new Item(seller, category, "맥북 " + i, "설명", 100_000,
                    LocalDateTime.now().plusDays(1));
            item.addImage("https://img/" + i + "/0.jpg");
            item.addImage("https://img/" + i + "/1.jpg");
            em.persist(item);
        }
        em.flush();
        em.clear();   // 영속성 컨텍스트를 비워야 실제 조회 쿼리가 나감

        statistics = emf.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    @DisplayName("[문제 재현] 상품 목록에서 판매자/이미지 접근 시 N+1 발생")
    void reproduceNPlusOne() {
        List<Item> items = itemRepository.findAll(); // 1번

        // 실제로 판매자/이미지를 사용하는 상황을 흉내 (화면에 그리기, DTO 변환 등)
        for (Item item : items) {
            item.getSeller().getNickname(); // 판매자마다 1번 -> N
            item.getImages().size(); // 상품마다 1번 -> N
        }

        log.debug("[N+1] 실행된 쿼리 수 = {}", statistics.getPrepareStatementCount());

        // 1 + 10 + 10 = 21
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + ITEM_COUNT * 2L);
    }

    @Test
    @DisplayName("[해결] fetch join 으로 판매자/이미지를 한 번에 조회")
    void solveWithFetchJoin() {
        List<Item> items = itemRepository.findAllWithSellerAndImages(); // 여기서 1번

        // 실제로 판매자/이미지를 사용하는 상황을 흉내 (화면에 그리기, DTO 변환 등)
        for (Item item : items) {
            item.getSeller().getNickname(); // 이미 채워져 있어 쿼리 없음
            item.getImages().size(); // 이미 채워져 있어 쿼리 없음
        }

        log.debug("[fetch join] 실행된 쿼리 수 = {}", statistics.getPrepareStatementCount());

        // 이미지 join 으로 행이 20개가 되지만, Hibernate 6+ 는 상품 중복을 자동 제거
        assertThat(items).hasSize(ITEM_COUNT);
        // 21번 -> 1번 해결 
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    // TODO [2주차] 남은 실습
    //  1) images(컬렉션)까지 fetch join + 페이징 하면 어떤 경고가 뜨는지 확인
    //  2) default_batch_fetch_size / @BatchSize 로 해결
    //  3) 목표: 페이징 가능 + 쿼리 2~3번 이내
}
