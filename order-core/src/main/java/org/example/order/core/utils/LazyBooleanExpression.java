package org.example.order.core.utils;

import com.querydsl.core.types.dsl.BooleanExpression;

@FunctionalInterface
public interface LazyBooleanExpression {
    BooleanExpression get();
}
