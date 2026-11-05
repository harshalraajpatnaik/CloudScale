package com.cloudscale.auth.security;

import com.cloudscale.auth.model.Role;
import com.cloudscale.auth.service.RedisTokenRevocationService;
import com.cloudscale.tenancy.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final RedisTokenRevocationService revocationService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, RedisTokenRevocationService revocationService) {
        this.tokenProvider = tokenProvider;
        this.revocationService = revocationService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String tokenHash = tokenProvider.hashToken(jwt);

                // Check Redis revocation blacklist
                if (!revocationService.isTokenRevoked(tokenHash)) {
                    UUID userId = tokenProvider.getUserId(jwt);
                    UUID orgId = tokenProvider.getOrganisationId(jwt);
                    String email = tokenProvider.getEmail(jwt);
                    Role role = tokenProvider.getRole(jwt);

                    UserPrincipal principal = new UserPrincipal(
                            userId, orgId, email, "", "", role, true
                    );

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    TenantContext.setTenantId(orgId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
