package fpt.sep.jlsf.service.impl;

import fpt.sep.jlsf.dto.RegisterDTO;
import fpt.sep.jlsf.entity.User;
import fpt.sep.jlsf.entity.VerifyToken;
import fpt.sep.jlsf.entity.VerifyToken.VerifyTokenType;
import fpt.sep.jlsf.exception.AppException;
import fpt.sep.jlsf.repository.UserRepository;
import fpt.sep.jlsf.repository.VerifyTokenRepository;
import fpt.sep.jlsf.util.EmailUtil;
import fpt.sep.jlsf.util.OtpUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerifyTokenRepository verifyTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpUtil otpUtil;

    @Mock
    private EmailUtil emailUtil;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_emailAlreadyExists_throwsException() {
        RegisterDTO dto = new RegisterDTO("testuser", "password", "test@example.com");
        when(userRepository.existsByEmail(dto.email())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.register(dto));
    }

    @Test
    void register_usernameAlreadyExists_throwsException() {
        RegisterDTO dto = new RegisterDTO("testuser", "password", "test@example.com");
        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(userRepository.existsByUsername(dto.username())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.register(dto));
    }

    @Test
    void register_newUser_success() {
        RegisterDTO dto = new RegisterDTO("testuser", "password", "test@example.com");
        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(userRepository.existsByUsername(dto.username())).thenReturn(false);
        when(passwordEncoder.encode(dto.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(otpUtil.generateOTP()).thenReturn("123456");

        userService.register(dto);

        verify(userRepository).save(argThat(user ->
                user.getUsername().equals(dto.username()) &&
                        user.getPassword().equals("encodedPassword") &&
                        user.getEmail().equals(dto.email()) &&
                        user.getAvatar().equals("https://engineering.usask.ca/images/no_avatar.jpg") &&
                        !user.isEnabled()
        ));
        verify(verifyTokenRepository).deleteAllByUserAndType(any(User.class), eq(VerifyTokenType.REGISTRATION));
        verify(otpUtil).generateOTP();
        verify(verifyTokenRepository).save(any(VerifyToken.class));
        verify(emailUtil).sendEmailAsync(eq(dto.email()), eq("123456"), eq(VerifyTokenType.REGISTRATION));
    }

    @Test
    void register_nullFields_throwsException() {
        RegisterDTO dto1 = new RegisterDTO(null, "password", "test@example.com");
        assertThrows(MethodArgumentNotValidException.class, () -> userService.register(dto1));

        RegisterDTO dto2 = new RegisterDTO("testuser", null, "test@example.com");
        assertThrows(MethodArgumentNotValidException.class, () -> userService.register(dto2));

        RegisterDTO dto3 = new RegisterDTO("testuser", "password", null);
        assertThrows(MethodArgumentNotValidException.class, () -> userService.register(dto3));
    }

    @Test
    void register_invalidEmail_throwsException() {
        RegisterDTO dto = new RegisterDTO("testuser", "password", "invalid-email");
        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(userRepository.existsByUsername(dto.username())).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> userService.register(dto));
    }

    @Test
    void register_saveUserFails_rollsBack() {
        RegisterDTO dto = new RegisterDTO("testuser", "password", "test@example.com");
        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(userRepository.existsByUsername(dto.username())).thenReturn(false);
        when(passwordEncoder.encode(dto.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> userService.register(dto));
        verify(userRepository).save(any(User.class));
        verify(verifyTokenRepository, never()).save(any(VerifyToken.class));
        verify(emailUtil, never()).sendEmailAsync(anyString(), anyString(), any(VerifyTokenType.class));
    }

    @Test
    void verifyAccount_userNotFound_throwsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> userService.verifyAccount("test@example.com", "123456"));
    }

    @Test
    void verifyAccount_noTokenFound_throwsException() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.REGISTRATION))
                .thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> userService.verifyAccount("test@example.com", "123456"));
    }

    @Test
    void verifyAccount_invalidOrExpiredOtp_throwsException() {
        User user = new User();
        user.setId(1L);
        VerifyToken token = VerifyToken.builder()
                .user(user)
                .token("654321")
                .type(VerifyTokenType.REGISTRATION)
                .requestedTime(LocalDateTime.now().minusMinutes(5))
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.REGISTRATION))
                .thenReturn(Optional.of(token));
        assertThrows(AppException.class, () -> userService.verifyAccount("test@example.com", "123456"));
    }

    @Test
    void verifyAccount_validOtp_success() {
        User user = new User();
        user.setId(1L);
        user.setEnabled(false);
        VerifyToken token = VerifyToken.builder()
                .user(user)
                .token("123456")
                .type(VerifyTokenType.REGISTRATION)
                .requestedTime(LocalDateTime.now().minusMinutes(5))
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.REGISTRATION))
                .thenReturn(Optional.of(token));

        userService.verifyAccount("test@example.com", "123456");

        assertTrue(user.isEnabled());
        verify(userRepository).save(user);
        verify(verifyTokenRepository).delete(token);
    }

    @Test
    void verifyAccount_expiredOtp_throwsException() {
        User user = new User();
        user.setId(1L);
        VerifyToken token = VerifyToken.builder()
                .user(user)
                .token("123456")
                .type(VerifyTokenType.REGISTRATION)
                .requestedTime(LocalDateTime.now().minusMinutes(15))
                .expirationTime(LocalDateTime.now().minusMinutes(5)) // OTP đã hết hạn
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.REGISTRATION))
                .thenReturn(Optional.of(token));
        assertThrows(AppException.class, () -> userService.verifyAccount("test@example.com", "123456"));
    }

    @Test
    void verifyAccount_nullInputs_throwsException() {
        assertThrows(AppException.class, () -> userService.verifyAccount(null, "123456"));
        assertThrows(AppException.class, () -> userService.verifyAccount("test@example.com", null));
    }

    @Test
    void verifyAccount_saveFails_rollsBack() {
        User user = new User();
        user.setId(1L);
        user.setEnabled(false);
        VerifyToken token = VerifyToken.builder()
                .user(user)
                .token("123456")
                .type(VerifyTokenType.REGISTRATION)
                .requestedTime(LocalDateTime.now().minusMinutes(5))
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.REGISTRATION))
                .thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> userService.verifyAccount("test@example.com", "123456"));
        verify(userRepository).save(user);
        verify(verifyTokenRepository, never()).delete(any(VerifyToken.class));
    }

    @Test
    void regenerateOtp_userNotFound_throwsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> userService.regenerateOtp("test@example.com"));
    }

    @Test
    void regenerateOtp_throttleTimeNotPassed_throwsException() {
        User user = new User();
        user.setId(1L);
        VerifyToken token = VerifyToken.builder()
                .user(user)
                .token("123456")
                .type(VerifyTokenType.REGISTRATION)
                .requestedTime(LocalDateTime.now().minusSeconds(30))
                .expirationTime(LocalDateTime.now().plusMinutes(10))
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.REGISTRATION))
                .thenReturn(Optional.of(token));
        assertThrows(AppException.class, () -> userService.regenerateOtp("test@example.com"));
    }

    @Test
    void regenerateOtp_deletesTokensOnce() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        VerifyToken oldToken = VerifyToken.builder()
                .user(user)
                .token("123456")
                .type(VerifyTokenType.REGISTRATION)
                .requestedTime(LocalDateTime.now().minusMinutes(2))
                .expirationTime(LocalDateTime.now().plusMinutes(8))
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.REGISTRATION))
                .thenReturn(Optional.of(oldToken));
        when(otpUtil.generateOTP()).thenReturn("654321");

        userService.regenerateOtp("test@example.com");

        verify(verifyTokenRepository).delete(oldToken);
        verify(verifyTokenRepository, times(1)).deleteAllByUserAndType(user, VerifyTokenType.REGISTRATION);
    }

    @Test
    void regenerateOtp_throttleTimeAtBoundary_success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        VerifyToken oldToken = VerifyToken.builder()
                .user(user)
                .token("123456")
                .type(VerifyTokenType.REGISTRATION)
                .requestedTime(LocalDateTime.now().minusMinutes(1)) // Đúng 1 phút
                .expirationTime(LocalDateTime.now().plusMinutes(8))
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.REGISTRATION))
                .thenReturn(Optional.of(oldToken));
        when(otpUtil.generateOTP()).thenReturn("654321");

        userService.regenerateOtp("test@example.com");

        verify(verifyTokenRepository).delete(oldToken);
        verify(verifyTokenRepository).save(any(VerifyToken.class));
    }

    @Test
    void regenerateOtp_nullEmail_throwsException() {
        assertThrows(AppException.class, () -> userService.regenerateOtp(null));
    }

    @Test
    void regenerateOtp_saveTokenFails_rollsBack() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        VerifyToken oldToken = VerifyToken.builder()
                .user(user)
                .token("123456")
                .type(VerifyTokenType.REGISTRATION)
                .requestedTime(LocalDateTime.now().minusMinutes(2))
                .expirationTime(LocalDateTime.now().plusMinutes(8))
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.REGISTRATION))
                .thenReturn(Optional.of(oldToken));
        when(otpUtil.generateOTP()).thenReturn("654321");
        when(verifyTokenRepository.save(any(VerifyToken.class))).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> userService.regenerateOtp("test@example.com"));
        verify(verifyTokenRepository).delete(oldToken);
        verify(verifyTokenRepository).save(any(VerifyToken.class));
        verify(emailUtil, never()).sendEmailAsync(anyString(), anyString(), any(VerifyTokenType.class));
    }

    @Test
    void regenerateOtp_success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com"); // Thêm dòng này để thiết lập email
        VerifyToken oldToken = VerifyToken.builder()
                .user(user)
                .token("123456")
                .type(VerifyTokenType.REGISTRATION)
                .requestedTime(LocalDateTime.now().minusMinutes(2))
                .expirationTime(LocalDateTime.now().plusMinutes(8))
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.REGISTRATION))
                .thenReturn(Optional.of(oldToken));
        when(otpUtil.generateOTP()).thenReturn("654321");

        userService.regenerateOtp("test@example.com");

        verify(verifyTokenRepository).delete(oldToken);
        ArgumentCaptor<VerifyToken> tokenCaptor = ArgumentCaptor.forClass(VerifyToken.class);
        verify(verifyTokenRepository).save(tokenCaptor.capture());
        VerifyToken newToken = tokenCaptor.getValue();
        assertEquals("654321", newToken.getToken());
        verify(emailUtil).sendEmailAsync(eq("test@example.com"), eq("654321"), eq(VerifyTokenType.REGISTRATION));
    }

    @Test
    void forgotPassword_userNotFound_throwsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> userService.forgotPassword("test@example.com"));
    }

    @Test
    void forgotPassword_success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(otpUtil.generateOTP()).thenReturn("123456");
        when(verifyTokenRepository.save(any(VerifyToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.forgotPassword("test@example.com");

        verify(verifyTokenRepository, times(1)).deleteAllByUserAndType(user, VerifyTokenType.RESET_PASSWORD); // Chỉ 1 lần gọi
        ArgumentCaptor<VerifyToken> tokenCaptor = ArgumentCaptor.forClass(VerifyToken.class);
        verify(verifyTokenRepository).save(tokenCaptor.capture());
        VerifyToken savedToken = tokenCaptor.getValue();
        assertEquals("123456", savedToken.getToken());
        assertEquals(VerifyTokenType.RESET_PASSWORD, savedToken.getType());
        assertEquals(user, savedToken.getUser());
        assertNotNull(savedToken.getRequestedTime());
        assertNotNull(savedToken.getExpirationTime());

        verify(emailUtil, times(1)).sendEmailAsync(eq("test@example.com"), eq("123456"), eq(VerifyTokenType.RESET_PASSWORD));
        verifyNoMoreInteractions(emailUtil, verifyTokenRepository);
    }

    @Test
    void forgotPassword_createsSingleToken() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(otpUtil.generateOTP()).thenReturn("123456");
        when(verifyTokenRepository.save(any(VerifyToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.forgotPassword("test@example.com");

        ArgumentCaptor<VerifyToken> tokenCaptor = ArgumentCaptor.forClass(VerifyToken.class);
        verify(verifyTokenRepository).save(tokenCaptor.capture());
        verify(verifyTokenRepository, times(1)).deleteAllByUserAndType(user, VerifyTokenType.RESET_PASSWORD);
        assertEquals(1, tokenCaptor.getAllValues().size()); // Chỉ 1 token được lưu
    }

    @Test
    void forgotPassword_saveTokenFails_rollsBack() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(otpUtil.generateOTP()).thenReturn("123456");
        when(verifyTokenRepository.save(any(VerifyToken.class))).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> userService.forgotPassword("test@example.com"));

        verify(verifyTokenRepository, times(1)).deleteAllByUserAndType(user, VerifyTokenType.RESET_PASSWORD);
        verify(verifyTokenRepository).save(any(VerifyToken.class));
        verify(emailUtil, never()).sendEmailAsync(anyString(), anyString(), any(VerifyTokenType.class));
    }

    @Test
    void forgotPassword_nullEmail_throwsException() {
        assertThrows(AppException.class, () -> userService.forgotPassword(null));
    }

    @Test
    void forgotPassword_deleteTokensFails_throwsException() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("Database error"))
                .when(verifyTokenRepository).deleteAllByUserAndType(user, VerifyTokenType.RESET_PASSWORD);

        assertThrows(RuntimeException.class, () -> userService.forgotPassword("test@example.com"));
        verify(verifyTokenRepository).deleteAllByUserAndType(user, VerifyTokenType.RESET_PASSWORD);
        verify(verifyTokenRepository, never()).save(any(VerifyToken.class));
    }

    @Test
    void resetPassword_userNotFound_throwsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> userService.resetPassword("test@example.com", "123456", "newPassword"));
    }

    @Test
    void resetPassword_invalidOtp_throwsException() {
        User user = new User();
        user.setId(1L);
        VerifyToken token = VerifyToken.builder()
                .user(user)
                .token("654321")
                .type(VerifyTokenType.RESET_PASSWORD)
                .requestedTime(LocalDateTime.now().minusMinutes(5))
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.RESET_PASSWORD))
                .thenReturn(Optional.of(token));
        when(otpUtil.validateOTP("654321", "123456")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.resetPassword("test@example.com", "123456", "newPassword"));
    }

    @Test
    void resetPassword_expiredOtp_throwsException() {
        User user = new User();
        user.setId(1L);
        VerifyToken token = VerifyToken.builder()
                .user(user)
                .token("123456")
                .type(VerifyTokenType.RESET_PASSWORD)
                .requestedTime(LocalDateTime.now().minusMinutes(15))
                .expirationTime(LocalDateTime.now().minusMinutes(5))
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.RESET_PASSWORD))
                .thenReturn(Optional.of(token));
        when(otpUtil.validateOTP("123456", "123456")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.resetPassword("test@example.com", "123456", "newPassword"));
    }

    @Test
    void resetPassword_nullInputs_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.resetPassword(null, "123456", "newPassword"));
        assertThrows(IllegalArgumentException.class, () -> userService.resetPassword("test@example.com", null, "newPassword"));
        assertThrows(IllegalArgumentException.class, () -> userService.resetPassword("test@example.com", "123456", null));
    }

    @Test
    void resetPassword_saveFails_rollsBack() {
        User user = new User();
        user.setId(1L);
        VerifyToken token = VerifyToken.builder()
                .user(user)
                .token("123456")
                .type(VerifyTokenType.RESET_PASSWORD)
                .requestedTime(LocalDateTime.now().minusMinutes(5))
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.RESET_PASSWORD))
                .thenReturn(Optional.of(token));
        when(otpUtil.validateOTP("123456", "123456")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> userService.resetPassword("test@example.com", "123456", "newPassword"));
        verify(userRepository).save(user);
        verify(verifyTokenRepository, never()).deleteAllByUserAndType(any(User.class), any(VerifyTokenType.class));
    }

    @Test
    void resetPassword_success() {
        User user = new User();
        user.setId(1L);
        VerifyToken token = VerifyToken.builder()
                .user(user)
                .token("123456")
                .type(VerifyTokenType.RESET_PASSWORD)
                .requestedTime(LocalDateTime.now().minusMinutes(5))
                .expirationTime(LocalDateTime.now().plusMinutes(5))
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.RESET_PASSWORD))
                .thenReturn(Optional.of(token));
        when(otpUtil.validateOTP("123456", "123456")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        userService.resetPassword("test@example.com", "123456", "newPassword");

        assertEquals("encodedNewPassword", user.getPassword());
        verify(userRepository).save(user);
        verify(verifyTokenRepository).deleteAllByUserAndType(user, VerifyTokenType.RESET_PASSWORD);
    }



    @Test
    void findByUsername_success() {
        User user = new User();
        user.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByUsername("testuser");
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void findByEmail_success() {
        User user = new User();
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("test@example.com");
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void save_success() {
        User user = new User();
        user.setUsername("testuser");
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.save(user);
        assertEquals(user, result);
    }

    @Test
    void findByUsername_notFound_returnsEmpty() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        Optional<User> result = userService.findByUsername("testuser");
        assertTrue(result.isEmpty());
    }

    @Test
    void findByEmail_notFound_returnsEmpty() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        Optional<User> result = userService.findByEmail("test@example.com");
        assertTrue(result.isEmpty());
    }

    @Test
    void save_fails_throwsException() {
        User user = new User();
        user.setUsername("testuser");
        when(userRepository.save(user)).thenThrow(new RuntimeException("Database error"));
        assertThrows(RuntimeException.class, () -> userService.save(user));
    }
}