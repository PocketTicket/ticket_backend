package com.example.security;

import com.example.models.user.SsoProvider;
import com.example.models.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Logs customers in via IServ or Moodle with the OpenID Connect authorization code flow:
 * <ol>
 *     <li>The browser is sent to {@link #authorizationUri}, where the customer logs in.</li>
 *     <li>IServ/Moodle send the browser back to /auth/{provider}/callback with a one-time code.</li>
 *     <li>{@link #fetchUser} trades the code for an access token, using the client secret, and
 *     asks the provider who the customer is. Both requests go straight from this backend to the
 *     provider, so their answers can be trusted without checking token signatures.</li>
 * </ol>
 */
@ApplicationScoped
public class SsoClient {

    private static final String SCOPES = "openid profile email";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    @Inject
    SsoConfig config;

    @Inject
    ObjectMapper objectMapper;

    /** The providers that are set up in .env, i.e. the login buttons the website shows. */
    public List<SsoProvider> getConfiguredProviders() {
        return Arrays.stream(SsoProvider.values())
                .filter(provider -> settings(provider).isPresent())
                .toList();
    }

    /**
     * @param state        a random value the callback has to bring back, see AuthController
     * @param codeVerifier PKCE: a random secret. Only its hash is sent here, the secret itself
     *                     only with the token request, so a stolen code is useless without it
     */
    public URI authorizationUri(SsoProvider provider, String state, String codeVerifier) {
        SsoConfig.Provider settings = requireSettings(provider);

        Map<String, String> query = new LinkedHashMap<>();
        query.put("response_type", "code");
        query.put("client_id", settings.clientId());
        query.put("redirect_uri", redirectUri(provider));
        query.put("scope", SCOPES);
        query.put("state", state);
        query.put("code_challenge", codeChallenge(codeVerifier));
        query.put("code_challenge_method", "S256");

        return URI.create(settings.url() + endpoints(provider).authorize() + "?" + formEncode(query));
    }

    /**
     * @return the customer as the provider knows them; the userId is not set yet
     * @throws IllegalStateException if the provider rejects the code or leaves out a required claim
     */
    public User fetchUser(SsoProvider provider, String code, String codeVerifier) {
        SsoConfig.Provider settings = requireSettings(provider);
        Endpoints endpoints = endpoints(provider);

        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", redirectUri(provider));
        form.put("client_id", settings.clientId());
        form.put("client_secret", settings.clientSecret());
        form.put("code_verifier", codeVerifier);

        JsonNode token = send(HttpRequest.newBuilder(URI.create(settings.url() + endpoints.token()))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(form)))
                .build());

        JsonNode userInfo = send(HttpRequest.newBuilder(URI.create(settings.url() + endpoints.userInfo()))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + claim(token, "access_token"))
                .GET()
                .build());

        return new User(
                0,
                provider,
                claim(userInfo, "sub"),
                claim(userInfo, "email"),
                claim(userInfo, "given_name"),
                claim(userInfo, "family_name")
        );
    }

    /** Paths below the provider's address, taken from its documentation. */
    private static Endpoints endpoints(SsoProvider provider) {
        return switch (provider) {
            // https://doku.iserv.de/manage/system/sso/
            case ISERV -> new Endpoints("/iserv/auth/auth", "/iserv/auth/public/token", "/iserv/auth/userinfo");
            // The Moodle plugin "OAuth2 Server" (local_oauth2), which the Moodle admin has to install
            case MOODLE -> new Endpoints("/local/oauth2/login.php", "/local/oauth2/token.php", "/local/oauth2/userinfo.php");
        };
    }

    private Optional<SsoConfig.Provider> settings(SsoProvider provider) {
        return switch (provider) {
            case ISERV -> config.iserv();
            case MOODLE -> config.moodle();
        };
    }

    private SsoConfig.Provider requireSettings(SsoProvider provider) {
        return settings(provider)
                .orElseThrow(() -> new IllegalStateException("Login via " + provider + " is not set up"));
    }

    private String redirectUri(SsoProvider provider) {
        return config.backendUrl() + "/auth/" + provider.pathName() + "/callback";
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        request.uri() + " answered HTTP " + response.statusCode() + ": " + response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not reach " + request.uri(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling " + request.uri(), e);
        }
    }

    private static String claim(JsonNode json, String name) {
        JsonNode value = json.get(name);

        if (value == null || value.asText().isBlank()) {
            throw new IllegalStateException("The SSO answer has no \"" + name + "\"");
        }
        return value.asText();
    }

    private static String codeChallenge(String codeVerifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java runtime has SHA-256", e);
        }
    }

    private static String formEncode(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private record Endpoints(String authorize, String token, String userInfo) { }
}
