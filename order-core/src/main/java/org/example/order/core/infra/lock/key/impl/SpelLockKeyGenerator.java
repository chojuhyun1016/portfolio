package org.example.order.core.infra.lock.key.impl;

import org.example.order.core.infra.lock.key.LockKeyGenerator;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

public class SpelLockKeyGenerator implements LockKeyGenerator {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Override
    public String generate(String keyExpression, Method method, Object[] args) {
        String[] paramNames = discoverer.getParameterNames(method);
        EvaluationContext context = new StandardEvaluationContext();

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }
}
