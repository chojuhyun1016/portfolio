package org.example.order.api.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 권장안 A' - 게이트웨이 종결형 최소 보안
 * - 모든 요청 permitAll (게이트웨이에서 인증/인가 완료 가정)
 * - Basic/Form/Logout/CSRF/세션 비활성
 * - 애플리케이션 내 SecurityFilterChain "단 1개"만 유지
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 미사용
                .csrf(c -> c.disable())

                // 이 체인을 모든 요청에 적용
                .securityMatcher(request -> true)

                // 모두 허용 (게이트웨이가 인증/인가를 책임진다는 전제)
                .authorizeHttpRequests(reg -> reg.anyRequest().permitAll())

                // 기본 인증/폼/로그아웃 전부 비활성
                .httpBasic(c -> c.disable())
                .formLogin(c -> c.disable())
                .logout(c -> c.disable())

                // 완전 무세션 (필요하면 .sessionCreationPolicy(STATELESS)로 변경 가능)
                .sessionManagement(sm -> sm.disable())

                // 혹시라도 기본 엔트리포인트 타는 경우를 차단 (실제로는 permitAll이라 안 탐)
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint((req, res, e) -> res.setStatus(200))
                );

        return http.build();
    }
}
