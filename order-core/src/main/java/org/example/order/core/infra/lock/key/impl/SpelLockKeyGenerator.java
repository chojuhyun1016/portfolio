package org.example.order.core.infra.lock.key.impl;

import org.example.order.core.infra.lock.key.LockKeyGenerator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;

/**
 * SpEL 기반 키 생성기
 * - MethodBasedEvaluationContext 를 사용해 #p0/#a0/매개변수명 바인딩 지원
 * - Integration/Aspect 경로와 동일한 바인딩 전략을 사용하여 테스트/운영 일관성 확보
 */
public class SpelLockKeyGenerator implements LockKeyGenerator {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Override
    public String generate(String expr, Method method, Object[] args) {
        if (expr == null) {
            return null;
        }

        Expression e = PARSER.parseExpression(expr);

        if (method == null) {
            return e.getValue(String.class);
        }

        MethodBasedEvaluationContext ctx =
                new MethodBasedEvaluationContext(null, method, args, NAME_DISCOVERER);

        return e.getValue(ctx, String.class);
    }
}
