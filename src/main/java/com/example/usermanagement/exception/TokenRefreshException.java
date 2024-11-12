package com.example.usermanagement.exception;

public class TokenRefreshException extends RuntimeException {
  public TokenRefreshException(String message) {
    super("Failed to refresh token: " + message);
  }
}
