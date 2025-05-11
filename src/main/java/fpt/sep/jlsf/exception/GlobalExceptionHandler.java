package fpt.sep.jlsf.exception;

import fpt.sep.jlsf.dto.ApiResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ApiResponseDTO(false, ex.getMessage(), "INVALID_INPUT", null));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponseDTO> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponseDTO(false, ex.getMessage(), "NOT_FOUND", null));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponseDTO> handleAppException(AppException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, ex.getMessage(), "APP_ERROR", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO(false, "Internal Server Error: " + ex.getMessage(), "INTERNAL_ERROR", null));
    }
}