package com.example.security;

import com.example.exception.UnauthorizedException;
import com.example.models.user.User;
import com.example.repository.UserRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;

/**
 * The customer who sent the current request. This is the only class that knows how an
 * SSO login looks, so connecting the SSO later means changing this class and nothing else.
 */
@RequestScoped
public class CurrentUser {
    @Inject
    HttpHeaders headers;

    @Inject
    UserRepository userRepository;

    /**
     * Stores the user on their first request and keeps email and names in sync
     * with what the SSO reports afterwards.
     *
     * @throws UnauthorizedException if nobody is logged in
     */
    public User get() {
        // TODO SSO: stand-in until the SSO is known. Anyone can send these headers,
        //  so they must be replaced by the claims of the verified SSO token.
        String subject = headers.getHeaderString("X-User-Subject");
        String email = headers.getHeaderString("X-User-Email");
        String firstName = headers.getHeaderString("X-User-First-Name");
        String lastName = headers.getHeaderString("X-User-Last-Name");

        if (subject == null || email == null || firstName == null || lastName == null) {
            throw new UnauthorizedException("You have to be logged in");
        }
        return userRepository.saveUser(new User(0, subject, email, firstName, lastName));
    }
}
