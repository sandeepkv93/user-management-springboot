package com.example.usermanagement.config;

import com.example.usermanagement.entity.Role;
import com.example.usermanagement.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {
  private final RoleRepository roleRepository;

  @Override
  public void run(String... args) {
    try {
      if (roleRepository.count() == 0) {
        log.info("Initializing roles...");

        Role userRole = new Role();
        userRole.setName(Role.RoleName.ROLE_USER);
        roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setName(Role.RoleName.ROLE_ADMIN);
        roleRepository.save(adminRole);

        log.info("Roles initialized successfully");
      } else {
        log.info("Roles already initialized");
      }
    } catch (Exception e) {
      log.error("Error initializing roles", e);
      throw e;
    }
  }
}
