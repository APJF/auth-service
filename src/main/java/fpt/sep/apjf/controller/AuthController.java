package fpt.sep.apjf.controller;

import fpt.sep.apjf.dto.ApiResponseDTO;
import fpt.sep.apjf.dto.LoginDTO;
import fpt.sep.apjf.dto.LoginResponse;
import fpt.sep.apjf.dto.RegisterDTO;
import fpt.sep.apjf.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginResponse payload = userService.login(loginDTO);
        return ResponseEntity.ok(new ApiResponseDTO(
                true, "Đăng nhập thành công", null, payload));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        ApiResponseDTO response = new ApiResponseDTO(true, "User registered. Please verify with OTP.");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponseDTO> verifyAccount(@RequestParam String email, @RequestParam String otp) {
        userService.verifyAccount(email, otp);
        return new ResponseEntity<>(new ApiResponseDTO(true, "Account verified successfully"), HttpStatus.OK);
    }

    @PostMapping("/otp")
    public ResponseEntity<ApiResponseDTO> regenerateOtp(@RequestParam String email) {
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