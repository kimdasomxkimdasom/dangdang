package com.dangdang.item;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.dangdang.item.QItem.item;

@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 동적 검색의 기본 형태만 구현. 나머지 조건은 [3주차] 실습에서 채우기
     */
    @Override
    public Page<Item> search(ItemSearchCondition cond, Pageable pageable) {
        List<Item> content = queryFactory
                .selectFrom(item)
                .where(
                        titleContains(cond.keyword()),
                        statusEq(cond.status())
                        // TODO [3주차] categoryId(하위 포함), 가격 범위, 마감 임박 조건
                )
                .orderBy(item.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // count 쿼리는 필요할 때만 실행됨 (마지막 페이지 등에서는 생략)
        var countQuery = queryFactory
                .select(item.count())
                .from(item)
                .where(
                        titleContains(cond.keyword()),
                        statusEq(cond.status())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression titleContains(String keyword) {
        return (keyword == null || keyword.isBlank()) ? null : item.title.contains(keyword);
    }

    private BooleanExpression statusEq(ItemStatus status) {
        return status == null ? null : item.status.eq(status);
    }
}
