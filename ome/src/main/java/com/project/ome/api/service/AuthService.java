// src/main/java/com/project/ome/api/service/AuthService.java
package com.project.ome.api.service;

import com.project.ome.api.dto.auth.*;
import com.project.ome.shared.entity.Account;
import com.project.ome.shared.entity.User;
import com.project.ome.shared.exception.BusinessException;
import com.project.ome.shared.repository.AccountRepository;
import com.project.ome.shared.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository    userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtService        jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check duplicates
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_TAKEN",
                    "Email already registered", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("USERNAME_TAKEN",
                    "Username already taken", HttpStatus.CONFLICT);
        }

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.ROLE_TRADER)
                .build();
        userRepository.save(user);

        // Seed demo accounts (USD + BTC + ETH) for testing
        seedDemoAccounts(user);

        log.info("New user registered: {}", user.getEmail());

        String token = jwtService.generateToken(user);
        return buildAuthResponse(token, user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(
                        "INVALID_CREDENTIALS",
                        "Invalid email or password",
                        HttpStatus.UNAUTHORIZED));

        if (!user.isActive()) {
            throw new BusinessException("ACCOUNT_DISABLED",
                    "Account is disabled", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS",
                    "Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtService.generateToken(user);
        return buildAuthResponse(token, user);
    }

    private void seedDemoAccounts(User user) {
        // Give new users demo balances for testing
        createAccount(user, "USD", new BigDecimal("100000.00"));
        createAccount(user, "BTC", new BigDecimal("10.00"));
        createAccount(user, "ETH", new BigDecimal("100.00"));
    }

    private void createAccount(User user, String currency, BigDecimal balance) {
        Account account = Account.builder()
                .user(user)
                .currency(currency)
                .availableBalance(balance)
                .build();
        accountRepository.save(account);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .role(user.getRole().name())
                .build();
    }
}