package fpt.sep.jlsf.controller;

import fpt.sep.jlsf.dto.ApiResponseDTO;
import fpt.sep.jlsf.dto.RegisterDTO;
import fpt.sep.jlsf.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDTO register(@RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return new ApiResponseDTO(true, "User registered. Please verify with OTP.");
    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponseDTO verifyAccount(@RequestParam String email,
                                        @RequestParam String otp) {
        userService.verifyAccount(email, otp);
        return new ApiResponseDTO(true, "Account verified successfully.");
    }

    @PostMapping("/{email}/otp")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponseDTO regenerateOtp(@PathVariable String email) {
        userService.regenerateOtp(email);
        return new ApiResponseDTO(true, "OTP regenerated successfully.");
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponseDTO forgotPassword(@RequestParam String email) {
        userService.forgotPassword(email);
        return new ApiResponseDTO(true, "Verification email sent.");
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponseDTO resetPassword(
            @RequestParam String email,
            @RequestParam String newPassword
    ) {
        userService.resetPassword(email, newPassword);
        return new ApiResponseDTO(true, "Password reset successfully.");
    }

}
