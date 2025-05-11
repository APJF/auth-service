    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponseDTO resetPassword(@RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return new ApiResponseDTO(true, "Password reset successfully.");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        try {
            String message = userService.login(loginDTO);
            return ResponseEntity.ok(new ApiResponseDTO(true, message));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO(false, e.getMessage()));
        }
    }
