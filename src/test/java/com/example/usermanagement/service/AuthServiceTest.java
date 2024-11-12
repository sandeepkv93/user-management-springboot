package com.example.usermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.example.usermanagement.dto.*;
import com.example.usermanagement.entity.RefreshToken;
import com.example.usermanagement.entity.Role;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.TokenRefreshException;
import com.example.usermanagement.exception.UsernameAlreadyExistsException;
import com.example.usermanagement.repository.RefreshTokenRepository;
import com.example.usermanagement.repository.RoleRepository;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.security.JwtTokenProvider;
import com.example.usermanagement.security.UserPrincipal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock private AuthenticationManager authenticationManager;
  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtTokenProvider tokenProvider;
  @Mock private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks private AuthService authService;

  private Role createUserRole() {
    Role role = new Role();
    role.setId(1L);
    role.setName(Role.RoleName.ROLE_USER);
    return role;
  }

  @Nested
  @DisplayName("Signup Tests")
  class SignupTests {
    private SignupRequest validSignupRequest;
    private Role userRole;

    @BeforeEach
    void setUp() {
      validSignupRequest = new SignupRequest();
      validSignupRequest.setUsername("testuser");
      validSignupRequest.setEmail("test@example.com");
      validSignupRequest.setPassword("password123");

      userRole = createUserRole();
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void shouldRegisterNewUser() {
      // Arrange
      when(userRepository.existsByUsername(anyString())).thenReturn(false);
      when(userRepository.existsByEmail(anyString())).thenReturn(false);
      when(roleRepository.findByName(Role.RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
      when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
      when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

      // Act
      SignupResponse response = authService.signup(validSignupRequest);

      // Assert
      assertThat(response.isSuccess()).isTrue();
      assertThat(response.getMessage()).isEqualTo("User registered successfully");
      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void shouldThrowExceptionWhenUsernameExists() {
      // Arrange
      when(userRepository.existsByUsername(anyString())).thenReturn(true);

      // Act & Assert
      org.junit.jupiter.api.Assertions.assertThrows(
          UsernameAlreadyExistsException.class, () -> authService.signup(validSignupRequest));
      verify(userRepository, never()).save(any(User.class));
    }
  }

  @Nested
  @DisplayName("Login Tests")
  class LoginTests {
    private LoginRequest validLoginRequest;
    private Authentication authentication;
    private UserPrincipal userPrincipal;
    private User user;

    @BeforeEach
    void setUp() {
      validLoginRequest = new LoginRequest();
      validLoginRequest.setEmail("test@example.com");
      validLoginRequest.setPassword("password123");

      Set<Role> roles = new HashSet<>();
      roles.add(createUserRole());

      user = User.builder().id(1L).email("test@example.com").roles(roles).build();

      userPrincipal = UserPrincipal.create(user);
      authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null);
    }

    @Test
    @DisplayName("Should successfully login user")
    void shouldLoginSuccessfully() {
      // Arrange
      when(authenticationManager.authenticate(any(Authentication.class)))
          .thenReturn(authentication);
      when(tokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("access-token");
      // Mock the user repository findById call
      when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
      when(refreshTokenRepository.save(any(RefreshToken.class)))
          .thenAnswer(
              i -> {
                RefreshToken token = (RefreshToken) i.getArgument(0);
                token.setId(1L);
                return token;
              });

      // Act
      LoginResponse response = authService.login(validLoginRequest);

      // Assert
      assertThat(response.getAccessToken()).isEqualTo("access-token");
      assertThat(response.getRefreshToken()).isNotNull();
      verify(authenticationManager).authenticate(any(Authentication.class));
      verify(tokenProvider).generateAccessToken(any(Authentication.class));
      verify(userRepository).findById(anyLong());
      verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
  }

  @Nested
  @DisplayName("Refresh Token Tests")
  class RefreshTokenTests {
    private RefreshToken validRefreshToken;
    private User user;

    @BeforeEach
    void setUp() {
      Set<Role> roles = new HashSet<>();
      roles.add(createUserRole());

      user = User.builder().id(1L).email("test@example.com").roles(roles).build();

      validRefreshToken =
          RefreshToken.builder()
              .id(1L)
              .user(user)
              .token("valid-refresh-token")
              .expiryDate(Instant.now().plusSeconds(3600))
              .build();
    }

    @Test
    @DisplayName("Should successfully refresh token")
    void shouldRefreshTokenSuccessfully() {
      // Arrange
      when(refreshTokenRepository.findByToken(anyString()))
          .thenReturn(Optional.of(validRefreshToken));
      when(tokenProvider.generateAccessToken(any(Authentication.class)))
          .thenReturn("new-access-token");

      // Act
      var response = authService.refreshToken("valid-refresh-token");

      // Assert
      assertThat(response.getAccessToken()).isEqualTo("new-access-token");
      assertThat(response.getRefreshToken()).isEqualTo("valid-refresh-token");
    }

    @Test
    @DisplayName("Should throw exception when refresh token is expired")
    void shouldThrowExceptionWhenRefreshTokenExpired() {
      // Arrange
      RefreshToken expiredToken =
          RefreshToken.builder()
              .id(1L)
              .user(user)
              .token("expired-token")
              .expiryDate(Instant.now().minusSeconds(3600))
              .build();

      when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(expiredToken));

      // Act & Assert
      org.junit.jupiter.api.Assertions.assertThrows(
          TokenRefreshException.class, () -> authService.refreshToken("expired-token"));
      verify(refreshTokenRepository).delete(expiredToken);
    }
  }

  @Nested
  @DisplayName("Logout Tests")
  class LogoutTests {
    private User user;

    @BeforeEach
    void setUp() {
      Set<Role> roles = new HashSet<>();
      roles.add(createUserRole());

      user = User.builder().id(1L).email("test@example.com").roles(roles).build();
    }

    @Test
    @DisplayName("Should successfully logout user")
    void shouldLogoutSuccessfully() {
      // Arrange
      when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
      doNothing().when(refreshTokenRepository).deleteByUser(any(User.class));

      // Act
      authService.logout("test@example.com");

      // Assert
      verify(userRepository).findByEmail("test@example.com");
      verify(refreshTokenRepository).deleteByUser(user);
    }
  }
}
