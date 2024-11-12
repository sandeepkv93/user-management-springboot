package com.example.usermanagement.controller;

import com.example.usermanagement.dto.*;
import com.example.usermanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/signup")
  public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
    return ResponseEntity.ok(authService.signup(request));
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<TokenRefreshResponse> refreshToken(
      @RequestBody TokenRefreshRequest request) {
    return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestBody String email) {
    authService.logout(email);
    return ResponseEntity.ok().build();
  }
}
