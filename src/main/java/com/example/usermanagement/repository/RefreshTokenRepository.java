package com.example.usermanagement.repository;

import com.example.usermanagement.entity.RefreshToken;
import com.example.usermanagement.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByToken(String token);

  Optional<RefreshToken> findByUser(User user);

  void deleteByUser(User user);
}
