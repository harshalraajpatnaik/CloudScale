package com.cloudscale.auth.service;

import com.cloudscale.auth.dto.*;
import com.cloudscale.auth.entity.RefreshToken;
import com.cloudscale.auth.entity.User;
import com.cloudscale.auth.model.Role;
import com.cloudscale.auth.repository.RefreshTokenRepository;
import com.cloudscale.auth.repository.UserRepository;
import com.cloudscale.auth.security.JwtTokenProvider;
import com.cloudscale.common.exception.ConflictException;
import com.cloudscale.common.exception.ResourceNotFoundException;
import com.cloudscale.common.exception.UnauthorizedException;
import com.cloudscale.tenancy.dto.OrganisationDTO;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenRevocationService redisTokenRevocationService;

    public AuthService(
            UserRepository userRepository,
            OrganisationRepository organisationRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RedisTokenRevocationService redisTokenRevocationService
    ) {
        this.userRepository = userRepository;
        this.organisationRepository = organisationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTokenRevocationService = redisTokenRevocationService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "A user with this email already exists");
        }

        String baseSlug = request.organisationName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "-");
        String slug = baseSlug;
        int suffix = 1;
        while (organisationRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix++;
        }

        Organisation org = new Organisation(request.organisationName(), slug);
        org = organisationRepository.save(org);

        User user = new User(
                org,
                email,
                passwordEncoder.encode(request.password()),
                request.fullName(),
                Role.ORGANISATION_ADMIN
        );
        user = userRepository.save(user);

        return issueTokens(user, "127.0.0.1", "Registration");
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp, String userAgent) {
        String email = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password"));

        if (!user.isActive()) {
            throw new UnauthorizedException("ACCOUNT_INACTIVE", "Your account has been deactivated");
        }

        if (user.isAccountLocked()) {
            throw new UnauthorizedException("ACCOUNT_LOCKED", "Account locked due to consecutive failed attempts. Try again later.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
                log.warn("Account {} locked for {} minutes due to {} failed attempts", email, LOCKOUT_MINUTES, MAX_FAILED_ATTEMPTS);
            }
            userRepository.save(user);
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        // Reset failed login attempts on success
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return issueTokens(user, clientIp, userAgent);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String clientIp, String userAgent) {
        String rawToken = request.refreshToken();
        String tokenHash = jwtTokenProvider.hashToken(rawToken);

        boolean isBlacklistedInRedis = redisTokenRevocationService.isTokenRevoked(tokenHash);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        // Reuse detection: If token is already blacklisted in Redis or marked revoked in DB, an attacker has replayed it!
        if (isBlacklistedInRedis || storedToken.isRevoked()) {
            log.error("REFRESH TOKEN REUSE ATTACK DETECTED for user {}! Revoking all tokens.", storedToken.getUser().getEmail());
            refreshTokenRepository.revokeAllByUserId(storedToken.getUser().getId());
            throw new UnauthorizedException("TOKEN_REUSE_DETECTED", "Security compromise detected. All sessions revoked.");
        }

        if (storedToken.isExpired()) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedException("TOKEN_EXPIRED", "Refresh token has expired");
        }

        // Single-Use Token Rotation: invalidate current token immediately
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        long ttlSeconds = ChronoUnit.SECONDS.between(Instant.now(), storedToken.getExpiresAt());
        if (ttlSeconds > 0) {
            redisTokenRevocationService.revokeToken(tokenHash, ttlSeconds);
        }

        // Issue fresh access and refresh tokens
        return issueTokens(storedToken.getUser(), clientIp, userAgent);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = jwtTokenProvider.hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                long ttlSeconds = ChronoUnit.SECONDS.between(Instant.now(), token.getExpiresAt());
                if (ttlSeconds > 0) {
                    redisTokenRevocationService.revokeToken(tokenHash, ttlSeconds);
                }
            });
        }
    }

    @Transactional(readOnly = true)
    public UserDTO getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
        return UserDTO.fromEntity(user);
    }

    private AuthResponse issueTokens(User user, String clientIp, String userAgent) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String rawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        String refreshTokenHash = jwtTokenProvider.hashToken(rawRefreshToken);

        Instant expiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());

        RefreshToken refreshToken = new RefreshToken(
                user,
                refreshTokenHash,
                expiresAt,
                clientIp,
                userAgent
        );
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.of(
                accessToken,
                rawRefreshToken,
                900000L, // 15 mins
                UserDTO.fromEntity(user),
                OrganisationDTO.fromEntity(user.getOrganisation())
        );
    }
}
