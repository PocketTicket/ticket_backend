package com.example.mapper;

import com.example.dto.user.UserCreateRequest;
import com.example.dto.user.UserResponse;
import com.example.models.user.AuthProvider;
import com.example.models.user.User;
import com.example.models.user.UserRole;

import java.util.List;

public final class UserMapper {

    private UserMapper() {
    }

    /** The id, the password hash and createdAt are not client input. */
    public static User toModel(UserCreateRequest request) {
        return new User(
                0,
                request.firstName().trim(),
                request.lastName().trim(),
                request.email().trim(),
                request.role() == null ? UserRole.USER : request.role(),
                request.authProvider() == null ? AuthProvider.LOCAL : request.authProvider(),
                request.externalId(),
                null,
                null
        );
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.userId(),
                user.firstName(),
                user.lastName(),
                user.email(),
                user.role(),
                user.authProvider(),
                user.createdAt()
        );
    }

    public static List<UserResponse> toResponses(List<User> users) {
        return users.stream().map(UserMapper::toResponse).toList();
    }
}
