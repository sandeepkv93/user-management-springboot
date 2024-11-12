package com.example.usermanagement.security.oauth2;

import com.example.usermanagement.entity.Role;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.entity.User.Provider;
import com.example.usermanagement.exception.OAuth2AuthenticationProcessingException;
import com.example.usermanagement.exception.ResourceNotFoundException;
import com.example.usermanagement.repository.RoleRepository;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.security.UserPrincipal;
import com.example.usermanagement.security.oauth2.userinfo.OAuth2UserInfo;
import java.util.HashSet;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest)
      throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

    try {
      return processOAuth2User(oAuth2UserRequest, oAuth2User);
    } catch (Exception ex) {
      throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
    }
  }

  private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
    try {
      OAuth2UserInfo oAuth2UserInfo =
          OAuth2UserInfoFactory.getOAuth2UserInfo(
              oAuth2UserRequest.getClientRegistration().getRegistrationId(),
              oAuth2User.getAttributes());

      if (oAuth2UserInfo.getEmail() == null || oAuth2UserInfo.getEmail().isEmpty()) {
        throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
      }

      Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
      User user;

      if (userOptional.isPresent()) {
        user = userOptional.get();
        if (!user.getProvider()
            .equals(
                Provider.valueOf(
                    oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase()))) {
          throw new OAuth2AuthenticationProcessingException(
              "Looks like you're signed up with "
                  + user.getProvider()
                  + ". Please use your "
                  + user.getProvider()
                  + " account to login.");
        }
        user = updateExistingUser(user, oAuth2UserInfo);
      } else {
        user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
      }

      return UserPrincipal.create(user, oAuth2User.getAttributes());

    } catch (Exception ex) {
      throw new OAuth2AuthenticationProcessingException(
          "Failed to process OAuth2 user details", ex);
    }
  }

  private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
    try {
      Role userRole =
          roleRepository
              .findByName(Role.RoleName.ROLE_USER)
              .orElseThrow(() -> new ResourceNotFoundException("Default user role not found"));

      User user =
          User.builder()
              .provider(
                  Provider.valueOf(
                      oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase()))
              .providerId(oAuth2UserInfo.getId())
              .username(generateUniqueUsername(oAuth2UserInfo.getEmail()))
              .email(oAuth2UserInfo.getEmail())
              .profilePictureUrl(oAuth2UserInfo.getImageUrl())
              .roles(new HashSet<>())
              .build();

      user.getRoles().add(userRole);
      return userRepository.save(user);
    } catch (Exception ex) {
      throw new OAuth2AuthenticationProcessingException("Failed to register OAuth2 user", ex);
    }
  }

  private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
    existingUser.setEmail(oAuth2UserInfo.getEmail());
    if (oAuth2UserInfo.getImageUrl() != null) {
      existingUser.setProfilePictureUrl(oAuth2UserInfo.getImageUrl());
    }
    return userRepository.save(existingUser);
  }

  private String generateUniqueUsername(String email) {
    String baseUsername = email.split("@")[0];
    String username = baseUsername;
    int counter = 1;

    while (userRepository.existsByUsername(username)) {
      username = baseUsername + counter++;
    }

    return username;
  }
}
