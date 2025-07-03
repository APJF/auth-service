package fpt.sep.apjf.utils;

import fpt.sep.apjf.entity.Token;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailUtils {

    private final JavaMailSender javaMailSender;

    // Phương thức gửi email chung, nhận vào nội dung HTML đã được tạo sẵn
    private void sendEmail(String email, String subject, String htmlContent) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setTo(email);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        javaMailSender.send(mimeMessage);
    }

    // Phương thức tạo nội dung HTML cho OTP (để giữ lại chức năng cũ)
    private String getHtmlContent(String email, String otp, String linkTemplate) {
        String verifyLink = String.format(linkTemplate, email, otp);
        return String.format("""
                <html>
                  <body>
                    <p>Chào bạn,</p>
                    <p>Vui lòng bấm vào link bên dưới:</p>
                    <a href="%s" target="_blank">Click để thực hiện</a>
                    <p>OTP của bạn là: <b>%s</b></p>
                  </body>
                </html>
                """, verifyLink, otp);
    }

    @Async("taskExecutor")
    public void sendEmailAsync(String email, String otp, Token.TokenType type) {
        try {
            switch (type) {
                case REGISTRATION:
                    sendOtpEmail(email, otp);
                    break;
                case RESET_PASSWORD:
                    sendResetPasswordEmail(email, otp);
                    break;
                default:
                    log.warn("Loại token không được hỗ trợ: {}", type);
            }
        } catch (Exception e) {
            log.error("Gửi email thất bại đến {}: {}", email, e.getMessage(), e);
        }
    }

    // Gửi email OTP cho đăng ký
    private void sendOtpEmail(String email, String otp) throws MessagingException {
        String subject = "Xác thực tài khoản";
        String htmlContent = getHtmlContent(email, otp, "http://localhost:8080/auth/verify?email=%s&otp=%s");
        sendEmail(email, subject, htmlContent);
        log.info("Gửi email OTP thành công đến: {}", email);
    }

    // Gửi email OTP cho reset password
    private void sendResetPasswordEmail(String email, String otp) throws MessagingException {
        String subject = "Đặt lại mật khẩu";
        String htmlContent = getHtmlContent(email, otp, "http://localhost:8080/auth/reset-password?email=%s&otp=%s");
        sendEmail(email, subject, htmlContent);
        log.info("Gửi email đặt lại mật khẩu thành công đến: {}", email);
    }
}
