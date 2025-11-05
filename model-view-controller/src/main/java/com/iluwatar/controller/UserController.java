package com.iluwatar.controller;

import com.iluwatar.model.User;
import com.iluwatar.model.dto.UserDto;
import com.iluwatar.service.UserService;
import com.iluwatar.view.UserView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserView userView;

    @Autowired
    public UserController(UserService userService, UserView userView) {
        this.userService = userService;
        this.userView = userView;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDto userDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> {
                errors.put(error.getField(), error.getDefaultMessage());
            });
            return ResponseEntity.badRequest().body(errors);
        }

        User user = userService.registerUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> userOptional = userService.findById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            UserDto userDto = convertToDto(user);
            // Remove password field from response
            userDto.setPassword(null);
            return ResponseEntity.ok(userDto);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @GetMapping
    public ResponseEntity<?> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage = userService.searchUsers(keyword != null ? keyword : "", pageable);
        Page<UserDto> userDtoPage = usersPage.map(this::convertToDto);
        // Remove password field from all responses
        userDtoPage.forEach(userDto -> userDto.setPassword(null));
        return ResponseEntity.ok(userDtoPage);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> {
                errors.put(error.getField(), error.getDefaultMessage());
            });
            return ResponseEntity.badRequest().body(errors);
        }

        User user = userService.updateUser(id, userDto);
        UserDto updatedUserDto = convertToDto(user);
        // Remove password field from response
        updatedUserDto.setPassword(null);
        return ResponseEntity.ok(updatedUserDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private UserDto convertToDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setPassword(user.getPassword());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setLastLoginAt(user.getLastLoginAt());
        userDto.setDeleted(user.isDeleted());
        return userDto;
    }
}