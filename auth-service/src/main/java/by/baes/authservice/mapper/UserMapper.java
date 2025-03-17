package by.baes.authservice.mapper;

import by.baes.authservice.dto.RegisterRequest;
import by.baes.authservice.dto.UserDto;
import by.baes.authservice.entity.Role;
import by.baes.authservice.entity.User;
import by.baes.authservice.repository.RoleRepository;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", imports = {Role.class, Collectors.class})
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))")
    UserDto toDto(User user);

    @Mapping(target = "password", expression = "java(passwordEncoder.encode(request.getPassword()))")
    @Mapping(target = "roles", expression = "java(mapRoles(request.getRoles(), roleRepository))")
    User toEntity(RegisterRequest request,
                  @Context PasswordEncoder passwordEncoder,
                  @Context RoleRepository roleRepository);

    default Set<Role> mapRoles(Set<String> roleNames, RoleRepository roleRepository) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of(roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Default role USER not found")));
        }
        return roleNames.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                .collect(Collectors.toSet());
    }
}