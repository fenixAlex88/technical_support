package by.baes.authservice.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String telegramId;
    private Set<String> roles;
}
