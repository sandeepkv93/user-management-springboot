package com.example.usermanagement.dto;

import lombok.Data;

@Data
public class TokenRefreshRequest {
  private String refreshToken;
}
