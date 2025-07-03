package fpt.sep.apjf.repository;

import fpt.sep.apjf.entity.User;
import fpt.sep.apjf.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findTopByUserAndTypeOrderByRequestedTimeDesc(User user, Token.TokenType type);

    Optional<Token> findTopByUserOrderByRequestedTimeDesc(User user);

    void deleteAllByUserAndType(User user, Token.TokenType type);
}
