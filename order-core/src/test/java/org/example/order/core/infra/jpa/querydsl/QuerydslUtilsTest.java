package org.example.order.core.infra.jpa.querydsl;

import org.example.order.core.infra.jpa.querydsl.builder.QuerydslUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class QuerydslUtilsTest {

    @Test
    @DisplayName("page(): Querydsl 인스턴스가 null이면 빈 페이지 반환")
    void page_shouldReturnEmptyWhenQuerydslNull() {
        var pageable = PageRequest.of(0, 10);
        var page = QuerydslUtils.page(null, null, pageable);

        assertThat(page).isInstanceOf(PageImpl.class);
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("DEFAULT_BATCH_SIZE는 합리적인 기본값(>0)을 가진다")
    void defaultBatchSize_positive() {
        assertThat(QuerydslUtils.DEFAULT_BATCH_SIZE).isGreaterThan(0);
    }
}
