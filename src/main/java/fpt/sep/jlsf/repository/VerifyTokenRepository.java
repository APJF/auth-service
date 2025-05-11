package fpt.sep.jlsf.repository;

import fpt.sep.jlsf.entity.User;
import fpt.sep.jlsf.entity.VerifyToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerifyTokenRepository extends JpaRepository<VerifyToken, Long> {

    Optional<VerifyToken> findTopByUserAndTypeOrderByRequestedTimeDesc(User user, VerifyToken.VerifyTokenType type);

    void deleteAllByUserAndType(User user, VerifyToken.VerifyTokenType type);
}
