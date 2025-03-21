package org.example.order.core.utils;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Visitor;

import javax.annotation.Nullable;
import java.util.function.Function;

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

    public <V> WhereClauseBuilder optionalAnd(boolean value, LazyBooleanExpression lazyBooleanExpression) {
        if (value) {
            return new WhereClauseBuilder(this.and(lazyBooleanExpression.get()));
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
        return this.getType();
    }

    private <V> WhereClauseBuilder applyIfNotNull(final V value, final Function<Predicate, WhereClauseBuilder> func, final LazyBooleanExpression lazyBooleanExpression) {
        if (value != null) {
            return new WhereClauseBuilder(func.apply(lazyBooleanExpression.get()));
        }

        return this;
    }
}