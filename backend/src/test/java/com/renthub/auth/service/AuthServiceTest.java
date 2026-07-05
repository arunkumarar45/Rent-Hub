package com.renthub.auth.service;

import com.renthub.auth.mapper.UserMapper;
import com.renthub.auth.model.dto.*;
import com.renthub.auth.model.entity.*;
import com.renthub.auth.repository.RefreshTokenRepository;
import com.renthub.auth.repository.RoleRepository;
import com.renthub.auth.repository.UserRepository;
import com.renthub.common.config.JwtTokenProvider;
import com.renthub.common.exception.BadRequestException;
import com.renthub.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private Role sampleRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenDurationMs", 604800000L);

        sampleRole = Role.builder().id(1L).name(RoleName.ROLE_CUSTOMER).build();
        sampleUser = User.builder()
                .id(100L)
                .email("test@renthub.com")
                .passwordHash("hashed_secret")
                .firstName("Test")
                .lastName("User")
                .phone("1234567890")
                .isVerified(true)
                .isOwner(false)
                .roles(new HashSet<>(Set.of(sampleRole)))
                .build();
    }

    @Test
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@renthub.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .phone("1234567890")
                .build();

        when(userRepository.existsByEmail("test@renthub.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(sampleRole));
        when(passwordEncoder.encode("password123")).thenReturn("hashed_secret");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("access_token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> {
            RefreshToken token = i.getArgument(0);
            token.setId(1L);
            return token;
        });
        when(userMapper.toDto(any(User.class))).thenReturn(UserDto.builder()
                .id(100L)
                .email("test@renthub.com")
                .firstName("Test")
                .lastName("User")
                .isOwner(false)
                .build());

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(100L, response.getUser().getId());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_DuplicateEmail_ThrowsBadRequestException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@renthub.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();

        when(userRepository.existsByEmail("test@renthub.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("test@renthub.com")
                .password("password123")
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken("test@renthub.com", "password123");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("login_access_token");
        when(userRepository.findByEmail("test@renthub.com")).thenReturn(Optional.of(sampleUser));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> {
            RefreshToken token = i.getArgument(0);
            token.setId(2L);
            return token;
        });
        when(userMapper.toDto(any(User.class))).thenReturn(UserDto.builder()
                .id(100L)
                .email("test@renthub.com")
                .build());

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("login_access_token", response.getAccessToken());
        verify(refreshTokenRepository, times(1)).deleteByUser(sampleUser);
    }
}
