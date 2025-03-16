package by.baes.authservice.mapper;

import by.baes.authservice.dto.UserDto;
import by.baes.authservice.entity.Role;
import by.baes.authservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {Role.class})
public interface UserMapper {
    @Mapping(target = "roles", expression = "java(user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toList()))")
    UserDto toDto(User user);
}