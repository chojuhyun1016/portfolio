package org.example.order.core.infra.querydsl.dsl;

import com.querydsl.core.types.dsl.BooleanExpression;

@FunctionalInterface
public interface LazyBooleanExpression {
    BooleanExpression get();
}
