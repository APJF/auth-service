package fpt.sep.apjf.repository;

import fpt.sep.apjf.entity.User;
import fpt.sep.apjf.entity.VerifyToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerifyTokenRepository extends JpaRepository<VerifyToken, Long> {

    Optional<VerifyToken> findTopByUserAndTypeOrderByRequestedTimeDesc(User user, VerifyToken.VerifyTokenType type);

    void deleteAllByUserAndType(User user, VerifyToken.VerifyTokenType type);
}
