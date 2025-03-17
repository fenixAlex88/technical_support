package by.baes.gatewayservice.service;

import by.baes.auth.grpc.AuthServiceGrpc;
import by.baes.auth.grpc.TokenRequest;
import by.baes.auth.grpc.UserResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class GrpcAuthService {

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceStub;

    public Mono<Map<String, Object>> validateToken(String token) {
        return Mono.fromCallable(() -> {
            TokenRequest request = TokenRequest.newBuilder()
                    .setToken(token)
                    .build();
            UserResponse response = authServiceStub.validateToken(request);

            // Преобразуем UserResponse в Map
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", response.getId());
            claims.put("name", response.getName());
            claims.put("email", response.getEmail());
            claims.put("roles", response.getRolesList());
            return claims;
        }).onErrorResume(e -> Mono.error(new RuntimeException("Token validation failed: " + e.getMessage())));
    }
}
