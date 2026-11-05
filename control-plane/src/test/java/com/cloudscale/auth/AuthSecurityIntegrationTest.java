package com.cloudscale.auth;

import com.cloudscale.auth.dto.*;
import com.cloudscale.auth.model.Role;
import com.cloudscale.auth.repository.UserRepository;
import com.cloudscale.auth.service.AuthService;
import com.cloudscale.common.exception.ForbiddenException;
import com.cloudscale.common.exception.UnauthorizedException;
import com.cloudscale.tenancy.dto.CreateMemberRequest;
import com.cloudscale.tenancy.dto.OrganisationDTO;
import com.cloudscale.tenancy.service.OrganisationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthSecurityIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private UserRepository userRepository;

    private String testEmail;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testEmail = "admin-" + UUID.randomUUID() + "@cloudscale.local";
        testPassword = "SuperSecurePassword123!";
    }

    @Test
    @DisplayName("Registration successfully creates organisation and admin user with tokens")
    void testRegisterSuccess() {
        RegisterRequest req = new RegisterRequest("Acme Cloud Corp", "Alice Engineer", testEmail, testPassword);
        AuthResponse response = authService.register(req);

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals(Role.ORGANISATION_ADMIN, response.user().role());
        assertEquals("Acme Cloud Corp", response.organisation().name());
    }

    @Test
    @DisplayName("Login verifies password and returns tokens; failed attempts lead to lockout")
    void testLoginAndLockout() {
        RegisterRequest regReq = new RegisterRequest("Lockout Test Org", "Bob Lock", testEmail, testPassword);
        authService.register(regReq);

        // 1. Successful Login
        AuthResponse loginRes = authService.login(new LoginRequest(testEmail, testPassword), "127.0.0.1", "TestAgent");
        assertNotNull(loginRes.accessToken());

        // 2. Failed logins triggering lockout after 5 attempts
        LoginRequest badReq = new LoginRequest(testEmail, "WrongPassword!");
        for (int i = 1; i <= 4; i++) {
            assertThrows(UnauthorizedException.class, () -> authService.login(badReq, "127.0.0.1", "TestAgent"));
        }

        // 5th failed attempt triggers lockout
        assertThrows(UnauthorizedException.class, () -> authService.login(badReq, "127.0.0.1", "TestAgent"));

        // Subsequent attempt with CORRECT password is also locked
        UnauthorizedException lockedEx = assertThrows(UnauthorizedException.class, () -> 
                authService.login(new LoginRequest(testEmail, testPassword), "127.0.0.1", "TestAgent")
        );
        assertEquals("ACCOUNT_LOCKED", lockedEx.getCode());
    }

    @Test
    @DisplayName("Refresh token rotation issues new tokens and rejects reused old tokens")
    void testRefreshTokenRotationAndReplayAttack() {
        RegisterRequest regReq = new RegisterRequest("Rotation Corp", "Carol Rotator", testEmail, testPassword);
        AuthResponse initial = authService.register(regReq);

        String originalRefreshToken = initial.refreshToken();

        // 1. First rotation should succeed
        AuthResponse rotated = authService.refreshToken(new RefreshTokenRequest(originalRefreshToken), "127.0.0.1", "TestAgent");
        assertNotNull(rotated.accessToken());
        assertNotNull(rotated.refreshToken());
        assertNotEquals(originalRefreshToken, rotated.refreshToken());

        // 2. Replaying the OLD refresh token must trigger reuse attack detection
        UnauthorizedException reuseEx = assertThrows(UnauthorizedException.class, () -> 
                authService.refreshToken(new RefreshTokenRequest(originalRefreshToken), "127.0.0.1", "TestAgent")
        );
        assertEquals("TOKEN_REUSE_DETECTED", reuseEx.getCode());
    }

    @Test
    @DisplayName("Tenant isolation rejects cross-tenant resource inspection with ForbiddenException")
    void testTenantIsolationEnforcement() {
        // Register Org A
        RegisterRequest orgAReq = new RegisterRequest("Org Alpha", "Alpha Admin", "alpha-" + UUID.randomUUID() + "@alpha.local", testPassword);
        AuthResponse orgA = authService.register(orgAReq);

        // Register Org B
        RegisterRequest orgBReq = new RegisterRequest("Org Beta", "Beta Admin", "beta-" + UUID.randomUUID() + "@beta.local", testPassword);
        AuthResponse orgB = authService.register(orgBReq);

        // In the context of Org A, attempting to access Org B's organisation should be forbidden
        com.cloudscale.tenancy.context.TenantContext.setTenantId(orgA.organisation().id());
        try {
            assertThrows(ForbiddenException.class, () -> organisationService.getOrganisation(orgB.organisation().id()));
            assertThrows(ForbiddenException.class, () -> organisationService.getOrganisationMembers(orgB.organisation().id()));
        } finally {
            com.cloudscale.tenancy.context.TenantContext.clear();
        }
    }
}
