package com.renthub.auth.service;

import com.renthub.auth.model.dto.*;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(String refreshToken);
    UserDto getCurrentProfile();
    UserDto updateProfile(UpdateProfileRequest request);
    BecomeOwnerResponse becomeOwner(BecomeOwnerRequest request);
}
