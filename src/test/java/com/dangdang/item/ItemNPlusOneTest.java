package com.dangdang.item;

import com.dangdang.TestcontainersConfiguration;
import com.dangdang.category.Category;
import com.dangdang.member.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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
        List<Item> items = itemRepository.findAll();               // 1번

        for (Item item : items) {
            item.getSeller().getNickname();                          // 판매자마다 1번 → N
            item.getImages().size();                                 // 상품마다 1번 → N
        }

        // 1 + 10 + 10 = 21
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + ITEM_COUNT * 2L);
    }

    // TODO [2주차] 아래 테스트를 통과시키기
    //  1) fetch join 으로 seller 함께 조회
    //  2) images(컬렉션)까지 fetch join + 페이징 하면 어떤 경고가 뜨는지 확인
    //  3) default_batch_fetch_size / @BatchSize 로 해결
    //  4) 목표: 쿼리 2~3번 이내
}
