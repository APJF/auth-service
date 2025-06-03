package fpt.sep.jlsf.service.impl;

import fpt.sep.jlsf.dto.RegisterDTO;
import fpt.sep.jlsf.entity.User;
import fpt.sep.jlsf.entity.VerifyToken;
import fpt.sep.jlsf.entity.VerifyToken.VerifyTokenType;
import fpt.sep.jlsf.exception.AppException;
import fpt.sep.jlsf.repository.UserRepository;
import fpt.sep.jlsf.repository.VerifyTokenRepository;
import fpt.sep.jlsf.service.UserService;
import fpt.sep.jlsf.utils.EmailUtils;
import fpt.sep.jlsf.utils.OtpUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Primary
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final VerifyTokenRepository verifyTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpUtils otpUtils;
    private final EmailUtils emailUtils;

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final Duration OTP_THROTTLE = Duration.ofMinutes(1);

    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        if (userRepository.existsByEmail(registerDTO.email())) {
            throw new IllegalArgumentException("Email đã tồn tại.");
        }
        if (userRepository.existsByUsername(registerDTO.username())) {
            throw new IllegalArgumentException("Username đã tồn tại.");
        }

        User user = new User();
        user.setUsername(registerDTO.username());
        user.setPassword(passwordEncoder.encode(registerDTO.password()));
        user.setEmail(registerDTO.email());
        user.setAvatar("https://engineering.usask.ca/images/no_avatar.jpg");
        user.setEnabled(false);
        userRepository.save(user);

        createAndSendToken(user, VerifyTokenType.REGISTRATION);
    }

    @Override
    @Transactional
    public void verifyAccount(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Email không tồn tại."));
        VerifyToken token = verifyTokenRepository
                .findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.REGISTRATION)
                .orElseThrow(() -> new AppException("OTP không tồn tại."));

        if (token.getExpirationTime().isBefore(LocalDateTime.now()) || !token.getToken().equals(otp)) {
            throw new AppException("OTP sai hoặc đã hết hạn.");
        }

        user.setEnabled(true);
        userRepository.save(user);
        verifyTokenRepository.delete(token);
    }

    @Override
    @Transactional
    public void regenerateOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User không tồn tại."));
        VerifyToken token = verifyTokenRepository
                .findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.REGISTRATION)
                .orElseThrow(() -> new AppException("Chưa có OTP trước đó."));

        if (Duration.between(token.getRequestedTime(), LocalDateTime.now()).compareTo(OTP_THROTTLE) < 0) {
            throw new AppException("Vui lòng chờ ít nhất 1 phút trước khi yêu cầu gửi lại OTP.");
        }

        verifyTokenRepository.delete(token);
        createAndSendToken(user, VerifyTokenType.REGISTRATION);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User không tồn tại."));
        createAndSendToken(user, VerifyTokenType.RESET_PASSWORD);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));
        VerifyToken token = verifyTokenRepository.findTopByUserAndTypeOrderByRequestedTimeDesc(user, VerifyTokenType.RESET_PASSWORD)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (!otpUtils.validateOTP(token.getToken(), otp)) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        if (token.getExpirationTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        save(user);

        // Vô hiệu hóa tất cả các token reset password của user này
        verifyTokenRepository.deleteAllByUserAndType(user, VerifyTokenType.RESET_PASSWORD);
    }

    private void createAndSendToken(User user, VerifyTokenType type) {
        verifyTokenRepository.deleteAllByUserAndType(user, type);
        LocalDateTime now = LocalDateTime.now();
        String otp = otpUtils.generateOTP();
        VerifyToken token = VerifyToken.builder()
                .user(user)
                .token(otp)
                .type(type)
                .requestedTime(now)
                .expirationTime(now.plus(OTP_TTL))
                .build();
        verifyTokenRepository.save(token);
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