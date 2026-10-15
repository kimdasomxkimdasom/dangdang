package com.dangdang.item;

/**
 * 상품 검색 조건. 모든 필드는 선택값(null 이면 조건 무시)
 */
public record ItemSearchCondition(
        String keyword,
        Long categoryId,       // [3주차] 하위 카테고리 포함 검색
        Long minPrice,
        Long maxPrice,
        ItemStatus status,
        Boolean endingSoon     // 24시간 이내 마감
) {
}
