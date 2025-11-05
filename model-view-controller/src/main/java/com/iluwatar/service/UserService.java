package com.iluwatar.service;

import com.iluwatar.model.User;
import com.iluwatar.model.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {

    User registerUser(UserDto userDto);

    boolean isUsernameUnique(String username);

    boolean isEmailUnique(String email);

    boolean isValidEmail(String email);

    Optional<User> findById(Long id);

    Page<User> searchUsers(String keyword, Pageable pageable);

    User updateUser(Long id, UserDto userDto);

    void deleteUser(Long id);

    void updateLastLoginAt(Long id);
}