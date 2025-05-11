    private void createAndSendToken(User user, VerifyTokenType type) {
        tokenRepository.deleteAllByUserAndType(user, type);
        LocalDateTime now = LocalDateTime.now();
        String otp = otpUtil.generateOTP();
        VerifyToken token = VerifyToken.builder()
                .user(user)
                .token(otp)
                .type(type)
                .requestedTime(now)
                .expirationTime(now.plus(OTP_TTL))
                .build();
        tokenRepository.save(token);
        emailUtil.sendEmailAsync(user.getEmail(), otp, type);
    }
