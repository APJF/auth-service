package fpt.sep.jlsf.service;

import fpt.sep.jlsf.dto.LoginDTO;
import fpt.sep.jlsf.dto.RegisterDTO;
import fpt.sep.jlsf.entity.User;
import fpt.sep.jlsf.exception.AppException;
import fpt.sep.jlsf.repository.UserRepository;
import fpt.sep.jlsf.util.EmailUtil;
import fpt.sep.jlsf.util.OtpUtil;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Primary
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpUtil otpUtil;
    private final EmailUtil emailUtil;

    @Override
    public void register(RegisterDTO registerDTO) {
        String otp = otpUtil.generateOTP();
        try {
            emailUtil.sendOtpEmail(registerDTO.email(), otp);
        } catch (Exception e) {
            log.error("Error sending email: {}", e.getMessage());
            throw new AppException("Error sending email OTP", e);

        }
        User user = new User();
        user.setUsername(registerDTO.username());
        user.setPassword(passwordEncoder.encode(registerDTO.password()));
        user.setEmail(registerDTO.email());
        user.setOtp(otp);
        user.setAvatar("https://engineering.usask.ca/images/no_avatar.jpg");
        user.setExpirationTime(LocalDateTime.now());
        //set status false
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Override
    public void verifyAccount(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Email not found: " + email));
        long secondsElapsed = Duration.between(user.getExpirationTime(), LocalDateTime.now()).getSeconds();
        if (!user.getOtp().equals(otp) || secondsElapsed > 300) {
            throw new AppException("OTP incorrect or expired. Please regenerate OTP and try again.");
        }
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Override
    public void regenerateOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found: " + email));
        String otp = otpUtil.generateOTP();
        try {
            emailUtil.sendOtpEmail(email, otp);
        } catch (MessagingException e) {
            throw new AppException("Error sending email OTP", e);
        }
        user.setOtp(otp);
        user.setExpirationTime(LocalDateTime.now());
        userRepository.save(user);
    }

    public String login(LoginDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.email())
                .orElseThrow(() -> new AppException("User not found: " + loginDTO.email()));
        if (!passwordEncoder.matches(loginDTO.password(), user.getPassword())) {
            return "Login failed";
        } else if (!user.isEnabled()) {
            return "Account not verified";
        }
        return "Login successful";
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found: " + email));
        String otp = otpUtil.generateOTP();
        user.setExpirationTime(LocalDateTime.now());
        try {
            user.setOtp(otp);
            emailUtil.sendSetPassword(email, otp);
            userRepository.save(user);
        } catch (MessagingException e) {
            throw new AppException("Error sending email OTP", e);
        }
    }

    @Override
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found: " + email));
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException("User not found with id: " + id));
    }

    @Override
    public Optional<User> findByUsername(String name) {
        return userRepository.findByUsername(name);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void updateUser(User updatedUser) {
        User existingUser = userRepository.findById(updatedUser.getId())
                .orElseThrow(() -> new AppException("User not found with id: " + updatedUser.getId()));
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setAddress(updatedUser.getAddress());
        existingUser.setAvatar(updatedUser.getAvatar());
        userRepository.save(existingUser);
    }

    public boolean checkPassword(String username, String rawPassword) {
        return findByUsername(username)
                .map(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .orElse(false);
    }

    public void changePassword(String username, String newPassword) {
        User user = findByUsername(username)
                .orElseThrow(() -> new AppException("User not found: " + username));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public List<User> searchUsers(String role, String email) {
        if ((role == null || role.isEmpty()) && (email == null || email.isEmpty())) {
            return userRepository.findAll();
        } else if (role != null && !role.isEmpty() && (email == null || email.isEmpty())) {
            return userRepository.findByAuthorities_Authority(role);
        } else if (role == null || role.isEmpty()) {
            return userRepository.findByEmailContaining(email);
        } else {
            return userRepository.findByAuthorities_AuthorityAndEmailContaining(role, email);
        }
    }
}
