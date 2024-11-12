package com.example.usermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.usermanagement.dto.UpdateUserRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.entity.Role;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.repository.UserRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock private UserRepository userRepository;
  @Mock private S3Service s3Service;

  @InjectMocks private UserService userService;

  private Role createUserRole() {
    Role role = new Role();
    role.setId(1L);
    role.setName(Role.RoleName.ROLE_USER);
    return role;
  }

  private User createUser() {
    Set<Role> roles = new HashSet<>();
    roles.add(createUserRole());

    return User.builder()
        .id(1L)
        .username("testuser")
        .email("test@example.com")
        .profilePictureUrl("profile.jpg")
        .roles(roles)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  private UserPrincipal createUserPrincipal(User user) {
    return UserPrincipal.create(user);
  }

  @Nested
  @DisplayName("Get User Profile Tests")
  class GetUserProfileTests {
    private User user;
    private UserPrincipal currentUser;

    @BeforeEach
    void setUp() {
      user = createUser();
      currentUser = createUserPrincipal(user);
    }

    @Test
    @DisplayName("Should successfully get user profile")
    void shouldGetUserProfile() {
      // Arrange
      when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));

      // Act
      UserResponse response = userService.getUserProfile(currentUser);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.getUsername()).isEqualTo(user.getUsername());
      assertThat(response.getEmail()).isEqualTo(user.getEmail());
      assertThat(response.getProfilePictureUrl()).isEqualTo(user.getProfilePictureUrl());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
      // Arrange
      when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

      // Act & Assert
      org.junit.jupiter.api.Assertions.assertThrows(
          UsernameNotFoundException.class, () -> userService.getUserProfile(currentUser));
    }
  }

  @Nested
  @DisplayName("Update User Tests")
  class UpdateUserTests {
    private User user;
    private UserPrincipal currentUser;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
      user = createUser();
      currentUser = createUserPrincipal(user);
      updateRequest = new UpdateUserRequest();
      updateRequest.setUsername("newusername");
    }

    @Test
    @DisplayName("Should successfully update user")
    void shouldUpdateUser() {
      // Arrange
      when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
      when(userRepository.existsByUsername(anyString())).thenReturn(false);
      when(userRepository.save(any(User.class))).thenReturn(user);

      // Act
      UserResponse response = userService.updateUser(currentUser, updateRequest);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.getUsername()).isEqualTo(updateRequest.getUsername());
      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void shouldThrowExceptionWhenUsernameExists() {
      // Arrange
      when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
      when(userRepository.existsByUsername(anyString())).thenReturn(true);

      // Act & Assert
      org.junit.jupiter.api.Assertions.assertThrows(
          RuntimeException.class, () -> userService.updateUser(currentUser, updateRequest));
      verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should not update if username is unchanged")
    void shouldNotUpdateIfUsernameUnchanged() {
      // Arrange
      when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
      when(userRepository.save(any(User.class))).thenReturn(user);
      updateRequest.setUsername(user.getUsername());

      // Act
      UserResponse response = userService.updateUser(currentUser, updateRequest);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.getUsername()).isEqualTo(user.getUsername());
      verify(userRepository, never()).existsByUsername(anyString());
      verify(userRepository).save(any(User.class));
    }
  }

  @Nested
  @DisplayName("Profile Picture Tests")
  class ProfilePictureTests {
    private User user;
    private UserPrincipal currentUser;
    private MultipartFile profilePicture;

    @BeforeEach
    void setUp() {
      user = createUser();
      currentUser = createUserPrincipal(user);
      profilePicture =
          new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
    }

    @Test
    @DisplayName("Should successfully update profile picture")
    void shouldUpdateProfilePicture() {
      // Arrange
      String newPictureUrl = "new-profile.jpg";
      when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
      when(s3Service.uploadFile(any(), eq("profile-pictures/1"))).thenReturn(newPictureUrl);

      User updatedUser = createUser();
      updatedUser.setProfilePictureUrl(newPictureUrl);
      when(userRepository.save(any(User.class))).thenReturn(updatedUser);

      // Act
      UserResponse response = userService.updateProfilePicture(currentUser, profilePicture);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.getProfilePictureUrl()).isEqualTo(newPictureUrl);
      verify(s3Service).deleteFile("profile.jpg");
      verify(s3Service).uploadFile(any(MultipartFile.class), eq("profile-pictures/1"));
      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully update profile picture when user has no existing picture")
    void shouldUpdateProfilePictureWhenNoExistingPicture() {
      // Arrange
      user.setProfilePictureUrl(null);
      String newPictureUrl = "new-profile.jpg";
      when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
      when(s3Service.uploadFile(any(), eq("profile-pictures/1"))).thenReturn(newPictureUrl);

      User updatedUser = createUser();
      updatedUser.setProfilePictureUrl(newPictureUrl);
      when(userRepository.save(any(User.class))).thenReturn(updatedUser);

      // Act
      UserResponse response = userService.updateProfilePicture(currentUser, profilePicture);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.getProfilePictureUrl()).isEqualTo(newPictureUrl);
      verify(s3Service, never()).deleteFile(anyString());
      verify(s3Service).uploadFile(any(MultipartFile.class), eq("profile-pictures/1"));
      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully delete profile picture")
    void shouldDeleteProfilePicture() {
      // Arrange
      when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));

      User updatedUser = createUser();
      updatedUser.setProfilePictureUrl(null);
      when(userRepository.save(any(User.class))).thenReturn(updatedUser);

      // Act
      UserResponse response = userService.deleteProfilePicture(currentUser);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.getProfilePictureUrl()).isNull();
      verify(s3Service).deleteFile("profile.jpg");
      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle delete profile picture when user has no picture")
    void shouldHandleDeleteProfilePictureWhenNoPicture() {
      // Arrange
      user.setProfilePictureUrl(null);
      when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));

      // Act
      UserResponse response = userService.deleteProfilePicture(currentUser);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.getProfilePictureUrl()).isNull();
      verify(s3Service, never()).deleteFile(anyString());
      verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found during profile picture update")
    void shouldThrowExceptionWhenUserNotFoundDuringProfilePictureUpdate() {
      // Arrange
      when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

      // Act & Assert
      org.junit.jupiter.api.Assertions.assertThrows(
          UsernameNotFoundException.class,
          () -> userService.updateProfilePicture(currentUser, profilePicture));
      verify(s3Service, never()).deleteFile(anyString());
      verify(s3Service, never()).uploadFile(any(), anyString());
      verify(userRepository, never()).save(any(User.class));
    }
  }
}
