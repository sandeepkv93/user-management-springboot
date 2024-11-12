package com.example.usermanagement.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class JwtTokenProviderTest {

  private JwtTokenProvider tokenProvider;
  private final String jwtSecret = "testSecretKeyWithMinimum256BitsForHS512SignatureAlgorithm";
  private final long accessTokenExpiration = 3600000; // 1 hour
  private final long refreshTokenExpiration = 86400000; // 24 hours

  @BeforeEach
  void setUp() {
    tokenProvider = new JwtTokenProvider(jwtSecret, accessTokenExpiration, refreshTokenExpiration);
  }

  @Test
  @DisplayName("Should generate valid access token")
  void shouldGenerateValidAccessToken() {
    // Arrange
    Authentication authentication = mock(Authentication.class);
    UserPrincipal userPrincipal =
        new UserPrincipal(1L, "test@example.com", "password", Collections.emptyList(), null);
    when(authentication.getPrincipal()).thenReturn(userPrincipal);

    // Act
    String token = tokenProvider.generateAccessToken(authentication);

    // Assert
    assertThat(token).isNotNull();
    assertTrue(tokenProvider.validateToken(token));
    assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should generate valid refresh token")
  void shouldGenerateValidRefreshToken() {
    // Arrange
    Long userId = 1L;

    // Act
    String token = tokenProvider.generateRefreshToken(userId);

    // Assert
    assertThat(token).isNotNull();
    assertTrue(tokenProvider.validateToken(token));
    assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
  }

  @Test
  @DisplayName("Should validate token correctly")
  void shouldValidateTokenCorrectly() {
    // Arrange
    Authentication authentication = mock(Authentication.class);
    UserPrincipal userPrincipal =
        new UserPrincipal(1L, "test@example.com", "password", Collections.emptyList(), null);
    when(authentication.getPrincipal()).thenReturn(userPrincipal);
    String token = tokenProvider.generateAccessToken(authentication);

    // Act & Assert
    assertTrue(tokenProvider.validateToken(token));
  }

  @Test
  @DisplayName("Should reject invalid token")
  void shouldRejectInvalidToken() {
    // Arrange
    String invalidToken = "invalidToken";

    // Act & Assert
    assertFalse(tokenProvider.validateToken(invalidToken));
  }

  @Test
  @DisplayName("Should extract user id from token")
  void shouldExtractUserIdFromToken() {
    // Arrange
    Authentication authentication = mock(Authentication.class);
    Long userId = 1L;
    UserPrincipal userPrincipal =
        new UserPrincipal(userId, "test@example.com", "password", Collections.emptyList(), null);
    when(authentication.getPrincipal()).thenReturn(userPrincipal);
    String token = tokenProvider.generateAccessToken(authentication);

    // Act
    Long extractedUserId = tokenProvider.getUserIdFromToken(token);

    // Assert
    assertThat(extractedUserId).isEqualTo(userId);
  }
}
