package com.example.service;

import com.example.dto.auth.SsoLogin;
import com.example.dto.user.UserResponse;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.UserMapper;
import com.example.models.user.SsoProvider;
import com.example.models.user.User;
import com.example.repository.SessionRepository;
import com.example.repository.UserRepository;
import com.example.security.CurrentUser;
import com.example.security.SsoClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/** The customer login via IServ or Moodle, see {@link SsoClient} for how it works. */
@ApplicationScoped
@Transactional
public class AuthService {

    /** How long a customer stays logged in. */
    public static final Duration SESSION_LIFETIME = Duration.ofDays(7);

    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    SsoClient ssoClient;

    @Inject
    UserRepository userRepository;

    @Inject
    SessionRepository sessionRepository;

    @Inject
    CurrentUser currentUser;

    public List<SsoProvider> getProviders() {
        return ssoClient.getConfiguredProviders();
    }

    /**
     * @param returnTo the path of the website to come back to after the login. Anything else,
     *                 e.g. a link to another site, is replaced by "/".
     * @throws ResourceNotFoundException if the provider is not set up
     */
    public SsoLogin startLogin(SsoProvider provider, String returnTo) {
        if (!ssoClient.getConfiguredProviders().contains(provider)) {
            throw new ResourceNotFoundException("Login via " + provider + " is not set up");
        }

        String state = randomToken();
        String codeVerifier = randomToken();

        return new SsoLogin(
                ssoClient.authorizationUri(provider, state, codeVerifier),
                state,
                codeVerifier,
                isWebsitePath(returnTo) ? returnTo : "/"
        );
    }

    /**
     * Finishes the login after IServ/Moodle sent the browser back.
     *
     * @return the token for the session cookie
     */
    public String finishLogin(SsoProvider provider, String code, String codeVerifier) {
        return createSession(ssoClient.fetchUser(provider, code, codeVerifier));
    }

    /** Development only, see AuthController#devLogin. */
    public String devLogin(String email, String firstName, String lastName) {
        return createSession(new User(0, SsoProvider.ISERV, "dev:" + email, email, firstName, lastName));
    }

    public UserResponse getLoggedInUser() {
        return UserMapper.toResponse(currentUser.get());
    }

    public void logout(String sessionToken) {
        if (sessionToken != null) {
            sessionRepository.deleteSession(sessionToken);
        }
    }

    /** Stores the customer, or refreshes their email and names, and opens a session. */
    private String createSession(User ssoUser) {
        User user = userRepository.saveUser(ssoUser);
        LocalDateTime now = LocalDateTime.now();

        sessionRepository.deleteExpiredSessions(now);

        String token = randomToken();
        sessionRepository.createSession(token, user.userId(), now.plus(SESSION_LIFETIME));
        return token;
    }

    /** Only paths on the website itself, so the login cannot be abused to send people to another site. */
    private static boolean isWebsitePath(String path) {
        return path != null && path.matches("/[A-Za-z0-9\\-._~/?=&%]*") && !path.startsWith("//");
    }

    /** 256 random bits, URL safe. */
    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
