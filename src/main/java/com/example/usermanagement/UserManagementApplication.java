package com.example.usermanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.example.usermanagement.entity")
@EnableJpaRepositories("com.example.usermanagement.repository")
public class UserManagementApplication {
  public static void main(String[] args) {
    SpringApplication.run(UserManagementApplication.class, args);
  }
}
