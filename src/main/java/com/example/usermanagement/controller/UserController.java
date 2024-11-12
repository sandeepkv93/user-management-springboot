package com.example.usermanagement.controller;

import com.example.usermanagement.dto.UpdateUserRequest;
import com.example.usermanagement.dto.UserResponse;
import com.example.usermanagement.security.CurrentUser;
import com.example.usermanagement.security.UserPrincipal;
import com.example.usermanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/me")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<UserResponse> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
    return ResponseEntity.ok(userService.getUserProfile(currentUser));
  }

  @PutMapping("/me")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<UserResponse> updateCurrentUser(
      @CurrentUser UserPrincipal currentUser, @Valid @RequestBody UpdateUserRequest updateRequest) {
    return ResponseEntity.ok(userService.updateUser(currentUser, updateRequest));
  }

  @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<UserResponse> updateProfilePicture(
      @CurrentUser UserPrincipal currentUser, @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(userService.updateProfilePicture(currentUser, file));
  }

  @DeleteMapping("/me/profile-picture")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<UserResponse> deleteProfilePicture(@CurrentUser UserPrincipal currentUser) {
    return ResponseEntity.ok(userService.deleteProfilePicture(currentUser));
  }
}
