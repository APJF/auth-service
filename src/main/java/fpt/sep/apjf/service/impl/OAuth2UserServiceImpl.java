package fpt.sep.apjf.service.impl;

import fpt.sep.apjf.entity.Authority;
import fpt.sep.apjf.entity.User;
import fpt.sep.apjf.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class OAuth2UserServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserService userService;

    public OAuth2UserServiceImpl(@Lazy UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // use default OAuth2UserService to load data user from Google
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        Map<String, Object> attributes = oauth2User.getAttributes();
        String email = (String) attributes.get("email");
        String googleName = (String) attributes.get("name");

        // find user by email, if not exist, create new user
        userService.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(googleName);
            newUser.setEmail(email);
            newUser.setEnabled(true);
            newUser.setAuthorities(List.of(new Authority(null, "ROLE_USER")));
            return userService.save(newUser);
        });

        // get authorities from oauth2User
        Collection<? extends GrantedAuthority> authorities = oauth2User.getAuthorities();

        // return OAuth2User, using "name" for identifier to display Google Account's name
        return new DefaultOAuth2User(authorities, attributes, "name");
    }
}