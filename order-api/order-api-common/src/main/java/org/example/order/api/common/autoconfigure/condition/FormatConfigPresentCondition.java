package org.example.order.api.common.autoconfigure.condition;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 포맷 설정 존재 시 자동구성 활성
 * - order.api.infra.format.* 중 하나라도 존재하거나
 * - order.api.infra.format.enabled=true 가 명시된 경우
 */
public class FormatConfigPresentCondition implements Condition {
    @Override
    public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata md) {
        var env = ctx.getEnvironment();
        var binder = Binder.get(env);
        var bound = binder.bind("order.api.infra.format", Bindable.mapOf(String.class, Object.class));

        boolean present = bound.isBound() && !bound.get().isEmpty();
        boolean explicitlyEnabled = "true".equalsIgnoreCase(env.getProperty("order.api.infra.format.enabled"));

        return present || explicitlyEnabled;
    }
}
