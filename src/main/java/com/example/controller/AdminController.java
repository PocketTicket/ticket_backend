package com.example.controller;

import com.example.dto.admin.AdminAccountRequest;
import com.example.dto.admin.AdminResponse;
import com.example.dto.admin.DoorStaffPasswordRequest;
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
 * The accounts of the login form: admins and the shared door staff account. Logging in is
 * done by Quarkus itself: POST /admin/login with the form fields username and password
 * (see application.properties).
 *
 * <p>A fresh installation has the admin account "admin" / "admin". Logged in with it,
 * GET /admin/me answers setupRequired = true and the rest of the admin panel answers 403,
 * until PUT /admin/account set a personal password and an email address.
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({AdminPasswordProvider.ACCOUNT_ROLE, AdminPasswordProvider.DOOR_ROLE})
public class AdminController {
    @Inject
    SecurityIdentity identity;

    @Inject
    AdminService adminService;

    /**
     * The logged-in account. JavaScript cannot read the login cookie, so this is how the
     * website finds out who is logged in: an admin (and whether the setup is done) or the
     * door staff.
     *
     * @return the account, or 401 if not logged in
     */
    @GET
    @Path("/me")
    public AdminResponse getLoggedInAdmin() {
        return adminService.getAdmin(identity.getPrincipal().getName());
    }

    /**
     * Sets a new password and the contact email address of the logged-in admin.
     * Not available to the door staff account.
     *
     * @return the updated admin, or 400 if the current password is wrong or the input is invalid
     */
    @PUT
    @Path("/account")
    @RolesAllowed(AdminPasswordProvider.ACCOUNT_ROLE)
    public AdminResponse updateAccount(@Valid AdminAccountRequest request) {
        return adminService.updateAccount(identity.getPrincipal().getName(), request);
    }

    /**
     * Sets the password of the account all door staff share, e.g. if its login spread
     * further than intended. (This is only for the admin panel)
     *
     * @return 204
     */
    @PUT
    @Path("/door-staff-password")
    @RolesAllowed(AdminPasswordProvider.ADMIN_ROLE)
    public Response updateDoorStaffPassword(@Valid DoorStaffPasswordRequest request) {
        adminService.updateDoorStaffPassword(request);
        return Response.noContent().build();
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
