package by.baes.authservice.controller;

import by.baes.authservice.dto.AuthResponse;
import by.baes.authservice.dto.LoginRequest;
import by.baes.authservice.dto.RegisterRequest;
import by.baes.authservice.dto.UserDto;
import by.baes.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication API", description = "API for user authentication, registration, and user management.")
@Slf4j
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
        log.info("AuthController initialized");
    }

    @Operation(summary = "User login", description = "Authenticates a user with their name and password, returning a JWT token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated user and returned JWT token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(name = "Successful login", value = "{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Bad Request", value = "{\"error\": \"Invalid credentials\"}"))),
            @ApiResponse(responseCode = "401", description = "Authentication failed",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Unauthorized", value = "{\"error\": \"Invalid password\"}")))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid
            @RequestBody(required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User login credentials",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(name = "Login example", value = "{\"name\": \"testuser\", \"password\": \"testpass\"}")))
            LoginRequest request
    ) {
        log.debug("Received login request for user: {}", request.getName());
        String token = authService.login(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @Operation(summary = "User registration", description = "Registers a new user with provided credentials and roles, returning a JWT token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully registered user and returned JWT token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(name = "Successful registration", value = "{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or user already exists",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Bad Request", value = "{\"error\": \"Username already exists: testuser\"}")))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid
            @RequestBody(required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User registration data",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterRequest.class),
                            examples = @ExampleObject(name = "Registration example", value = "{\"name\": \"newuser\", \"password\": \"newpass\", \"email\": \"newuser@example.com\", \"roles\": [\"USER\"]}")))
            RegisterRequest request
    ) {
        log.debug("Received register request for user: {}", request.getName());
        String token = authService.register(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @Operation(summary = "User logout", description = "Invalidates the user's JWT token, effectively logging them out.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged out", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Invalid or missing Authorization header",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Bad Request", value = "{\"error\": \"Invalid Authorization header\"}")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(name = "Authorization", required = true)
            @Parameter(description = "Bearer token in the format 'Bearer <token>'", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            String authHeader
    ) {
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

    @Operation(summary = "Get all roles", description = "Retrieves a list of all available roles in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of roles",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, type = "array"),
                            examples = @ExampleObject(name = "Roles list", value = "[\"USER\", \"ADMIN\"]")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/roles")
    public ResponseEntity<List<String>> getAllRoles() {
        log.debug("Received request to get all roles");
        List<String> roles = authService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    @Operation(summary = "Get all users", description = "Retrieves a paginated list of users, optionally filtered by roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated list of users",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class),
                            examples = @ExampleObject(name = "Users list", value = "{\"content\": [{\"id\": 1, \"name\": \"testuser\", \"email\": \"test@example.com\", \"roles\": [\"USER\"]}], \"totalElements\": 1, \"totalPages\": 1}"))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameters",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Bad Request", value = "{\"error\": \"Invalid sort direction\"}")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/users")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (0-based)", example = "0") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "Number of users per page", example = "10") int size,
            @RequestParam(defaultValue = "asc") @Parameter(description = "Sort direction (asc or desc)", example = "asc") String sort,
            @RequestParam(required = false) @Parameter(description = "Filter by roles (optional)", example = "USER") List<String> roles
    ) {
        log.debug("Received request to get users with page: {}, size: {}, sort: {}, roles: {}", page, size, sort, roles);
        Page<UserDto> users = authService.getAllUsers(page, size, sort, roles);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get user by ID", description = "Retrieves detailed information about a user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user details",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class),
                            examples = @ExampleObject(name = "User details", value = "{\"id\": 1, \"name\": \"testuser\", \"email\": \"test@example.com\", \"roles\": [\"USER\"]}"))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Not Found", value = "{\"error\": \"User not found with id: 1\"}")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUserById(
            @PathVariable @Parameter(description = "ID of the user to retrieve", example = "1") Long id
    ) {
        log.debug("Received request to get user by id: {}", id);
        UserDto user = authService.getUserById(id);
        return ResponseEntity.ok(user);
    }
}