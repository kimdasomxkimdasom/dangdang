package com.dangdang.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long>, ItemRepositoryCustom {

    // 판매자(N:1)와 이미지(1:N)를 join 으로 한 번에 조회 → N+1 해결
    @Query("select i from Item i join fetch i.seller left join fetch i.images")
    List<Item> findAllWithSellerAndImages();

    // [문제 재현용] 컬렉션(images) fetch join + 페이징 → DB 가 아닌 메모리에서 페이징됨
    // count 쿼리에는 fetch join 을 쓸 수 없어서 countQuery 를 따로 지정
    @Query(value = "select i from Item i join fetch i.seller left join fetch i.images",
            countQuery = "select count(i) from Item i")
    Page<Item> findPageWithSellerAndImages(Pageable pageable);

    // [권장 조합] N:1(판매자)만 fetch join → 행 수가 늘지 않아 DB 에서 바로 limit
    // 컬렉션(images, bids)은 default_batch_fetch_size 로 in 쿼리 조회
    @Query(value = "select i from Item i join fetch i.seller",
            countQuery = "select count(i) from Item i")
    Page<Item> findPageWithSeller(Pageable pageable);
}
