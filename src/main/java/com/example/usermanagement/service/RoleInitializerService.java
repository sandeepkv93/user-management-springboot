package com.example.usermanagement.service;

import com.example.usermanagement.entity.Role;
import com.example.usermanagement.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleInitializerService {
  private final RoleRepository roleRepository;

  @PostConstruct
  @Transactional
  public void initRoles() {
    try {
      if (roleRepository.count() == 0) {
        log.info("Initializing roles...");

        List<Role> roles =
            Arrays.asList(
                createRole(Role.RoleName.ROLE_USER), createRole(Role.RoleName.ROLE_ADMIN));

        roleRepository.saveAll(roles);
        log.info("Roles initialized successfully");
      } else {
        log.info("Roles already initialized");
      }
    } catch (Exception e) {
      log.error("Error initializing roles", e);
      throw e;
    }
  }

  private Role createRole(Role.RoleName name) {
    Role role = new Role();
    role.setName(name);
    return role;
  }
}
