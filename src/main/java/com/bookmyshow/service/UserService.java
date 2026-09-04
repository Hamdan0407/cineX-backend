package com.bookmyshow.service;

import com.bookmyshow.dto.AuthResponse;
import com.bookmyshow.dto.LoginRequest;
import com.bookmyshow.dto.RegisterRequest;
import com.bookmyshow.entity.User;
import com.bookmyshow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.bookmyshow.exception.ValidationException;
import com.bookmyshow.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole("USER");

        User savedUser = userRepository.save(user);

        // Return a mock token for now
        return new AuthResponse("mock-token-for-" + savedUser.getEmail(),
                savedUser.getEmail(), savedUser.getName(), savedUser.getRole());
    }

    public AuthResponse loginUser(LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return new AuthResponse("mock-token-for-" + user.getEmail(),
                        user.getEmail(), user.getName(), user.getRole());
            }
        }
        
        throw new ValidationException("Invalid email or password");
    }

    public AuthResponse adminLoginUser(LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                    throw new SecurityException("Access Denied: This account does not have Admin privileges.");
                }
                return new AuthResponse("admin-token-for-" + user.getEmail(),
                        user.getEmail(), user.getName(), user.getRole());
            }
        }
        
        throw new UnauthorizedException("Invalid admin credentials");
    }

    public org.springframework.data.domain.Page<com.bookmyshow.dto.UserResponse> getUsersPaginated(int page, int size) {
        return userRepository.findAll(org.springframework.data.domain.PageRequest.of(page, size))
                .map(com.bookmyshow.dto.mapper.UserMapper::toResponse);
    }
}
