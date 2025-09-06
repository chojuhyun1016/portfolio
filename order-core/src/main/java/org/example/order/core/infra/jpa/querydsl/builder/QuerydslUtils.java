package org.example.order.core.infra.jpa.querydsl.builder;

import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import org.example.order.core.infra.jpa.querydsl.dsl.WhereClauseBuilder;
import org.hibernate.query.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.Querydsl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * QueryDSL 보조 유틸리티
 */
public class QuerydslUtils {

    public static final int DEFAULT_BATCH_SIZE = 10000;

    /**
     * Querydsl 5.x에서 fetchCount()가 제거됨에 따라, count 쿼리를 별도로 생성하여 total을 구한다.
     * - count 전용 쿼리를 새로 만들어 실행
     * - Page 데이터는 기존 query 에 pageable 적용 후 fetch
     */
    public static <E> Page<E> page(final Querydsl querydsl, final JPQLQuery<E> query, final Pageable pageable) {
        if (querydsl == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0L);
        }

        Long total = query.select(Wildcard.count).fetchFirst();
        long totalElements = (total == null) ? 0L : total;

        final List<E> list = querydsl.applyPagination(pageable, query).fetch();

        return new PageImpl<>(list, pageable, totalElements);
    }

    /**
     * 하이버네이트 스트림을 활용해 결과를 스트리밍으로 처리
     */
    public static <T> Stream<T> stream(final JPQLQuery<T> query) {
        return ((JPAQuery<T>) query).createQuery().unwrap(Query.class).getResultStream();
    }

    /**
     * Where 절을 유연하게 작성하기 위한 빌더 시작점
     */
    public static WhereClauseBuilder where() {
        return new WhereClauseBuilder();
    }
}
