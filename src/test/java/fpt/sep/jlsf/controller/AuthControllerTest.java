package fpt.sep.jlsf.controller;

import fpt.sep.apjf.controller.AuthController;
import fpt.sep.apjf.dto.ApiResponseDTO;
import fpt.sep.apjf.dto.LoginDTO;
import fpt.sep.apjf.dto.RegisterDTO;
import fpt.sep.apjf.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @Test
    void testLoginSuccess() {
        LoginDTO loginDTO = new LoginDTO("email@example.com", "password");
        when(userService.login(loginDTO)).thenReturn("Login successful");

        ResponseEntity<ApiResponseDTO> response = authController.login(loginDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Login successful", Objects.requireNonNull(response.getBody()).message());
        assertTrue(response.getBody().success());
    }

    @Test
    void testRegister() {
        RegisterDTO registerDTO = new RegisterDTO("phan", "email@example.com", "123456");
        ResponseEntity<ApiResponseDTO> response = authController.register(registerDTO);

        verify(userService).register(registerDTO);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("User registered. Please verify with OTP.", response.getBody().message());
    }


    @Test
    void testRegisterFalse() {
        //  input
        RegisterDTO badDto = new RegisterDTO("phan", "email", "123456");

        // 2. Stub for userService.register(...) throw IllegalArgumentException
        doThrow(new IllegalArgumentException("Email invalid"))
                .when(userService).register(badDto);

        // 3. call controller
        ResponseEntity<ApiResponseDTO> response = authController.register(badDto);

        // 4. Verify & Assert
        verify(userService).register(badDto);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Email invalid", response.getBody().message());
    }
}



