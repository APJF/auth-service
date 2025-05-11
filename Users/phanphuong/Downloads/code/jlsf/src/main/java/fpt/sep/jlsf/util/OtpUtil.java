@Component
public class OtpUtil {
    /**
     * Tạo mã OTP ngẫu nhiên 6 chữ số
     * @return Chuỗi OTP 6 chữ số
     */
    public String generateOTP() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }
    
    /**
     * Kiểm tra tính hợp lệ của OTP
     * @param storedOTP OTP đã lưu trong hệ thống
     * @param providedOTP OTP do người dùng cung cấp
     * @return true nếu OTP hợp lệ, false nếu không
     */
    public boolean validateOTP(String storedOTP, String providedOTP) {
        if (storedOTP == null || providedOTP == null) {
            return false;
        }
        return storedOTP.equals(providedOTP);
    }
}
