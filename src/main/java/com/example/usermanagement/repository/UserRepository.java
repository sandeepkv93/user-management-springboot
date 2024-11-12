package com.example.usermanagement.repository;

import com.example.usermanagement.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  Optional<User> findByUsername(String username);

  Boolean existsByEmail(String email);

  Boolean existsByUsername(String username);
}
