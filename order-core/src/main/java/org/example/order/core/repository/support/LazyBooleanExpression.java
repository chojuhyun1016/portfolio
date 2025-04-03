package org.example.order.core.repository.support;

import com.querydsl.core.types.dsl.BooleanExpression;

@FunctionalInterface
public interface LazyBooleanExpression {
    BooleanExpression get();
}
