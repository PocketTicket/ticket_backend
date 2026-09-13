package com.example.controller;

import com.example.dto.auth.SsoLogin;
import com.example.dto.user.UserResponse;
import com.example.exception.ResourceNotFoundException;
import com.example.models.user.SsoProvider;
import com.example.security.CurrentUser;
import com.example.service.AuthService;
import io.quarkus.logging.Log;
import io.quarkus.runtime.LaunchMode;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/** The customer login via IServ or Moodle. Admins and door staff use /admin/login instead. */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthController {

    /** Remembers a started login until the browser comes back from IServ/Moodle. */
    private static final String LOGIN_COOKIE = "ticket_sso_login";
    private static final int LOGIN_COOKIE_SECONDS = 10 * 60;

    @Inject
    AuthService authService;

    @ConfigProperty(name = "ticket.frontend-url")
    String frontendUrl;

    /**
     * The login buttons to show, e.g. ["ISERV", "MOODLE"]. Only providers set up in .env are listed.
     */
    @GET
    @Path("/providers")
    public List<SsoProvider> getProviders() {
        return authService.getProviders();
    }

    /**
     * Starts the login. Opened by the browser itself (a link, not fetch), e.g.
     * /auth/iserv/login?returnTo=/cart. After the login the browser ends up at
     * {ticket.frontend-url}{returnTo}.
     *
     * @return a redirect to IServ or Moodle, or 404 if that provider is not set up
     */
    @GET
    @Path("/{provider}/login")
    public Response login(@PathParam("provider") SsoProvider provider, @QueryParam("returnTo") String returnTo) {
        SsoLogin login = authService.startLogin(provider, returnTo);
        String loginCookie = String.join(".", login.state(), login.codeVerifier(), encode(login.returnPath()));

        return Response.seeOther(login.authorizationUri())
                .cookie(cookie(LOGIN_COOKIE, loginCookie, "/", LOGIN_COOKIE_SECONDS))
                .build();
    }

    /**
     * Where IServ and Moodle send the browser back to; register
     * {ticket.sso.backend-url}/auth/iserv/callback or .../auth/moodle/callback there as redirect URI.
     * Always ends on the website: logged in at {ticket.frontend-url}{returnTo}, or at
     * {ticket.frontend-url}/?login=failed.
     */
    @GET
    @Path("/{provider}/callback")
    public Response callback(@PathParam("provider") SsoProvider provider,
                             @QueryParam("code") String code,
                             @QueryParam("state") String state,
                             @CookieParam(LOGIN_COOKIE) String loginCookie) {
        String[] login = loginCookie == null ? new String[0] : loginCookie.split("\\.");

        // The state proves that the login was started in this browser and not by someone
        // else's link. A missing code means the customer cancelled at IServ/Moodle.
        if (login.length != 3 || code == null || !login[0].equals(state)) {
            return loginFailed();
        }

        try {
            String sessionToken = authService.finishLogin(provider, code, login[1]);

            return Response.seeOther(URI.create(frontendUrl + decode(login[2])))
                    .cookie(sessionCookie(sessionToken), cookie(LOGIN_COOKIE, "", "/", 0))
                    .build();
        } catch (RuntimeException e) {
            Log.warnf(e, "Login via %s failed", provider);
            return loginFailed();
        }
    }

    /**
     * The logged-in customer, e.g. to show their name.
     *
     * @return the customer, or 401 if not logged in
     */
    @GET
    @Path("/me")
    public UserResponse getLoggedInUser() {
        return authService.getLoggedInUser();
    }

    /**
     * Ends the session. The customer stays logged in at IServ/Moodle.
     */
    @POST
    @Path("/logout")
    public Response logout(@CookieParam(CurrentUser.SESSION_COOKIE) String sessionToken) {
        authService.logout(sessionToken);
        return Response.noContent()
                .cookie(cookie(CurrentUser.SESSION_COOKIE, "", "/", 0))
                .build();
    }

    /**
     * Development only: logs in as a made-up customer without IServ or Moodle, so the website
     * can be developed locally. Answers 404 unless the backend runs with ./mvnw quarkus:dev.
     */
    @GET
    @Path("/dev/login")
    public Response devLogin(@QueryParam("email") @DefaultValue("kunde@example.com") String email,
                             @QueryParam("firstName") @DefaultValue("Test") String firstName,
                             @QueryParam("lastName") @DefaultValue("Kunde") String lastName) {
        if (LaunchMode.current() != LaunchMode.DEVELOPMENT) {
            throw new ResourceNotFoundException("Not found");
        }
        return Response.noContent()
                .cookie(sessionCookie(authService.devLogin(email, firstName, lastName)))
                .build();
    }

    private Response loginFailed() {
        return Response.seeOther(URI.create(frontendUrl + "/?login=failed"))
                .cookie(cookie(LOGIN_COOKIE, "", "/", 0))
                .build();
    }

    private static NewCookie sessionCookie(String sessionToken) {
        return cookie(CurrentUser.SESSION_COOKIE, sessionToken, "/", (int) AuthService.SESSION_LIFETIME.toSeconds());
    }

    /** JavaScript cannot read these cookies, and other sites cannot send them along with their requests. */
    private static NewCookie cookie(String name, String value, String path, int maxAgeSeconds) {
        return new NewCookie.Builder(name)
                .value(value)
                .path(path)
                .maxAge(maxAgeSeconds)
                .httpOnly(true)
                .secure(true)
                .sameSite(NewCookie.SameSite.LAX)
                .build();
    }

    private static String encode(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
