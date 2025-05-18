package fpt.sep.jlsf.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDTO(
        @NotBlank(message = "Username cannot left blank")
        @Size(min = 4, message = "username must be at least 4 character")
        String username,

        @NotBlank(message = "Email cannot left blank")
        @Email(message = "Wrong email format")
        String email,

        @NotBlank(message = "Password cannot left blank")
        @Size(min = 6, message = "password must be at least 6 character")
        String password
) {
}

