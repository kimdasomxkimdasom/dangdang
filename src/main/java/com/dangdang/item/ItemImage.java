package com.dangdang.item;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private int sortOrder;   // 0 = 대표 이미지

    ItemImage(Item item, String url, int sortOrder) {
        this.item = item;
        this.url = url;
        this.sortOrder = sortOrder;
    }
}
