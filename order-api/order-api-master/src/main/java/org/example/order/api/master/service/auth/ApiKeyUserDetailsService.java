package org.example.order.api.master.service.auth;

import org.example.order.api.master.security.ApiKeyUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyUserDetailsService implements UserDetailsService {
    // 이 부분은 실제 인증 키 값 관리 방식으로 교체 가능 (DB, ENV, Redis 등)
    private static final String STATIC_API_KEY = "dev-key-123";

    @Override
    public UserDetails loadUserByUsername(String apiKey) throws UsernameNotFoundException {
        if (!STATIC_API_KEY.equals(apiKey)) {
            throw new UsernameNotFoundException("Invalid API Key");
        }
        return new ApiKeyUserDetails(apiKey, "ROLE_API_CLIENT");
    }
}
