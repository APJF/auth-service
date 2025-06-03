package fpt.sep.jlsf.utils;

import fpt.sep.jlsf.entity.VerifyToken;
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
    public void sendEmailAsync(String email, String otp, VerifyToken.VerifyTokenType type) {
        try {
            switch (type) {
                case REGISTRATION:
                    sendOtpEmail(email, otp);
                    break;
                case RESET_PASSWORD:
                    sendSetPassword(email, otp);
                    break;
                case VERIFY_EMAIL:
                    sendOtpEmail(email, otp);
                    break;
                default:
                    sendOtpEmail(email, otp);
            }
        } catch (Exception e) {
            log.error("Error sending {} email: {}", type, e.getMessage(), e);
        }
    }

    public void sendOtpEmail(String email, String otp) throws MessagingException {
        String linkTemplate = "http://localhost:8080/auth/verify-account?email=%s&otp=%s";
        sendEmail(email, "Email Verification", getHtmlContent(email, otp, linkTemplate));
    }

    public void sendSetPassword(String email, String otp) throws MessagingException {
        String linkTemplate = "http://localhost:8080/auth/reset-password?email=%s&otp=%s";
        sendEmail(email, "Reset Password", getHtmlContent(email, otp, linkTemplate));
    }
}
