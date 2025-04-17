package fpt.sep.jlsf.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpUtil {
    public String generateOTP() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }
}