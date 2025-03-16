package by.baes.authservice.controller;

import by.baes.authservice.dto.AuthResponse;
import by.baes.authservice.dto.LoginRequest;
import by.baes.authservice.dto.RegisterRequest;
import by.baes.authservice.dto.UserDto;
import by.baes.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
        log.info("AuthController initialized");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Received login request for user: {}", request.getName());
        String token = authService.login(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("Received register request for user: {}", request.getName());
        String token = authService.register(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        log.debug("Received logout request");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
            log.info("Logout processed successfully");
            return ResponseEntity.ok().build();
        }
        log.warn("Invalid Authorization header for logout");
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/roles")
    public ResponseEntity<List<String>> getAllRoles() {
        log.debug("Received request to get all roles");
        List<String> roles = authService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "asc") String sort,
            @RequestParam(required = false) List<String> roles) {
        log.debug("Received request to get users with page: {}, size: {}, sort: {}, roles: {}", page, size, sort, roles);
        Page<UserDto> users = authService.getAllUsers(page, size, sort, roles);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        log.debug("Received request to get user by id: {}", id);
        UserDto user = authService.getUserById(id);
        return ResponseEntity.ok(user);
    }
}