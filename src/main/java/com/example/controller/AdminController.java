package com.example.controller;

import com.example.dto.admin.AdminResponse;
import com.example.security.AdminPasswordProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Session handling for the admin panel. Logging in is done by Quarkus itself:
 * POST /admin/login with the form fields username and password (see application.properties).
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminPasswordProvider.ADMIN_ROLE)
public class AdminController {
    @Inject
    SecurityIdentity identity;

    /**
     * The logged-in admin. JavaScript cannot read the login cookie, so this is how the
     * admin panel finds out whether it is still logged in.
     *
     * @return the admin, or 401 if not logged in
     */
    @GET
    @Path("/me")
    public AdminResponse getLoggedInAdmin() {
        return new AdminResponse(identity.getPrincipal().getName());
    }

    /**
     * Removes the login cookie.
     */
    @POST
    @Path("/logout")
    public Response logout() {
        FormAuthenticationMechanism.logout(identity);
        return Response.noContent().build();
    }
}
