package org.example.order.core.infra.jpa.querydsl.dsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Visitor;
import org.springframework.lang.Nullable;

import java.util.function.Function;

/**
 * 가독성 좋은 Where 절 생성을 위한 헬퍼
 * - null/boolean 조건에 따른 동적 and/or 조합 지원
 */
public class WhereClauseBuilder implements Predicate {

    private final BooleanBuilder delegate;

    public WhereClauseBuilder() {
        this.delegate = new BooleanBuilder();
    }

    public WhereClauseBuilder(Predicate predicate) {
        this.delegate = new BooleanBuilder(predicate);
    }

    public WhereClauseBuilder and(Predicate right) {
        return new WhereClauseBuilder(delegate.and(right));
    }

    public WhereClauseBuilder or(Predicate right) {
        return new WhereClauseBuilder(delegate.or(right));
    }

    public <V> WhereClauseBuilder optionalAnd(V value, LazyBooleanExpression lazyBooleanExpression) {
        return applyIfNotNull(value, this::and, lazyBooleanExpression);
    }

    public WhereClauseBuilder optionalAnd(boolean value, LazyBooleanExpression lazyBooleanExpression) {
        if (value) {
            return this.and(lazyBooleanExpression.get());
        }

        return this;
    }

    public <V> WhereClauseBuilder optionalOr(V value, LazyBooleanExpression lazyBooleanExpression) {
        return applyIfNotNull(value, this::or, lazyBooleanExpression);
    }

    @Override
    public Predicate not() {
        return new WhereClauseBuilder(delegate.not());
    }

    @Nullable
    @Override
    public <R, C> R accept(final Visitor<R, C> v, @Nullable final C context) {
        return delegate.accept(v, context);
    }

    @Override
    public Class<? extends Boolean> getType() {
        return Boolean.class;
    }

    private <V> WhereClauseBuilder applyIfNotNull(final V value,
                                                  final Function<Predicate, WhereClauseBuilder> func,
                                                  final LazyBooleanExpression lazyBooleanExpression) {
        if (value != null) {
            return func.apply(lazyBooleanExpression.get());
        }

        return this;
    }
}
