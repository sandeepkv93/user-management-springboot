package com.example.usermanagement.exception;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
  private final LocalDateTime timestamp = LocalDateTime.now();
  private String error;
  private String message;
}
