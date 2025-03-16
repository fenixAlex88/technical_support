package by.baes.gatewayservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@Slf4j
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        log.info("Configuring Spring Security for Gateway");
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/auth/login", "/api/auth/register", "/api/auth/logout").permitAll()
                        .anyExchange().hasRole("ADMIN")
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, ex) -> {
                            log.warn("Authentication failed for path: {}", exchange.getRequest().getPath());
                            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, ex) -> {
                            return exchange.getPrincipal()
                                    .cast(Authentication.class) // Приводим Principal к Authentication
                                    .doOnNext(auth -> log.warn("Access denied for path: {} by user: {} with roles: {}",
                                            exchange.getRequest().getPath(), auth.getName(), auth.getAuthorities()))
                                    .then(Mono.fromRunnable(() -> {
                                        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
                                    }))
                                    .then(exchange.getResponse().setComplete());
                        })
                );
        return http.build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.withUsername("placeholder")
                .password("{noop}placeholder")
                .roles("USER")
                .build();
        return new MapReactiveUserDetailsService(user);
    }
}