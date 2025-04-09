package javaloginmodule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javaloginmodule.model.UserDetailsResponse;
import javaloginmodule.model.UserRequest;
import javaloginmodule.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    public void register_returnSuccessResponse_ifValidRegistration() throws Exception {
        UserRequest request = new UserRequest("sam", "Password123");
        UserDetailsResponse mockResponse = new UserDetailsResponse(1, "sam", LocalDateTime.now());

        Mockito.when(authService.register(Mockito.any(UserRequest.class))).thenReturn(Optional.of(mockResponse));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("sam"));
    }
}
