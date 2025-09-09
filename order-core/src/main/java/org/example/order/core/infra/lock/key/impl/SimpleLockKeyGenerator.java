package org.example.order.core.infra.lock.key.impl;

import org.example.order.core.infra.lock.key.LockKeyGenerator;

import java.lang.reflect.Method;

/**
 * SimpleLockKeyGenerator
 * - 리터럴만으로 구성된 표현식("'a' + 'b' + 'c'")을 실제 문자열로 평가
 * - 그 외 표현식은 원문 그대로 반환 (호환성 유지)
 * 예) "order:42" -> 그대로, "'order:' + '42'" -> "order:42"
 */
public class SimpleLockKeyGenerator implements LockKeyGenerator {

    @Override
    public String generate(String expr, Method method, Object[] args) {
        if (expr == null) {
            return null;
        }

        // 리터럴 결합 패턴("'..' + '..' + ..")이면 안전하게 해석
        String trimmed = expr.trim();

        if (trimmed.matches("^'(?:[^'\\\\]|\\\\.)*'(\\s*\\+\\s*'(?:[^'\\\\]|\\\\.)*')*$")) {
            String[] parts = trimmed.split("\\s*\\+\\s*");
            StringBuilder sb = new StringBuilder();

            for (String p : parts) {
                if (p.length() >= 2 && p.startsWith("'") && p.endsWith("'")) {
                    sb.append(p, 1, p.length() - 1);
                } else {
                    // 섞여있으면 원문 유지
                    return expr;
                }
            }
            return sb.toString();
        }

        // 그대로 반환(기존 동작 유지)
        return expr;
    }
}
