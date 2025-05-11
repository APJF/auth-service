package fpt.sep.jlsf.dto;

public record ApiResponseDTO(
        boolean success,
        String message,
        String errorCode, // Thêm mã lỗi (nếu thất bại)
        Object data // Thêm dữ liệu bổ sung (nếu thành công)
) {
    // Constructor cho trường hợp không cần errorCode và data
    public ApiResponseDTO(boolean success, String message) {
        this(success, message, null, null);
    }
}