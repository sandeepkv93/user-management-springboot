package com.example.usermanagement.service;

import com.example.usermanagement.dto.UpdateUserRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final S3Service s3Service;

  public UserResponse getUserProfile(UserPrincipal currentUser) {
    User user =
        userRepository
            .findById(currentUser.getId())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return UserResponse.fromUser(user);
  }

  @Transactional
  public UserResponse updateUser(UserPrincipal currentUser, UpdateUserRequest updateRequest) {
    User user =
        userRepository
            .findById(currentUser.getId())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    if (updateRequest.getUsername() != null
        && !updateRequest.getUsername().equals(user.getUsername())
        && userRepository.existsByUsername(updateRequest.getUsername())) {
      throw new RuntimeException("Username is already taken");
    }

    if (updateRequest.getUsername() != null) {
      user.setUsername(updateRequest.getUsername());
    }

    User updatedUser = userRepository.save(user);
    return UserResponse.fromUser(updatedUser);
  }

  @Transactional
  public UserResponse updateProfilePicture(UserPrincipal currentUser, MultipartFile file) {
    User user =
        userRepository
            .findById(currentUser.getId())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    // Delete old profile picture if exists
    if (user.getProfilePictureUrl() != null) {
      s3Service.deleteFile(user.getProfilePictureUrl());
    }

    String pictureUrl = s3Service.uploadFile(file, "profile-pictures/" + user.getId());
    user.setProfilePictureUrl(pictureUrl);

    User updatedUser = userRepository.save(user);
    return UserResponse.fromUser(updatedUser);
  }

  public UserResponse deleteProfilePicture(UserPrincipal currentUser) {
    User user =
        userRepository
            .findById(currentUser.getId())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    if (user.getProfilePictureUrl() != null) {
      s3Service.deleteFile(user.getProfilePictureUrl());
      user.setProfilePictureUrl(null);
      User updatedUser = userRepository.save(user);
      return UserResponse.fromUser(updatedUser);
    }

    return UserResponse.fromUser(user);
  }
}
