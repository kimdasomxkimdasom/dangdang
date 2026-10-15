package com.dangdang.category;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 셀프 참조 계층 구조 (디지털 > 노트북 > 맥북)
 * [3주차] "디지털" 로 검색하면 하위 카테고리 상품까지 나오게 하기
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> children = new ArrayList<>();

    public Category(String name, Category parent) {
        this.name = name;
        if (parent != null) {
            parent.addChild(this);
        }
    }

    // 연관관계 편의 메서드: 양쪽을 항상 같이 세팅
    private void addChild(Category child) {
        this.children.add(child);
        child.parent = this;
    }
}
