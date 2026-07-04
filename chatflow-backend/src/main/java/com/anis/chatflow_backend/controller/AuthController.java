package com.anis.chatflow_backend.controller;

import com.anis.chatflow_backend.dto.RegisterRequest;
import com.anis.chatflow_backend.model.User;
import com.anis.chatflow_backend.repository.UserRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.anis.chatflow_backend.security.JwtUtil;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/")
    public String home() {

        return "Auth API Working";
    }

  @PostMapping("/register")
public String registerUser(@RequestBody RegisterRequest request) {

    User user = new User();

    user.setName(request.getName());
    user.setEmail(request.getEmail());
    user.setPassword(request.getPassword());

    userRepository.save(user);

    return "User Registered Successfully";
}
@GetMapping("/users")

public List<User> getUsers() {

    return userRepository.findAll();
}

    @PostMapping("/login")
public String loginUser(@RequestBody User user) {

    List<User> users = userRepository.findByEmail(user.getEmail());

if(users.isEmpty()) {
    return "User Not Found";
}


User existingUser = users.get(0);

    if (existingUser == null) {
        return "User Not Found";
    }

    if (!existingUser.getPassword().equals(user.getPassword())) {
        return "Wrong Password";
    }

    String token = jwtUtil.generateToken(existingUser.getEmail());

    return token;
}
    }
