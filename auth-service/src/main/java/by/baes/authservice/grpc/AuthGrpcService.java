package by.baes.authservice.grpc;

import by.baes.authservice.entity.Role;
import by.baes.authservice.entity.User;
import by.baes.authservice.service.AuthService;
import com.example.auth.grpc.AuthServiceGrpc;
import com.example.auth.grpc.TokenRequest;
import com.example.auth.grpc.UserResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.stream.Collectors;

@GrpcService
@Slf4j
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {
    private final AuthService authService;

    public AuthGrpcService(AuthService authService) {
        this.authService = authService;
        log.info("AuthGrpcService initialized");
    }

    @Override
    public void validateToken(TokenRequest request, StreamObserver<UserResponse> responseObserver) {
        log.debug("Received gRPC validateToken request");
        try {
            User user = authService.validateToken(request.getToken());

            UserResponse response = UserResponse.newBuilder()
                    .setId(user.getId())
                    .setName(user.getName())
                    .setEmail(user.getEmail() != null ? user.getEmail() : "")
                    .setTelegramId(user.getTelegramId() != null ? user.getTelegramId() : "")
                    .addAllRoles(user.getRoles().stream()
                            .map(Role::getName)
                            .collect(Collectors.toList()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Token validated via gRPC for user: {}", user.getName());
        } catch (Exception e) {
            log.error("Error validating token via gRPC: {}", e.getMessage());
            Status status = Status.INVALID_ARGUMENT.withDescription(e.getMessage());
            responseObserver.onError(status.asRuntimeException());
        }
    }
}