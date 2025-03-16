package by.baes.gatewayservice.filter;

import by.baes.gatewayservice.config.GrpcAuthService;
import com.example.auth.grpc.AuthServiceGrpc;
import com.example.auth.grpc.TokenRequest;
import com.example.auth.grpc.UserResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TokenValidationFilter extends AbstractGatewayFilterFactory<TokenValidationFilter.Config> {

    private final GrpcAuthService grpcAuthService;

    public TokenValidationFilter(GrpcAuthService grpcAuthService) {
        super(Config.class);
        this.grpcAuthService = grpcAuthService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().toString();
            if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
                log.debug("Skipping token validation for open path: {}", path);
                return chain.filter(exchange);
            }

            String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (token == null || !token.startsWith("Bearer ")) {
                log.warn("No valid Bearer token found in request");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            token = token.substring(7);
            log.debug("Validating token: {}", token);

            try {
                UserResponse userResponse = grpcAuthService.validateToken(token);
                log.info("Token validated for user: {}", userResponse.getName());

                List<SimpleGrantedAuthority> authorities = userResponse.getRolesList().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userResponse.getName(), null, authorities
                );

                exchange.getRequest().mutate()
                        .header("X-User-Id", String.valueOf(userResponse.getId()))
                        .header("X-User-Name", userResponse.getName())
                        .header("X-User-Email", userResponse.getEmail())
                        .header("X-User-Roles", String.join(",", userResponse.getRolesList()))
                        .build();

                return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            } catch (Exception e) {
                log.error("Token validation failed: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    public static class Config {
    }
}