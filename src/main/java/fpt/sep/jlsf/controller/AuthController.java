package fpt.sep.jlsf.controller;

import fpt.sep.jlsf.dto.ApiResponseDTO;
import fpt.sep.jlsf.dto.RegisterDTO;
import fpt.sep.jlsf.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO> register(@RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        ApiResponseDTO response = new ApiResponseDTO(true, "User registered. Please verify with OTP.");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponseDTO> verifyAccount(@RequestParam String email, @RequestParam String otp) {
        userService.verifyAccount(email, otp);
        return new ResponseEntity<>(new ApiResponseDTO(true, "Account verified successfully"), HttpStatus.OK);
    }

    @PostMapping("/{email}/otp")
    public ResponseEntity<ApiResponseDTO> regenerateOtp(@PathVariable String email) {
        userService.regenerateOtp(email);
        return new ResponseEntity<>(new ApiResponseDTO(true, "OTP regenerated successfully"), HttpStatus.OK);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO> forgotPassword(@RequestParam String email) {
        userService.forgotPassword(email);
        return new ResponseEntity<>(new ApiResponseDTO(true, "Verification email sent"), HttpStatus.OK);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO> resetPassword(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword) {
        userService.resetPassword(email, otp, newPassword);
        return new ResponseEntity<>(new ApiResponseDTO(true, "Password reset successfully"), HttpStatus.OK);
    }
}