package com.example.controller;

import com.example.dto.settings.SettingsRequest;
import com.example.dto.settings.SettingsResponse;
import com.example.security.AdminPasswordProvider;
import com.example.service.SettingsService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Settings of the admin panel. (This is only for the admin panel) */
@Path("/settings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminPasswordProvider.ADMIN_ROLE)
public class SettingsController {
    @Inject
    SettingsService settingsService;

    @GET
    public SettingsResponse getSettings() {
        return settingsService.getSettings();
    }

    /**
     * Changes the settings. A new number of payment days applies to orders placed from
     * now on; existing orders keep their due date.
     */
    @PUT
    public SettingsResponse updateSettings(@Valid SettingsRequest request) {
        return settingsService.updateSettings(request);
    }
}
