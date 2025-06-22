package fpt.sep.apjf.service;

import fpt.sep.apjf.dto.LoginDTO;
import fpt.sep.apjf.dto.LoginResponse;
import fpt.sep.apjf.dto.RegisterDTO;
import fpt.sep.apjf.entity.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserService {

    @Transactional(readOnly = true)
    LoginResponse login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);

    void verifyAccount(String email, String otp);

    void regenerateOtp(String email);

    void forgotPassword(String email);

    void resetPassword(String email, String otp, String newPassword);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    User save(User user);
}