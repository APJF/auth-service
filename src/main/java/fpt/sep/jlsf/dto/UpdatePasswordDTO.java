package fpt.sep.jlsf.dto;

public record UpdatePasswordDTO(
        String currentPassword,
        String newPassword,
        String confirmNewPassword
) {
}