package com.example.controller;

import com.example.dto.user.UserCreateRequest;
import com.example.dto.user.UserResponse;
import com.example.service.UserService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {
    @Inject
    UserService userService;

    @GET
    public List<UserResponse> getUsers() {
        return userService.getUsers();
    }

    @GET
    @Path("/{userId}")
    public UserResponse getUserById(@PathParam("userId") int userId) {
        return userService.getUserById(userId);
    }

    /**
     * Creates an account. Passwords are not handled here yet - LOCAL accounts are
     * created without one until the admin login is built, and IServ/Moodle users
     * will be created from their provider's subject on first sign-in.
     *
     * @return 201 with the user, 409 if the email is taken
     */
    @POST
    public Response createUser(@Valid UserCreateRequest request) {
        UserResponse created = userService.createUser(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }
}
