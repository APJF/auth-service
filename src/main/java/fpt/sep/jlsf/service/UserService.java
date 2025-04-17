package fpt.sep.jlsf.service;

import fpt.sep.jlsf.dto.LoginDTO;
import fpt.sep.jlsf.dto.RegisterDTO;
import fpt.sep.jlsf.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    String login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);

    void verifyAccount(String email, String otp);

    void regenerateOtp(String email);

    void forgotPassword(String email);

    void resetPassword(String email, String newPassword);

    User getUserById(Long id);

    Optional<User> findByUsername(String name);

    Optional<User> findByEmail(String name);

    User save(User user);

    void updateUser(User updatedUser);

    boolean checkPassword(String username, String rawPassword);

    void changePassword(String username, String newPassword);

    List<User> searchUsers(String role, String email);
}
