package com.example.mp.gw.common.config;
//package com.uplus.mp.gw.common.config;
//
//
//import java.util.Arrays;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig extends WebSecurityConfigurerAdapter
//{
//	@Value("${spring.cors.allowed.origins}")
//	private String allowedOrigins;
//
//	@Value("${spring.cors.allowed.methods}")
//	private String allowedMethods;
//
//	@Override
//	public void configure(HttpSecurity http) throws Exception
//	{
//		http.authorizeRequests()
//			.antMatchers("/**")
//			.permitAll()
//			.and()
//			.cors()
//			.and()
//			.csrf().disable();
//	}
//
//	@Bean
//	public CorsConfigurationSource corsConfigurationSource()
//	{
//	    CorsConfiguration configuration = new CorsConfiguration();
//	    configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
//	    configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
//	    configuration.setAllowCredentials(true);
//	    configuration.addAllowedHeader("*");
//	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//	    source.registerCorsConfiguration("/**", configuration);
//	    return source;
//	}
//}
