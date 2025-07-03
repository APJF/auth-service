package fpt.sep.apjf.service.impl;

import fpt.sep.apjf.dto.LoginDTO;
import fpt.sep.apjf.dto.LoginResponse;
import fpt.sep.apjf.dto.RegisterDTO;
import fpt.sep.apjf.entity.Authority;
import fpt.sep.apjf.entity.User;
import fpt.sep.apjf.entity.Token;
import fpt.sep.apjf.entity.Token.TokenType;
import fpt.sep.apjf.exception.AppException;
import fpt.sep.apjf.repository.AuthorityRepository;
import fpt.sep.apjf.repository.UserRepository;
import fpt.sep.apjf.repository.TokenRepository;
import fpt.sep.apjf.service.UserService;
import fpt.sep.apjf.utils.EmailUtils;
import fpt.sep.apjf.utils.JwtUtils;
import fpt.sep.apjf.utils.OtpUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Primary
public class UserServiceImpl implements UserService {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final Duration OTP_THROTTLE = Duration.ofMinutes(1);
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpUtils otpUtils;
    private final EmailUtils emailUtils;
    private final JwtUtils jwtUtils;
    private final AuthorityRepository authorityRepository;

    @Override
    @Transactional
    public LoginResponse login(LoginDTO loginDTO) {
        // 1. Kiểm tra email có tồn tại không
        User user = userRepository.findByEmail(loginDTO.email())
                .orElseThrow(() -> new BadCredentialsException("Email không tồn tại trong hệ thống"));

        // 2. Kiểm tra mật khẩu có khớp không
        if (!passwordEncoder.matches(loginDTO.password(), user.getPassword())) {
            throw new BadCredentialsException("Mật khẩu không chính xác");
        }

        // 3. Kiểm tra tài khoản đã được kích hoạt chưa
        if (!user.isEnabled()) {
            throw new BadCredentialsException("Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email để kích hoạt tài khoản");
        }

        // 4. Lấy role & sinh token
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new LoginResponse(
                user.getUsername(),
                roles,
                jwtUtils.generateTokenFromUsername(user),
                user.getAvatar());
    }

    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        if (userRepository.existsByEmail(registerDTO.email())) {
            throw new IllegalArgumentException("Email đã tồn tại.");
        }
        Authority userRole = authorityRepository.findByAuthority("ROLE_USER")
                .orElseThrow();
        User user = new User();
        user.setUsername("new user");

        user.setAuthorities(new ArrayList<>(List.of(userRole)));
        user.setPassword(passwordEncoder.encode(registerDTO.password()));
        user.setEmail(registerDTO.email());
        user.setAvatar("https://engineering.usask.ca/images/no_avatar.jpg");
        user.setEnabled(false);
        userRepository.save(user);

        createAndSendToken(user, TokenType.REGISTRATION);
    }

    @Override
    @Transactional
    public void verifyAccount(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Email không tồn tại."));

        // Tìm token mới nhất (bất kể type nào)
        Token token = tokenRepository
                .findTopByUserOrderByRequestedTimeDesc(user)
                .orElseThrow(() -> new AppException("OTP không tồn tại."));

        if (token.getExpirationTime().isBefore(LocalDateTime.now()) || !token.getToken().equals(otp)) {
            throw new AppException("OTP sai hoặc đã hết hạn.");
        }

        // Xử lý dựa trên type của token
        if (token.getType() == TokenType.REGISTRATION) {
            user.setEnabled(true);
            userRepository.save(user);
        }
        // Với reset password, chỉ cần xóa token, không cần thay đổi user
        // Logic reset password đã được xử lý ở method resetPassword

        tokenRepository.delete(token);
    }

    @Override
    @Transactional
    public void regenerateOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User không tồn tại."));
        Token token = tokenRepository
                .findTopByUserAndTypeOrderByRequestedTimeDesc(user, TokenType.REGISTRATION)
                .orElseThrow(() -> new AppException("Chưa có OTP trước đó."));

        if (Duration.between(token.getRequestedTime(), LocalDateTime.now()).compareTo(OTP_THROTTLE) < 0) {
            throw new AppException("Vui lòng chờ ít nhất 1 phút trước khi yêu cầu gửi lại OTP.");
        }

        tokenRepository.delete(token);
        createAndSendToken(user, TokenType.REGISTRATION);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User không tồn tại."));
        createAndSendToken(user, TokenType.RESET_PASSWORD);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = findByEmail(email).orElseThrow(() -> new EntityNotFoundException("Email không tồn tại"));
        Token token = tokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, TokenType.RESET_PASSWORD)
                .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ"));

        if (!otpUtils.validateOTP(token.getToken(), otp)) {
            throw new IllegalArgumentException("OTP không chính xác");
        }

        if (token.getExpirationTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP đã hết hạn");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        save(user);

        // Vô hiệu hóa tất cả các token reset password của user này
        tokenRepository.deleteAllByUserAndType(user, TokenType.RESET_PASSWORD);
    }

    private void createAndSendToken(User user, TokenType type) {
        tokenRepository.deleteAllByUserAndType(user, type);
        LocalDateTime now = LocalDateTime.now();
        String otp = otpUtils.generateOTP();
        Token token = Token.builder()
                .user(user)
                .token(otp)
                .type(type)
                .requestedTime(now)
                .expirationTime(now.plus(OTP_TTL))
                .build();
        tokenRepository.save(token);
        emailUtils.sendEmailAsync(user.getEmail(), otp, type);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }
}