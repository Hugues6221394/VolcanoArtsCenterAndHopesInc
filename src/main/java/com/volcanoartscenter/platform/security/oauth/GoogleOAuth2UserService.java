package com.volcanoartscenter.platform.security.oauth;

import com.volcanoartscenter.platform.shared.model.Role;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.repository.RoleRepository;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = new DefaultOAuth2UserService().loadUser(userRequest);
        Map<String, Object> attrs = oauthUser.getAttributes();

        String email = normalizeEmail((String) attrs.get("email"));
        if (email == null) {
            throw new InternalAuthenticationServiceException("Google account email is required.");
        }

        String givenName = firstNonBlank((String) attrs.get("given_name"), firstToken((String) attrs.get("name")), "Guest");
        String familyName = firstNonBlank((String) attrs.get("family_name"), remainingTokens((String) attrs.get("name")), "User");
        String pictureUrl = (String) attrs.get("picture");

        User user = userRepository.findByEmail(email).orElseGet(() -> createRegisteredClient(email, givenName, familyName, pictureUrl));

        // Keep profile data fresh from Google while preserving user customizations when already set.
        if (isBlank(user.getFirstName())) user.setFirstName(givenName);
        if (isBlank(user.getLastName())) user.setLastName(familyName);
        if (isBlank(user.getProfileImageUrl()) && !isBlank(pictureUrl)) user.setProfileImageUrl(pictureUrl);
        if (!Boolean.TRUE.equals(user.getEnabled())) user.setEnabled(true);
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        }
        userRepository.save(user);

        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName))
                .map(GrantedAuthority.class::cast)
                .toList();

        Map<String, Object> principalAttrs = new HashMap<>(attrs);
        principalAttrs.put("email", user.getEmail());
        principalAttrs.put("name", user.getFullName());

        return new DefaultOAuth2User(authorities, principalAttrs, "email");
    }

    private User createRegisteredClient(String email, String firstName, String lastName, String pictureUrl) {
        Role clientRole = roleRepository.findByName("REGISTERED_CLIENT")
                .orElseThrow(() -> new IllegalStateException("REGISTERED_CLIENT role is missing."));
        Set<Role> roles = new HashSet<>();
        roles.add(clientRole);
        return userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .firstName(firstName)
                .lastName(lastName)
                .profileImageUrl(pictureUrl)
                .enabled(true)
                .roles(roles)
                .build());
    }

    private static String normalizeEmail(String email) {
        if (email == null) return null;
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) return value.trim();
        }
        return null;
    }

    private static String firstToken(String value) {
        if (isBlank(value)) return null;
        String[] parts = value.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : null;
    }

    private static String remainingTokens(String value) {
        if (isBlank(value)) return null;
        String[] parts = value.trim().split("\\s+");
        if (parts.length <= 1) return null;
        return String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
