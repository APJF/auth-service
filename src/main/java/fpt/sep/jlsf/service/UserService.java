package fpt.sep.jlsf.service;

import fpt.sep.jlsf.dto.RegisterDTO;
import fpt.sep.jlsf.entity.User;

import java.util.Optional;

public interface UserService {

    void register(RegisterDTO registerDTO);

    void verifyAccount(String email, String otp);

    void regenerateOtp(String email);

    void forgotPassword(String email);

    void resetPassword(String email, String otp, String newPassword);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    User save(User user);
}