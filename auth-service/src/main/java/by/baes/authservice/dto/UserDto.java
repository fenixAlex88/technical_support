package by.baes.authservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String telegramId;
    private List<String> roles;
}
