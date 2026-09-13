package com.example.dto.auth;

import java.net.URI;

/** A started login: where to send the browser, and what to remember until it comes back. */
public record SsoLogin(
        URI authorizationUri,
        String state,
        String codeVerifier,
        // The path of the website to return to after the login
        String returnPath
) { }
