package com.example.security;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

/**
 * The SSO settings from application.properties and .env. A provider that is not configured
 * at all is not offered on the website; empty values are not allowed.
 */
@ConfigMapping(prefix = "ticket.sso")
public interface SsoConfig {

    /**
     * The address of this backend as the browser sees it, without trailing slash.
     * IServ and Moodle send the browser back to it after the login.
     */
    String backendUrl();

    Optional<Provider> iserv();

    Optional<Provider> moodle();

    interface Provider {
        /** e.g. https://mein-iserv.de or https://moodle.example.com, without trailing slash */
        String url();

        String clientId();

        String clientSecret();
    }
}
