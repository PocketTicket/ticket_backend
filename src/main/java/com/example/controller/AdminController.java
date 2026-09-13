package com.example.controller;

import com.example.dto.admin.AdminAccountRequest;
import com.example.dto.admin.AdminResponse;
import com.example.security.AdminPasswordProvider;
import com.example.service.AdminService;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The own account of the logged-in admin. Logging in is done by Quarkus itself:
 * POST /admin/login with the form fields username and password (see application.properties).
 *
 * <p>A fresh installation has the account "admin" / "admin". Logged in with it,
 * GET /admin/me answers setupRequired = true and the rest of the admin panel answers 403,
 * until PUT /admin/account set a personal password and an email address.
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminPasswordProvider.ACCOUNT_ROLE)
public class AdminController {
    @Inject
    SecurityIdentity identity;

    @Inject
    AdminService adminService;

    /**
     * The logged-in admin. JavaScript cannot read the login cookie, so this is how the
     * admin panel finds out whether it is still logged in and whether the setup is done.
     *
     * @return the admin, or 401 if not logged in
     */
    @GET
    @Path("/me")
    public AdminResponse getLoggedInAdmin() {
        return adminService.getAdmin(identity.getPrincipal().getName());
    }

    /**
     * Sets a new password and the contact email address.
     *
     * @return the updated admin, or 400 if the current password is wrong or the input is invalid
     */
    @PUT
    @Path("/account")
    public AdminResponse updateAccount(@Valid AdminAccountRequest request) {
        return adminService.updateAccount(identity.getPrincipal().getName(), request);
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
