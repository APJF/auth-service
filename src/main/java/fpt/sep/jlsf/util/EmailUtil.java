package fpt.sep.jlsf.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailUtil {

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

    public void sendOtpEmail(String email, String otp) throws MessagingException {
        String linkTemplate = "http://localhost:8080/auth/verify-account?email=%s&otp=%s";
        sendEmail(email, "Email Verification", getHtmlContent(email, otp, linkTemplate));
    }

    public void sendSetPassword(String email, String otp) throws MessagingException {
        String linkTemplate = "http://localhost:8080/auth/reset-password?email=%s&otp=%s";
        sendEmail(email, "Reset Password", getHtmlContent(email, otp, linkTemplate));
    }
}
