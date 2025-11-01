package com.adrian.blogweb1.security.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {
    private OAuth2User oAuth2User;
    private String registrationId;
    private String userNameAttributeName;
    private Collection<? extends GrantedAuthority> authorities;

    public CustomOAuth2User(OAuth2User oAuth2User, String registrationId,
                            String userNameAttributeName, Collection<? extends GrantedAuthority> authorities) {
        this.oAuth2User = oAuth2User;
        this.registrationId = registrationId;
        this.userNameAttributeName = userNameAttributeName;
        this.authorities = authorities;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oAuth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities; // Usamos las autoridades de UserSec
    }

    @Override
    public String getName() {
        return oAuth2User.getAttribute(userNameAttributeName);
    }

    public String getEmail() {
        String email = oAuth2User.getAttribute("email");
        if (email == null && "github".equals(registrationId)) {
            email = oAuth2User.getAttribute("id") + "@github-user.com";
        }
        return email;
    }

    public String getUsername() {
        return oAuth2User.getAttribute("login");
    }
}