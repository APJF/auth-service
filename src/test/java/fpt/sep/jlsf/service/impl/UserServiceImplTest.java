package fpt.sep.jlsf.service.impl;

import fpt.sep.jlsf.dto.LoginDTO;
import fpt.sep.jlsf.dto.RegisterDTO;
import fpt.sep.jlsf.entity.User;
import fpt.sep.jlsf.exception.AppException;
import fpt.sep.jlsf.repository.UserRepository;
import fpt.sep.jlsf.util.EmailUtil;
import fpt.sep.jlsf.util.OtpUtil;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailUtil emailUtil;
    @Mock
    private OtpUtil otpUtil;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_shouldSaveUserAndSendEmail() throws MessagingException {
        RegisterDTO dto = new RegisterDTO("phan", "email@example.com", "123456");
        when(otpUtil.generateOTP()).thenReturn("123456");

        userService.register(dto);

        verify(emailUtil).sendOtpEmail(dto.email(), "123456");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_shouldReturnSuccessMessage() {
        LoginDTO dto = new LoginDTO("email@example.com", "password");
        User user = new User();
        user.setEmail(dto.email());
        user.setPassword("hashed");
        user.setEnabled(true);

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), user.getPassword())).thenReturn(true);

        String result = userService.login(dto);
        assertEquals("Login successful", result);
    }

    @Test
    void login_shouldFailIfPasswordWrong() {
        LoginDTO dto = new LoginDTO("email@example.com", "wrongpass");
        User user = new User();
        user.setEmail(dto.email());
        user.setPassword("correctHash");
        user.setEnabled(true);

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), user.getPassword())).thenReturn(false);

        String result = userService.login(dto);
        assertEquals("Login failed", result);
    }

    @Test
    void resetPassword_shouldEncodeAndSave() {
        User user = new User();
        user.setEmail("email@example.com");

        when(userRepository.findByEmail("email@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("hashed");

        userService.resetPassword("email@example.com", "newpass");

        assertEquals("hashed", user.getPassword());
        assertTrue(user.isEnabled());
        verify(userRepository).save(user);
    }

    @Test
    void checkPassword_shouldReturnTrueIfMatch() {
        User user = new User();
        user.setUsername("phan");
        user.setPassword("hash");

        when(userRepository.findByUsername("phan")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain", "hash")).thenReturn(true);

        assertTrue(userService.checkPassword("phan", "plain"));
    }

    @Test
    void checkPassword_shouldReturnFalseIfNotMatch() {
        when(userRepository.findByUsername("phan")).thenReturn(Optional.empty());
        assertFalse(userService.checkPassword("phan", "wrong"));
    }

    @Test
    void forgotPassword_shouldThrowIfUserNotFound() {
        when(userRepository.findByEmail("none@example.com")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> userService.forgotPassword("none@example.com"));
    }

    @Test
    void verifyAccount_shouldEnableUserIfOtpCorrect() {
        User user = new User();
        user.setOtp("123456");
        user.setExpirationTime(java.time.LocalDateTime.now());

        when(userRepository.findByEmail("email@example.com")).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> userService.verifyAccount("email@example.com", "123456"));
        assertTrue(user.isEnabled());
        verify(userRepository).save(user);
    }
}