package com.fulfillx.auth.application;

import com.fulfillx.auth.api.CreateUserRequest;
import com.fulfillx.auth.api.LoginRequest;
import com.fulfillx.auth.api.RegisterRequest;
import com.fulfillx.auth.api.TokenResponse;
import com.fulfillx.auth.api.UserResponse;
import com.fulfillx.auth.domain.RefreshToken;
import com.fulfillx.auth.domain.UserAccount;
import com.fulfillx.auth.domain.UserStatus;
import com.fulfillx.auth.infrastructure.persistence.RefreshTokenRepository;
import com.fulfillx.auth.infrastructure.persistence.UserAccountRepository;
import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import com.fulfillx.common.security.Roles;
import com.fulfillx.security.JwtProperties;
import com.fulfillx.security.JwtService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final Optional<StringRedisTemplate> redis;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UserAccountRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            Optional<StringRedisTemplate> redis
    ) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.redis = redis;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        return createUser(request.email(), request.password(), request.fullName(), Set.of(Roles.CUSTOMER));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        return createUser(request.email(), request.password(), request.fullName(), request.roles());
    }

    private UserResponse createUser(String email, String password, String fullName, Set<String> roles) {
        if (users.existsByEmailIgnoreCase(email)) {
            throw new FulfillxException(ErrorCode.EMAIL_TAKEN, "Email already registered");
        }
        UserAccount user = UserAccount.create(
                email,
                passwordEncoder.encode(password),
                fullName,
                roles
        );
        users.save(user);
        return toResponse(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new FulfillxException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new FulfillxException(ErrorCode.ACCOUNT_DISABLED, "Account is disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new FulfillxException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }
        return issueTokens(user, UUID.randomUUID());
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        String hash = hash(refreshTokenValue);
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new FulfillxException(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid refresh token"));
        if (stored.isRevoked()) {
            refreshTokens.revokeFamily(stored.getFamilyId());
            throw new FulfillxException(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token reuse detected");
        }
        if (stored.isExpired()) {
            stored.revoke();
            throw new FulfillxException(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token expired");
        }
        UserAccount user = users.findById(stored.getUserId())
                .orElseThrow(() -> new FulfillxException(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid refresh token"));
        TokenResponse issued = issueTokens(user, stored.getFamilyId());
        stored.replaceWith(null);
        return issued;
    }

    @Transactional
    public void logout(String accessToken, String refreshTokenValue) {
        refreshTokens.findByTokenHash(hash(refreshTokenValue)).ifPresent(RefreshToken::revoke);
        denylistAccessToken(accessToken);
    }

    public UserResponse me(UUID userId) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new FulfillxException(ErrorCode.NOT_FOUND, "User not found"));
        return toResponse(user);
    }

    private TokenResponse issueTokens(UserAccount user, UUID familyId) {
        String access = jwtService.createAccessToken(user.getId(), user.getEmail(), user.getRoles());
        String refresh = newRefreshTokenValue();
        RefreshToken entity = RefreshToken.issue(
                user.getId(),
                hash(refresh),
                familyId,
                Instant.now().plusSeconds(jwtProperties.getRefreshTokenTtlSeconds())
        );
        refreshTokens.save(entity);
        return new TokenResponse(access, refresh, "Bearer", jwtService.accessTokenTtlSeconds());
    }

    private void denylistAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        redis.ifPresent(template -> {
            try {
                var claims = jwtService.parse(accessToken.replaceFirst("(?i)Bearer ", ""));
                long ttl = Math.max(1, claims.getExpiration().toInstant().getEpochSecond() - Instant.now().getEpochSecond());
                template.opsForValue().set("auth:denylist:" + claims.getId(), "1", Duration.ofSeconds(ttl));
            } catch (Exception ignored) {
                // Token already invalid.
            }
        });
    }

    private String newRefreshTokenValue() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private UserResponse toResponse(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles(),
                user.getStatus().name()
        );
    }
}
