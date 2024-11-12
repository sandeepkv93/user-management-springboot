package com.example.usermanagement.dto;

import com.example.usermanagement.entity.User;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
  private Long id;
  private String username;
  private String email;
  private String profilePictureUrl;
  private Instant createdAt;
  private Instant updatedAt;

  public static UserResponse fromUser(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .profilePictureUrl(user.getProfilePictureUrl())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }
}
