package com.iluwatar.service.impl;

import com.iluwatar.exception.EmailAlreadyExistsException;
import com.iluwatar.exception.InvalidEmailFormatException;
import com.iluwatar.exception.UserNotFoundException;
import com.iluwatar.exception.UsernameAlreadyExistsException;
import com.iluwatar.model.User;
import com.iluwatar.model.dto.UserDto;
import com.iluwatar.model.repository.UserRepository;
import com.iluwatar.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    @Transactional
    public User registerUser(UserDto userDto) {
        // Check if username is unique
        if (!isUsernameUnique(userDto.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }

        // Check if email is unique
        if (!isEmailUnique(userDto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // Validate email format
        if (!isValidEmail(userDto.getEmail())) {
            throw new InvalidEmailFormatException("Invalid email format");
        }

        // Create new user
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        return userRepository.save(user);
    }

    @Override
    public boolean isUsernameUnique(String username) {
        return userRepository.findByUsernameIgnoreCase(username).isEmpty();
    }

    @Override
    public boolean isEmailUnique(String email) {
        return userRepository.findByEmail(email).isEmpty();
    }

    @Override
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    public Page<User> searchUsers(String keyword, Pageable pageable) {
        return userRepository.searchByUsernameOrEmail(keyword, pageable);
    }

    @Override
    @Transactional
    public User updateUser(Long id, UserDto userDto) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new UserNotFoundException("User not found")
        );

        // Update username if provided and unique
        if (userDto.getUsername() != null && !userDto.getUsername().equals(user.getUsername())) {
            if (!isUsernameUnique(userDto.getUsername())) {
                throw new UsernameAlreadyExistsException("Username already exists");
            }
            user.setUsername(userDto.getUsername());
        }

        // Update email if provided and unique
        if (userDto.getEmail() != null && !userDto.getEmail().equals(user.getEmail())) {
            if (!isEmailUnique(userDto.getEmail())) {
                throw new EmailAlreadyExistsException("Email already exists");
            }
            if (!isValidEmail(userDto.getEmail())) {
                throw new InvalidEmailFormatException("Invalid email format");
            }
            user.setEmail(userDto.getEmail());
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found")
        );
        user.setDeleted(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateLastLoginAt(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new UserNotFoundException("User not found")
        );
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }
}