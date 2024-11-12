package com.example.usermanagement.exception;

public class EmailAlreadyExistsException extends BadRequestException {
  public EmailAlreadyExistsException(String email) {
    super(String.format("Email %s already exists", email));
  }
}
