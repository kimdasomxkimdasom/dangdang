package com.dangdang.bid;

import com.dangdang.global.entity.BaseTimeEntity;
import com.dangdang.item.Item;
import com.dangdang.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(name = "idx_bid_item_price", columnList = "item_id, price"))
public class Bid extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bidder_id")
    private Member bidder;

    @Column(nullable = false)
    private long price;

    public Bid(Item item, Member bidder, long price) {
        this.item = item;
        this.bidder = bidder;
        this.price = price;
        item.getBids().add(this);
    }
}
