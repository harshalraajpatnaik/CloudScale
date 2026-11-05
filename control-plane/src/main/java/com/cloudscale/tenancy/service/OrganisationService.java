package com.cloudscale.tenancy.service;

import com.cloudscale.auth.dto.UserDTO;
import com.cloudscale.auth.entity.User;
import com.cloudscale.auth.repository.UserRepository;
import com.cloudscale.common.exception.ConflictException;
import com.cloudscale.common.exception.ForbiddenException;
import com.cloudscale.common.exception.ResourceNotFoundException;
import com.cloudscale.tenancy.context.TenantContext;
import com.cloudscale.tenancy.dto.CreateMemberRequest;
import com.cloudscale.tenancy.dto.OrganisationDTO;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrganisationService {

    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public OrganisationService(
            OrganisationRepository organisationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.organisationRepository = organisationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public OrganisationDTO getOrganisation(UUID targetOrgId) {
        verifyTenantAccess(targetOrgId);
        Organisation org = organisationRepository.findById(targetOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("ORG_NOT_FOUND", "Organisation not found"));
        return OrganisationDTO.fromEntity(org);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getOrganisationMembers(UUID targetOrgId) {
        verifyTenantAccess(targetOrgId);
        return userRepository.findByOrganisationId(targetOrgId).stream()
                .map(UserDTO::fromEntity)
                .toList();
    }

    @Transactional
    public UserDTO addMember(UUID targetOrgId, CreateMemberRequest request) {
        verifyTenantAccess(targetOrgId);
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "A user with this email already exists");
        }

        Organisation org = organisationRepository.findById(targetOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("ORG_NOT_FOUND", "Organisation not found"));

        User newUser = new User(
                org,
                email,
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.role()
        );
        newUser = userRepository.save(newUser);

        return UserDTO.fromEntity(newUser);
    }

    public void verifyTenantAccess(UUID resourceOrgId) {
        UUID currentTenant = TenantContext.getTenantId();
        if (currentTenant != null && !currentTenant.equals(resourceOrgId)) {
            throw new ForbiddenException("CROSS_TENANT_ACCESS_DENIED", "Access to other tenant infrastructure is strictly prohibited");
        }
    }
}
