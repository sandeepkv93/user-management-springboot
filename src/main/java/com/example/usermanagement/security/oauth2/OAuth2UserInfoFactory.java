package com.example.usermanagement.security.oauth2;

import com.example.usermanagement.exception.OAuth2AuthenticationProcessingException;
import com.example.usermanagement.security.oauth2.userinfo.GithubOAuth2UserInfo;
import com.example.usermanagement.security.oauth2.userinfo.GoogleOAuth2UserInfo;
import com.example.usermanagement.security.oauth2.userinfo.OAuth2UserInfo;
import java.util.Map;

public class OAuth2UserInfoFactory {
  public static OAuth2UserInfo getOAuth2UserInfo(
      String registrationId, Map<String, Object> attributes) {
    return switch (registrationId.toLowerCase()) {
      case "google" -> new GoogleOAuth2UserInfo(attributes);
      case "github" -> new GithubOAuth2UserInfo(attributes);
      default ->
          throw new OAuth2AuthenticationProcessingException(
              "Sorry! Login with " + registrationId + " is not supported yet.");
    };
  }
}
