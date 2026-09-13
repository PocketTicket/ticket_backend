package com.example.security;

import com.example.exception.UnauthorizedException;
import com.example.models.user.User;
import com.example.repository.SessionRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;

import java.time.LocalDateTime;

/**
 * The customer who sent the current request, identified by the session cookie that
 * AuthController sets after the login via IServ or Moodle.
 */
@RequestScoped
public class CurrentUser {

    public static final String SESSION_COOKIE = "ticket_session";

    @Inject
    HttpHeaders headers;

    @Inject
    SessionRepository sessionRepository;

    /**
     * @throws UnauthorizedException if nobody is logged in or the session has expired
     */
    public User get() {
        Cookie cookie = headers.getCookies().get(SESSION_COOKIE);
        User user = cookie == null ? null : sessionRepository.getUserBySessionToken(cookie.getValue(), LocalDateTime.now());

        if (user == null) {
            throw new UnauthorizedException("You have to be logged in");
        }
        return user;
    }
}
