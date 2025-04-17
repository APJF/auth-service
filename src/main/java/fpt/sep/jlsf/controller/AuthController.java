package fpt.sep.jlsf.controller;

import fpt.sep.jlsf.dto.ApiResponseDTO;
import fpt.sep.jlsf.dto.LoginDTO;
import fpt.sep.jlsf.dto.RegisterDTO;
import fpt.sep.jlsf.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        String message = userService.login(loginDTO);
        return ResponseEntity.ok(new ApiResponseDTO(true, message));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO> register(@RequestBody RegisterDTO registerDTO) {
//        userService.register(registerDTO);
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(new ApiResponseDTO(true, "User registered. Please verify with OTP."));
                try {
                    userService.register(registerDTO);   // vẫn gọi service void
                    return ResponseEntity
                            .status(HttpStatus.CREATED)
                            .body(new ApiResponseDTO(true, "User registered. Please verify with OTP."));
                } catch (Exception ex) {
                    return ResponseEntity
                            .status(HttpStatus.CREATED)
                            .body(new ApiResponseDTO(false, ex.getMessage()));
                }
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponseDTO> verifyAccount(@RequestParam String email,
                                                        @RequestParam String otp) {
        userService.verifyAccount(email, otp);
        return ResponseEntity.ok(new ApiResponseDTO(true, "Account verified successfully."));
    }

    @PostMapping("/regenerate-otp")
    public ResponseEntity<ApiResponseDTO> regenerateOtp(@RequestParam String email) {
        userService.regenerateOtp(email);
        return ResponseEntity.ok(new ApiResponseDTO(true, "OTP regenerated successfully."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO> forgotPassword(@RequestParam String email) {
        userService.forgotPassword(email);
        return ResponseEntity.ok(new ApiResponseDTO(true, "Verification email sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO> resetPassword(@RequestParam String email,
                                                        @RequestParam String newPassword) {
        userService.resetPassword(email, newPassword);
        return ResponseEntity.ok(new ApiResponseDTO(true, "Password reset successfully."));
    }
}
