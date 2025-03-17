package by.baes.authservice.grpc;

import by.baes.auth.grpc.AuthServiceGrpc;
import by.baes.auth.grpc.TokenRequest;
import by.baes.auth.grpc.UserResponse;
import by.baes.authservice.dto.UserDto;
import by.baes.authservice.service.AuthService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {
    private final AuthService authService;

    @Override
    public void validateToken(TokenRequest request, StreamObserver<UserResponse> responseObserver) {
        log.debug("Received gRPC validateToken request for token: {}", request.getToken());
        try {
            UserDto userDto = authService.validateToken(request.getToken());

            UserResponse response = UserResponse.newBuilder()
                    .setId(userDto.getId())
                    .setName(userDto.getName())
                    .setEmail(userDto.getEmail() != null ? userDto.getEmail() : "")
                    .setTelegramId(userDto.getTelegramId() != null ? userDto.getTelegramId() : "")
                    .addAllRoles(userDto.getRoles())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Token validated via gRPC for user: {}", userDto.getName());
        } catch (Exception e) {
            log.error("Error validating token via gRPC: {}", e.getMessage());
            Status status = Status.INVALID_ARGUMENT.withDescription(e.getMessage());
            responseObserver.onError(status.asRuntimeException());
        }
    }
}