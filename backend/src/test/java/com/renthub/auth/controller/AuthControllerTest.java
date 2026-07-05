package com.renthub.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renthub.auth.model.dto.*;
import com.renthub.auth.service.AuthService;
import com.renthub.auth.service.CustomUserDetailsService;
import com.renthub.common.config.JwtAuthenticationFilter;
import com.renthub.common.config.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void register_Returns201Created() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("alice@renthub.com")
                .password("secret12345")
                .firstName("Alice")
                .lastName("Smith")
                .phone("5550192")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock.jwt.token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .expiresInMs(86400000L)
                .user(UserDto.builder().id(1L).email("alice@renthub.com").firstName("Alice").lastName("Smith").build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.data.user.email").value("alice@renthub.com"));
    }

    @Test
    void login_Returns200Ok() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("alice@renthub.com")
                .password("secret12345")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock.login.jwt")
                .refreshToken("mock-login-refresh")
                .tokenType("Bearer")
                .user(UserDto.builder().id(1L).email("alice@renthub.com").build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("mock.login.jwt"));
    }
}
