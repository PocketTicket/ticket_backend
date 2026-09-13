package com.example.security;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Accepts the login cookie on every request after the admin logged in. The cookie is
 * encrypted with SESSION_ENCRYPTION_KEY and only issued after a correct password, so
 * the username inside it can be trusted.
 */
@ApplicationScoped
public class AdminSessionProvider implements IdentityProvider<TrustedAuthenticationRequest> {

    @Override
    public Class<TrustedAuthenticationRequest> getRequestType() {
        return TrustedAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest request,
                                              AuthenticationRequestContext context) {
        return Uni.createFrom().item(AdminPasswordProvider.adminIdentity(request.getPrincipal()));
    }
}
