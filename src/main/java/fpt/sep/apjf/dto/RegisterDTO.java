package fpt.sep.apjf.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDTO(

        @NotBlank(message = "Email cannot left blank")
        @Email(message = "Wrong email format")
        String email,

        @NotBlank(message = "Password cannot left blank")
        @Size(min = 6, message = "password must be at least 6 character")
        String password
) {
}

