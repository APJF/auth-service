package fpt.sep.jlsf.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verify_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, length = 64)
    private String token;

    @Column(name = "requested_time", nullable = false)
    private LocalDateTime requestedTime;

    @Column(name = "expiration_time", nullable = false)
    private LocalDateTime expirationTime;

    /**
     * Loại token: đăng ký, reset mật khẩu, verify email…
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private VerifyTokenType type;

    public enum VerifyTokenType {
        REGISTRATION,
        RESET_PASSWORD,
        VERIFY_EMAIL
    }
}

