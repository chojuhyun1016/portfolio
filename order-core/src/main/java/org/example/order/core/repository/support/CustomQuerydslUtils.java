package org.example.order.core.repository.support;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import org.hibernate.query.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.Querydsl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class CustomQuerydslUtils {
    public static final int DEFAULT_BATCH_SIZE = 10000;

    public static <E> Page<E> page(final Querydsl querydsl, final JPQLQuery<E> query, final Pageable pageable) {
        if (querydsl == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0L);
        }

        long total = query.fetchCount();
        final List<E> list = querydsl.applyPagination(pageable, query).fetch();

        return new PageImpl<>(list, pageable, total);
    }

    public static <T> Stream<T> stream(final JPQLQuery<T> query) {
        return ((JPAQuery<T>) query).createQuery().unwrap(Query.class).getResultStream();
    }

    public static WhereClauseBuilder where() {
        return new WhereClauseBuilder();
    }
}
