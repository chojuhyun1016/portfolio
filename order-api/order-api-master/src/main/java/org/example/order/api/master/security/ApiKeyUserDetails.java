package org.example.order.api.master.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * API Key 인증 전용 사용자 상세 정보 객체
 * server-to-server 통신, 로그인 없이 api-key 인증
 */
@Getter
@AllArgsConstructor
public class ApiKeyUserDetails implements UserDetails {

    private final String apiKey;
    private final List<GrantedAuthority> authorities;

    public ApiKeyUserDetails(String apiKey, String role) {
        this.apiKey = apiKey;
        this.authorities = List.of(new SimpleGrantedAuthority(role));
    }

    @Override public String getUsername() { return apiKey; }
    @Override public String getPassword() { return ""; }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
