package com.example.usermanagement.exception;

public class UsernameAlreadyExistsException extends BadRequestException {
  public UsernameAlreadyExistsException(String username) {
    super(String.format("Username %s already exists", username));
  }
}
