package by.baes.authservice.service;

import by.baes.authservice.dto.LoginRequest;
import by.baes.authservice.dto.RegisterRequest;
import by.baes.authservice.dto.UserDto;
import by.baes.authservice.entity.Role;
import by.baes.authservice.entity.User;
import by.baes.authservice.exception.*;
import by.baes.authservice.mapper.UserMapper;
import by.baes.authservice.repository.RoleRepository;
import by.baes.authservice.repository.UserRepository;
import by.baes.authservice.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public String login(LoginRequest request) {
        log.debug("Login attempt for user: {}", request.getName());
        User user = userRepository.findByName(request.getName())
                .orElseThrow(() -> {
                    log.error("User not found: {}", request.getName());
                    return new UserNotFoundException("User not found: " + request.getName());
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", request.getName());
            throw new InvalidCredentialsException("Invalid password");
        }

        String token = jwtUtil.generateToken(user);
        log.info("Login successful for user: {}, token generated", request.getName());
        return token;
    }

    public String register(RegisterRequest request) {
        log.debug("Register attempt for user: {}", request.getName());
        if (userRepository.existsByName(request.getName())) {
            log.warn("Username already exists: {}", request.getName());
            throw new UserAlreadyExistsException("Username already exists: " + request.getName());
        }

        User user = userMapper.toEntity(request, passwordEncoder, roleRepository);

        userRepository.save(user);
        String token = jwtUtil.generateToken(user);
        log.info("User registered successfully: {}, token generated", request.getName());
        return token;
    }

    @Cacheable(value = "userDtos", key = "#token")
    public UserDto validateToken(String token) {
        log.debug("Validating token: {}", token);
        Claims claims;
        try {
            claims = jwtUtil.validateToken(token);
        } catch (Exception e) {
            log.error("Invalid token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid JWT token: " + e.getMessage());
        }
        User user = userRepository.findByName(claims.getSubject())
                .orElseThrow(() -> {
                    log.error("User not found for token subject: {}", claims.getSubject());
                    return new UserNotFoundException("User not found: " + claims.getSubject());
                });
        UserDto userDto = userMapper.toDto(user);
        log.info("Token validated for user: {}", claims.getSubject());
        return userDto;
    }

    @CacheEvict(value = "userDtos", key = "#token")
    public void logout(String token) {
        log.debug("Logout attempt for token");
        try {
            jwtUtil.validateToken(token);
            log.info("Logout successful for token");
        } catch (Exception e) {
            log.error("Invalid token during logout: {}", e.getMessage());
            throw new InvalidTokenException("Invalid JWT token during logout: " + e.getMessage());
        }
    }

    public List<String> getAllRoles() {
        log.debug("Fetching all roles");
        List<String> roles = roleRepository.findAll().stream()
                .map(Role::getName)
                .distinct()
                .collect(Collectors.toList());
        log.info("Retrieved {} roles", roles.size());
        return roles;
    }

    public Page<UserDto> getAllUsers(int page, int size, String sort, List<String> roles) {
        log.debug("Fetching users with page: {}, size: {}, sort: {}, roles: {}", page, size, sort, roles);
        Sort.Direction direction = sort.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "name"));

        Page<UserDto> users = (roles != null && !roles.isEmpty())
                ? userRepository.findByRolesIn(roles, pageable).map(userMapper::toDto)
                : userRepository.findAll(pageable).map(userMapper::toDto);
        log.info("Retrieved {} users", users.getTotalElements());
        return users;
    }

    public UserDto getUserById(Long id) {
        log.debug("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found with id: {}", id);
                    return new UserNotFoundException("User not found with id: " + id);
                });
        UserDto dto = userMapper.toDto(user);
        log.info("User retrieved: {}", user.getName());
        return dto;
    }
}