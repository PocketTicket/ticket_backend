package com.example.service;

import com.example.dto.user.UserCreateRequest;
import com.example.dto.user.UserResponse;
import com.example.exception.BusinessRuleException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.UserMapper;
import com.example.models.user.User;
import com.example.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
@Transactional
public class UserService {
    @Inject
    UserRepository userRepository;

    public List<UserResponse> getUsers() {
        return UserMapper.toResponses(userRepository.getUsers());
    }

    public UserResponse getUserById(int userId) {
        return UserMapper.toResponse(findUser(userId));
    }

    /**
     * @throws BusinessRuleException if the address is already taken. Checked up
     *                               front so the caller gets a 409 rather than a
     *                               500 out of idx_users_email.
     */
    public UserResponse createUser(UserCreateRequest request) {
        User user = UserMapper.toModel(request);

        if (userRepository.getUserByEmail(user.email()) != null) {
            throw new BusinessRuleException("A user with the email " + user.email() + " already exists");
        }

        return UserMapper.toResponse(userRepository.createUser(user));
    }

    /**
     * @throws ResourceNotFoundException if no user has that id. Used by OrderService
     *                                   so an unknown buyer fails as a 404 instead of
     *                                   a foreign key violation.
     */
    public void requireExists(int userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("No user with id " + userId);
        }
    }

    private User findUser(int userId) {
        User user = userRepository.getUserById(userId);

        if (user == null) {
            throw new ResourceNotFoundException("No user with id " + userId);
        }
        return user;
    }
}
