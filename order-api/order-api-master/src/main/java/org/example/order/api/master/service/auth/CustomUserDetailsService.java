package org.example.order.api.master.service.auth;

import org.example.order.api.common.auth.ClientRole;
import org.example.order.api.master.auth.CustomUserDetails;
import org.example.order.api.master.config.ApiKeyProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@EnableConfigurationProperties({ApiKeyProperties.class})
public class CustomUserDetailsService implements UserDetailsService {
    private Map<String, UserDetails> store;

    public CustomUserDetailsService(ApiKeyProperties apiKeyProperties) {
        this.store = Map.of(apiKeyProperties.getKey(), new CustomUserDetails(apiKeyProperties.getKey(), ClientRole.ROLE_CLIENT.getCode()));
    }

    @Override
    public UserDetails loadUserByUsername(String apiKey) throws UsernameNotFoundException {
        return Optional.ofNullable(store.get(apiKey)).orElseThrow(() -> new UsernameNotFoundException("Invalid API key: " + apiKey));
    }
}
