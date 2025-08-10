package org.example.order.api.common.autoconfigure.condition;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Web 공통 설정 존재 시 자동구성 활성
 * - order.api.infra.web.* 가 존재하거나
 * - logging/format/security 중 하나라도 설정이 존재하거나
 * - order.api.infra.web.enabled=true 가 명시된 경우
 */
public class WebConfigPresentCondition implements Condition {
    @Override
    public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata md) {
        var env = ctx.getEnvironment();
        var binder = Binder.get(env);

        boolean webPresent = binder.bind("order.api.infra.web", Bindable.mapOf(String.class, Object.class))
                .map(m -> !m.isEmpty()).orElse(false);

        boolean loggingPresent = binder.bind("order.api.infra.logging", Bindable.mapOf(String.class, Object.class))
                .map(m -> !m.isEmpty()).orElse(false);

        boolean formatPresent = binder.bind("order.api.infra.format", Bindable.mapOf(String.class, Object.class))
                .map(m -> !m.isEmpty()).orElse(false);

        boolean securityPresent = binder.bind("order.api.infra.security", Bindable.mapOf(String.class, Object.class))
                .map(m -> !m.isEmpty()).orElse(false);

        boolean explicitlyEnabled = "true".equalsIgnoreCase(env.getProperty("order.api.infra.web.enabled"));

        return webPresent || loggingPresent || formatPresent || securityPresent || explicitlyEnabled;
    }
}
