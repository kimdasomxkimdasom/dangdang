package com.dangdang.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long>, ItemRepositoryCustom {

    // 판매자(N:1)와 이미지(1:N)를 join 으로 한 번에 조회 → N+1 해결
    @Query("select i from Item i join fetch i.seller left join fetch i.images")
    List<Item> findAllWithSellerAndImages();
}
