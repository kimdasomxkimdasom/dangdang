package com.dangdang.item;

import com.dangdang.bid.Bid;
import com.dangdang.category.Category;
import com.dangdang.global.entity.BaseTimeEntity;
import com.dangdang.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(name = "idx_item_status_end_at", columnList = "status, endAt"))
public class Item extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id")
    private Member seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    private String description;

    @Column(nullable = false)
    private long startPrice;

    @Column(nullable = false)
    private long currentPrice;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status;

    // 이미지는 상품 생명주기에 완전히 종속 → cascade + orphanRemoval
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc")
    private List<ItemImage> images = new ArrayList<>();

    // 입찰은 독립적으로 저장되므로 cascade 없이 조회용으로만 매핑
    @OneToMany(mappedBy = "item")
    private List<Bid> bids = new ArrayList<>();

    // TODO [4주차] 동시 입찰 실습: @Version 낙관적 락 vs 비관적 락 비교

    public Item(Member seller, Category category, String title, String description,
                long startPrice, LocalDateTime endAt) {
        this.seller = seller;
        this.category = category;
        this.title = title;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.endAt = endAt;
        this.status = ItemStatus.BIDDING;
    }

    public void addImage(String url) {
        images.add(new ItemImage(this, url, images.size()));
    }

    /**
     * 입찰가 갱신. 동시성 제어 없이 작성된 상태 → [4주차]에서 동시 입찰 테스트로 깨지는 걸 먼저 확인
     */
    public void updateCurrentPrice(long bidPrice, LocalDateTime now) {
        if (status != ItemStatus.BIDDING || now.isAfter(endAt)) {
            throw new IllegalStateException("입찰이 마감된 상품입니다.");
        }
        if (bidPrice <= currentPrice) {
            throw new IllegalArgumentException("현재가보다 높은 금액으로 입찰해야 합니다.");
        }
        this.currentPrice = bidPrice;
    }
}
