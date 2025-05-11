package fpt.sep.jlsf.repository;

import fpt.sep.jlsf.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByAuthorities_Authority(String role);

    List<User> findByEmailContaining(String email);

    List<User> findByAuthorities_AuthorityAndEmailContaining(String role, String email);
}
