package com.example.mapper;

import com.example.dto.user.UserResponse;
import com.example.models.user.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.firstName(), user.lastName(), user.email(), user.ssoProvider());
    }
}
