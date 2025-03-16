package by.baes.gatewayservice.config;

import com.example.auth.grpc.AuthServiceGrpc;
import com.example.auth.grpc.TokenRequest;
import com.example.auth.grpc.UserResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class GrpcAuthService {
    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceStub;

    public UserResponse validateToken(String token) {
        TokenRequest request = TokenRequest.newBuilder()
                .setToken(token)
                .build();
        return authServiceStub.validateToken(request);
    }
}
