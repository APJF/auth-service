package fpt.sep.jlsf.controller;

import fpt.sep.jlsf.dto.ApiResponseDTO;
import fpt.sep.jlsf.dto.LoginRequest;
import fpt.sep.jlsf.dto.LoginResponse;
import fpt.sep.jlsf.dto.RegisterDTO;
import fpt.sep.jlsf.service.UserService;
import fpt.sep.jlsf.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    private final UserService userService;


    @PostMapping("/signin")
    public ResponseEntity<?> authenticationUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate
                    (new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));
        } catch (AuthenticationException e) {
            Map<String, Object> map = new HashMap<>();
            map.put("error", "Invalid username or password");
            map.put("status", false);
            return new ResponseEntity<Object>(map, HttpStatus.NOT_FOUND);

        }
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        LoginResponse loginResponse = new LoginResponse(userDetails.getUsername(), roles, jwtToken);
        return ResponseEntity.ok(loginResponse);
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