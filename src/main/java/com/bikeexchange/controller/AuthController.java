package com.bikeexchange.controller;

import com.bikeexchange.dto.request.LoginRequest;
import com.bikeexchange.dto.request.RegisterRequest;
import com.bikeexchange.dto.request.ResetPasswordRequest;
import com.bikeexchange.dto.response.JwtAuthResponse;
import com.bikeexchange.model.User;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.model.VerificationToken;
import com.bikeexchange.repository.UserRepository;
import com.bikeexchange.repository.UserWalletRepository;
import com.bikeexchange.repository.VerificationTokenRepository;
import com.bikeexchange.security.JwtTokenProvider;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.service.EmailService;
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

        @Autowired
        private EmailService emailService;
 
        @Autowired
        private VerificationTokenRepository tokenRepository;

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
        public ResponseEntity<?> registerUser(
                        @org.springframework.web.bind.annotation.RequestBody RegisterRequest signUpRequest) {
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

                // Create and save verification token
                VerificationToken token = new VerificationToken(savedUser, VerificationToken.TokenType.VERIFICATION);
                tokenRepository.save(token);

                // Send verification email
                emailService.sendVerificationEmail(savedUser, token.getToken());

                // Create Wallet for the new user
                UserWallet wallet = new UserWallet();
                wallet.setUser(savedUser);
                wallet.setAvailablePoints(0L);
                wallet.setFrozenPoints(0L);
                walletRepository.save(wallet);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "User registered successfully. Please check your email to verify your account.");

                return new ResponseEntity<>(response, HttpStatus.CREATED);
        }

        @PostMapping("/register-no-verify")
        @Transactional
        @Operation(summary = "Register New User Without Verification", description = "Creates a new buyer account and activates it immediately without email verification")
        public ResponseEntity<?> registerUserNoVerify(
                        @org.springframework.web.bind.annotation.RequestBody RegisterRequest signUpRequest) {
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
                user.setIsVerified(true);
                user.setStatus("ACTIVE");

                User savedUser = userRepository.save(user);

                // Create Wallet for the new user
                UserWallet wallet = new UserWallet();
                wallet.setUser(savedUser);
                wallet.setAvailablePoints(0L);
                wallet.setFrozenPoints(0L);
                walletRepository.save(wallet);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "User registered and activated successfully.");

                return new ResponseEntity<>(response, HttpStatus.CREATED);
        }

        @PostMapping("/verify")
        @Transactional
        @Operation(summary = "Verify Email", description = "Verify user's email using the token sent to them")
        public ResponseEntity<?> verifyUser(@RequestParam("token") String token) {
                VerificationToken verificationToken = tokenRepository
                                .findByTokenAndType(token, VerificationToken.TokenType.VERIFICATION)
                                .orElse(null);

                if (verificationToken == null) {
                        return new ResponseEntity<>(Collections.singletonMap("message", "Invalid token!"),
                                        HttpStatus.BAD_REQUEST);
                }

                if (verificationToken.isExpired()) {
                        return new ResponseEntity<>(Collections.singletonMap("message", "Token expired!"),
                                        HttpStatus.BAD_REQUEST);
                }

                User user = verificationToken.getUser();
                user.setIsVerified(true);
                user.setStatus("ACTIVE");
                userRepository.save(user);

                tokenRepository.deleteByUserAndType(user, VerificationToken.TokenType.VERIFICATION);

                return ResponseEntity.ok(Collections.singletonMap("message", "Account verified successfully!"));
        }

        @PostMapping("/forgot-password")
        @Transactional
        @Operation(summary = "Forgot Password", description = "Send a password reset link to the user's email")
        public ResponseEntity<?> forgotPassword(@RequestParam String email) {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user == null) {
                        return new ResponseEntity<>(Collections.singletonMap("message", "User not found with this email!"),
                                        HttpStatus.NOT_FOUND);
                }

                // Delete any existing reset tokens
                tokenRepository.deleteByUserAndType(user, VerificationToken.TokenType.PASSWORD_RESET);

                VerificationToken token = new VerificationToken(user, VerificationToken.TokenType.PASSWORD_RESET);
                tokenRepository.save(token);

                emailService.sendResetPasswordEmail(user, token.getToken());

                return ResponseEntity.ok(Collections.singletonMap("message", "Password reset link has been sent to your email."));
        }

        @PostMapping("/reset-password")
        @Transactional
        @Operation(summary = "Reset Password", description = "Reset user's password using the token and new password")
        public ResponseEntity<?> resetPassword(@org.springframework.web.bind.annotation.RequestBody ResetPasswordRequest request) {
                if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                        return new ResponseEntity<>(Collections.singletonMap("message", "Passwords do not match!"),
                                        HttpStatus.BAD_REQUEST);
                }

                VerificationToken resetToken = tokenRepository
                                .findByTokenAndType(request.getToken(), VerificationToken.TokenType.PASSWORD_RESET)
                                .orElse(null);

                if (resetToken == null || resetToken.isExpired()) {
                        return new ResponseEntity<>(Collections.singletonMap("message", "Invalid or expired token!"),
                                        HttpStatus.BAD_REQUEST);
                }

                User user = resetToken.getUser();
                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                userRepository.save(user);

                tokenRepository.deleteByUserAndType(user, VerificationToken.TokenType.PASSWORD_RESET);

                return ResponseEntity.ok(Collections.singletonMap("message", "Password has been successfully reset."));
        }

        @PostMapping("/change-status/{id}")
        @Operation(summary = "Change User Status", description = "Allow admin to change user status (e.g., ACTIVE, BANNED, UNVERIFIED)")
        public ResponseEntity<?> changeStatus(@PathVariable Long id, @RequestParam String status) {
                User user = userRepository.findById(id).orElse(null);
                if (user == null) {
                        return new ResponseEntity<>(Collections.singletonMap("message", "User not found!"),
                                        HttpStatus.NOT_FOUND);
                }
                user.setStatus(status);
                userRepository.save(user);
                return ResponseEntity.ok(Collections.singletonMap("message", "User status updated to " + status));
        }
}
