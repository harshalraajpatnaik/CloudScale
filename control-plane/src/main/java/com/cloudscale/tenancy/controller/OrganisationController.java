package com.cloudscale.tenancy.controller;

import com.cloudscale.auth.dto.UserDTO;
import com.cloudscale.auth.security.UserPrincipal;
import com.cloudscale.common.dto.ApiResponse;
import com.cloudscale.tenancy.dto.CreateMemberRequest;
import com.cloudscale.tenancy.dto.OrganisationDTO;
import com.cloudscale.tenancy.service.OrganisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organisations")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Organisation & Tenancy", description = "Tenant boundary management, organization quotas, and member administration")
public class OrganisationController {

    private final OrganisationService organisationService;

    public OrganisationController(OrganisationService organisationService) {
        this.organisationService = organisationService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current organization", description = "Retrieves active tenant organization metadata and resource quotas")
    public ResponseEntity<ApiResponse<OrganisationDTO>> getCurrentOrganisation(@AuthenticationPrincipal UserPrincipal principal) {
        OrganisationDTO org = organisationService.getOrganisation(principal.getOrganisationId());
        return ResponseEntity.ok(ApiResponse.ok(org));
    }

    @GetMapping("/me/members")
    @Operation(summary = "List organization members", description = "Lists all registered users within the tenant organization")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getOrganisationMembers(@AuthenticationPrincipal UserPrincipal principal) {
        List<UserDTO> members = organisationService.getOrganisationMembers(principal.getOrganisationId());
        return ResponseEntity.ok(ApiResponse.ok(members));
    }

    @PostMapping("/me/members")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN')")
    @Operation(summary = "Add member to organization", description = "Provisions a new user with specified role within the tenant organization")
    public ResponseEntity<ApiResponse<UserDTO>> addMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMemberRequest request
    ) {
        UserDTO member = organisationService.addMember(principal.getOrganisationId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(member));
    }

    @GetMapping("/{orgId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @organisationService.verifyTenantAccess(#orgId)")
    @Operation(summary = "Inspect specified organization", description = "Returns organization data protected by tenant boundary validation")
    public ResponseEntity<ApiResponse<OrganisationDTO>> getOrganisationById(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrganisationDTO org = organisationService.getOrganisation(orgId);
        return ResponseEntity.ok(ApiResponse.ok(org));
    }
}
