package com.dangdang.wish;

import com.dangdang.global.entity.BaseTimeEntity;
import com.dangdang.item.Item;
import com.dangdang.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Member N:M Item 을 @ManyToMany 대신 중간 엔티티로 풀어낸 형태
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_wish_member_item", columnNames = {"member_id", "item_id"}))
public class Wish extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wish_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    public Wish(Member member, Item item) {
        this.member = member;
        this.item = item;
    }
}
