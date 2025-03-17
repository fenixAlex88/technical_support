package by.baes.gatewayservice.filter;

import by.baes.gatewayservice.config.GrpcAuthService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TokenValidationFilter extends AbstractGatewayFilterFactory<TokenValidationFilter.Config> {

    private final GrpcAuthService grpcAuthService;

    public TokenValidationFilter(GrpcAuthService grpcAuthService) {
        super(Config.class);
        this.grpcAuthService = grpcAuthService;
        log.info("TokenValidationFilter initialized");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().toString();

            log.info("Request path: {}", path);

            // Пропускаем исключенные пути
            List<String> excludePaths = parseConfigList(config.getExcludePaths());
            if (!excludePaths.isEmpty() && excludePaths.stream().anyMatch(path::startsWith)) {
                log.info("Excluding path: {}", path);
                return chain.filter(exchange);
            }

            String token = extractToken(request);
            if (token == null) {
                log.warn("Token not found");
                return handleUnauthorized(exchange, HttpStatus.UNAUTHORIZED);
            }

            return grpcAuthService.validateToken(token)
                    .flatMap(claims -> {
                        log.info("Token validated, claims: {}", claims);

                        // Проверяем роли, если они указаны
                        List<String> requiredRoles = parseConfigList(config.getRoles());
                        if (!requiredRoles.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            List<String> userRoles = (List<String>) claims.get("roles");
                            if (userRoles == null || requiredRoles.stream()
                                    .noneMatch(userRoles::contains)) {
                                log.warn("User roles {} do not match required roles {}", userRoles, requiredRoles);
                                return handleUnauthorized(exchange, HttpStatus.FORBIDDEN);
                            }
                        }

                        // Добавляем claims в заголовки
                        ServerHttpRequest modifiedRequest = addClaimsToHeaders(request, claims);
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    })
                    .onErrorResume(e -> {
                        log.warn("Token validation failed: {}", e.getMessage());
                        return handleUnauthorized(exchange, HttpStatus.UNAUTHORIZED);
                    });
        };
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private ServerHttpRequest addClaimsToHeaders(ServerHttpRequest request, Map<String, Object> claims) {
        ServerHttpRequest.Builder modifiedRequest = request.mutate();
        for (String claimKey : claims.keySet()) {
            modifiedRequest.header("X-Auth-" + claimKey, claims.get(claimKey).toString());
        }
        return modifiedRequest.build();
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    // Вспомогательный метод для обработки строки конфигурации
    private List<String> parseConfigList(String configValue) {
        if (configValue == null || configValue.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(configValue.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Setter
    @Getter
    public static class Config {
        private String excludePaths;
        private String roles;
    }
}