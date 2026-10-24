package com.dangdang.support;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.ArrayList;
import java.util.List;

/**
 * Hibernate 가 실행하기 직전의 SQL 을 가로채서 모아두는 테스트용 도구.
 * 쿼리 "수"는 Statistics 로, 쿼리 "내용"(limit 유무 등)은 이걸로 검증한다.
 * application.yml 의 hibernate.session_factory.statement_inspector 에 등록해서 사용
 */
public class SqlCaptor implements StatementInspector {

    private static final List<String> SQLS = new ArrayList<>();

    @Override
    public String inspect(String sql) {
        SQLS.add(sql);
        return sql;
    }

    public static List<String> sqls() {
        return List.copyOf(SQLS);
    }

    public static void clear() {
        SQLS.clear();
    }
}
