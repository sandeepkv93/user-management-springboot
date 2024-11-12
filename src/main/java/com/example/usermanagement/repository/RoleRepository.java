package com.example.usermanagement.repository;

import com.example.usermanagement.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(Role.RoleName name);
}
