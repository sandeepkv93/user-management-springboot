package com.example.usermanagement.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
  @Size(min = 3, max = 20)
  private String username;
}
