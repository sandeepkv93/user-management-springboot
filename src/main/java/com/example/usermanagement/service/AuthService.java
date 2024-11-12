package com.example.usermanagement.service;

import com.example.usermanagement.dto.*;
import com.example.usermanagement.entity.RefreshToken;
import com.example.usermanagement.entity.Role;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.*;
import com.example.usermanagement.repository.RefreshTokenRepository;
import com.example.usermanagement.repository.RoleRepository;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.security.JwtTokenProvider;
import com.example.usermanagement.security.UserPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider tokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;

  @Transactional
  public SignupResponse signup(SignupRequest request) {
    // Validate username and email
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new UsernameAlreadyExistsException(request.getUsername());
    }
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new EmailAlreadyExistsException(request.getEmail());
    }

    // Get user role
    Role userRole =
        roleRepository
            .findByName(Role.RoleName.ROLE_USER)
            .orElseThrow(() -> new ResourceNotFoundException("Default user role not found"));

    // Create new user
    User user =
        User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .provider(User.Provider.LOCAL)
            .roles(Set.of(userRole))
            .build();

    userRepository.save(user);

    return new SignupResponse(true, "User registered successfully");
  }

  @Transactional
  public LoginResponse login(LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
    String accessToken = tokenProvider.generateAccessToken(authentication);
    RefreshToken refreshToken = createRefreshToken(userPrincipal.getId());

    return new LoginResponse(accessToken, refreshToken.getToken());
  }

  @Transactional
  public RefreshToken createRefreshToken(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);

    RefreshToken refreshToken =
        RefreshToken.builder()
            .user(user)
            .token(UUID.randomUUID().toString())
            .expiryDate(Instant.now().plusMillis(tokenProvider.getRefreshTokenExpiration()))
            .build();

    return refreshTokenRepository.save(refreshToken);
  }

  @Transactional
  public TokenRefreshResponse refreshToken(String refreshToken) {
    return refreshTokenRepository
        .findByToken(refreshToken)
        .map(this::verifyRefreshToken)
        .map(RefreshToken::getUser)
        .map(
            user -> {
              // Create proper authorities from user roles
              List<SimpleGrantedAuthority> authorities =
                  user.getRoles().stream()
                      .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                      .collect(Collectors.toList());

              UserPrincipal userPrincipal = UserPrincipal.create(user);

              // Create authentication with proper authorities
              Authentication authentication =
                  new UsernamePasswordAuthenticationToken(userPrincipal, null, authorities);

              String accessToken = tokenProvider.generateAccessToken(authentication);
              return new TokenRefreshResponse(accessToken, refreshToken);
            })
        .orElseThrow(() -> new TokenRefreshException("Invalid refresh token"));
  }

  private RefreshToken verifyRefreshToken(RefreshToken token) {
    if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
      refreshTokenRepository.delete(token);
      throw new TokenRefreshException("Refresh token was expired");
    }
    return token;
  }

  @Transactional
  public void logout(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    refreshTokenRepository.deleteByUser(user);
  }
}
