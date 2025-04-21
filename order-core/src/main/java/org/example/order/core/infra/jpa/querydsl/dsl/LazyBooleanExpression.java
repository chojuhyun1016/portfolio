package org.example.order.core.infra.jpa.querydsl.dsl;

import com.querydsl.core.types.dsl.BooleanExpression;

@FunctionalInterface
public interface LazyBooleanExpression {
    BooleanExpression get();
}
