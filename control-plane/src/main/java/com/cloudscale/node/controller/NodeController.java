package com.cloudscale.node.controller;

import com.cloudscale.auth.security.UserPrincipal;
import com.cloudscale.common.dto.ApiResponse;
import com.cloudscale.node.dto.NodeDTO;
import com.cloudscale.node.dto.RegisterNodeRequest;
import com.cloudscale.node.dto.UpdateNodeStatusRequest;
import com.cloudscale.node.service.NodeService;
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
@RequestMapping("/api/v1/nodes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Infrastructure Nodes", description = "Registration, lifecycle management, and monitoring of compute nodes")
public class NodeController {

    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN')")
    @Operation(summary = "Register compute node", description = "Registers a new host node with total CPU cores and memory limits")
    public ResponseEntity<ApiResponse<NodeDTO>> registerNode(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterNodeRequest request
    ) {
        NodeDTO node = nodeService.registerNode(principal.getOrganisationId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(node));
    }

    @GetMapping
    @Operation(summary = "List compute nodes", description = "Retrieves all registered nodes for the active tenant organization")
    public ResponseEntity<ApiResponse<List<NodeDTO>>> listNodes(@AuthenticationPrincipal UserPrincipal principal) {
        List<NodeDTO> nodes = nodeService.listNodes(principal.getOrganisationId());
        return ResponseEntity.ok(ApiResponse.ok(nodes));
    }

    @GetMapping("/{nodeId}")
    @Operation(summary = "Get compute node details", description = "Retrieves allocated and available resource meters for a specific node")
    public ResponseEntity<ApiResponse<NodeDTO>> getNode(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID nodeId
    ) {
        NodeDTO node = nodeService.getNode(principal.getOrganisationId(), nodeId);
        return ResponseEntity.ok(ApiResponse.ok(node));
    }

    @PatchMapping("/{nodeId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN', 'INFRASTRUCTURE_OPERATOR')")
    @Operation(summary = "Update node lifecycle status", description = "Transitions node state (e.g., ACTIVE, DRAINING, MAINTENANCE)")
    public ResponseEntity<ApiResponse<NodeDTO>> updateNodeStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID nodeId,
            @Valid @RequestBody UpdateNodeStatusRequest request
    ) {
        NodeDTO updated = nodeService.updateNodeStatus(principal.getOrganisationId(), nodeId, request.status());
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @PostMapping("/{nodeId}/heartbeat")
    @Operation(summary = "Record node heartbeat", description = "Agent probe endpoint renewing liveness timestamp")
    public ResponseEntity<ApiResponse<Void>> recordHeartbeat(@PathVariable UUID nodeId) {
        nodeService.recordHeartbeat(nodeId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/{nodeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORGANISATION_ADMIN')")
    @Operation(summary = "Decommission node", description = "Safely removes an empty node from the infrastructure registry")
    public ResponseEntity<ApiResponse<Void>> deleteNode(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID nodeId
    ) {
        nodeService.deleteNode(principal.getOrganisationId(), nodeId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
