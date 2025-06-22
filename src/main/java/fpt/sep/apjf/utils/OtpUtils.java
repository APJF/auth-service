package fpt.sep.apjf.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpUtils {

    public String generateOTP() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }

    public boolean validateOTP(String storedOTP, String providedOTP) {
        if (storedOTP == null || providedOTP == null) {
            return false;
        }
        return storedOTP.equals(providedOTP);
    }
}