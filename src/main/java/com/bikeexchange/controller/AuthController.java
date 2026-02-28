package com.bikeexchange.controller;

import com.bikeexchange.dto.request.LoginRequest;
import com.bikeexchange.dto.request.RegisterRequest;
import com.bikeexchange.dto.response.JwtAuthResponse;
import com.bikeexchange.model.User;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.repository.UserWalletRepository;
import com.bikeexchange.security.JwtTokenProvider;
import com.bikeexchange.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "APIs for user login, registration and JWT token generation")
public class AuthController {

        @Autowired
        private AuthenticationManager authenticationManager;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private UserWalletRepository walletRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private JwtTokenProvider tokenProvider;

        @PostMapping("/login")
        @Operation(summary = "Authenticate User", description = "Login user using email and password to receive a JWT access token. Use the dropdown to auto-fill sample accounts.")
        public ResponseEntity<?> authenticateUser(
                        @RequestBody(description = "Choose a role to auto-fill sample credentials:", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginRequest.class), examples = {
                                        @ExampleObject(name = "Admin Account", value = "{\"email\": \"admin@bikeexchange.com\", \"password\": \"password123\"}"),
                                        @ExampleObject(name = "Seller Account", value = "{\"email\": \"seller@bikeexchange.com\", \"password\": \"password123\"}"),
                                        @ExampleObject(name = "Inspector Account", value = "{\"email\": \"inspector@bikeexchange.com\", \"password\": \"password123\"}"),
                                        @ExampleObject(name = "Buyer Account", value = "{\"email\": \"buyer@bikeexchange.com\", \"password\": \"password123\"}")
                        })) @org.springframework.web.bind.annotation.RequestBody LoginRequest loginRequest) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                loginRequest.getEmail(),
                                                loginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = tokenProvider.generateToken(authentication);

                UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", new JwtAuthResponse(jwt, principal.getId(), principal.getUsername(),
                                principal.getFullName(), principal.getPhone(), principal.getRole()));

                return ResponseEntity.ok(response);
        }

        @PostMapping("/register")
        @Transactional
        @Operation(summary = "Register New User", description = "Creates a new buyer account on the platform")
        public ResponseEntity<?> registerUser(@RequestBody RegisterRequest signUpRequest) {
                if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                        return new ResponseEntity<>(Collections.singletonMap("message", "Email is already taken!"),
                                        HttpStatus.BAD_REQUEST);
                }

                User user = new User();
                user.setEmail(signUpRequest.getEmail());
                user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
                user.setFullName(signUpRequest.getFullName());
                user.setPhone(signUpRequest.getPhone());
                user.setAddress(signUpRequest.getAddress());
                user.setRole(User.UserRole.BUYER);

                User savedUser = userRepository.save(user);

                // Create Wallet for the new user
                UserWallet wallet = new UserWallet();
                wallet.setUser(savedUser);
                wallet.setAvailablePoints(0L);
                wallet.setFrozenPoints(0L);
                walletRepository.save(wallet);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "User registered successfully with a new wallet");

                return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
}
