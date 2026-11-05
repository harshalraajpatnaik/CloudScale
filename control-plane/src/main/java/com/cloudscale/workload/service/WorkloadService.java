package com.cloudscale.workload.service;

import com.cloudscale.common.exception.ConflictException;
import com.cloudscale.common.exception.ResourceNotFoundException;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.scheduler.service.SchedulerService;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import com.cloudscale.workload.dto.CreateWorkloadRequest;
import com.cloudscale.workload.dto.WorkloadDTO;
import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.model.WorkloadState;
import com.cloudscale.workload.repository.WorkloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkloadService {

    private static final Logger log = LoggerFactory.getLogger(WorkloadService.class);

    private final WorkloadRepository workloadRepository;
    private final OrganisationRepository organisationRepository;
    private final NodeRepository nodeRepository;
    private final SchedulerService schedulerService;
    private final com.cloudscale.audit.service.AuditLogService auditLogService;

    public WorkloadService(
            WorkloadRepository workloadRepository,
            OrganisationRepository organisationRepository,
            NodeRepository nodeRepository,
            SchedulerService schedulerService,
            com.cloudscale.audit.service.AuditLogService auditLogService
    ) {
        this.workloadRepository = workloadRepository;
        this.organisationRepository = organisationRepository;
        this.nodeRepository = nodeRepository;
        this.schedulerService = schedulerService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public WorkloadDTO createWorkload(UUID orgId, CreateWorkloadRequest request) {
        Organisation org = organisationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ORG_NOT_FOUND", "Organisation not found"));

        // Validate organizational quota
        if (request.cpuCoresRequired() > org.getMaxCpuQuota()) {
            throw new ConflictException("CPU_EXCEEDS_ORG_QUOTA", 
                    "Requested CPU cores (" + request.cpuCoresRequired() + ") exceeds organization limit of " + org.getMaxCpuQuota());
        }

        Workload workload = new Workload(
                org,
                request.name(),
                request.workloadType(),
                request.priority(),
                request.cpuCoresRequired(),
                request.memoryMbRequired()
        );
        workload = workloadRepository.save(workload);
        log.info("Created workload {} ({}) for org {}", workload.getName(), workload.getId(), orgId);
        auditLogService.recordEvent(orgId, null, "WORKLOAD_CREATED", "Workload", workload.getId().toString(), null, "{}", null);

        return WorkloadDTO.fromEntity(workload);
    }

    @Transactional(readOnly = true)
    public List<WorkloadDTO> listWorkloads(UUID orgId) {
        return workloadRepository.findByOrganisationId(orgId).stream()
                .map(WorkloadDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkloadDTO getWorkload(UUID orgId, UUID workloadId) {
        Workload w = workloadRepository.findByIdAndOrganisationId(workloadId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WORKLOAD_NOT_FOUND", "Workload not found"));
        return WorkloadDTO.fromEntity(w);
    }

    @Transactional
    public WorkloadDTO stopWorkload(UUID orgId, UUID workloadId) {
        WorkloadDTO dto = schedulerService.deschedule(orgId, workloadId);
        auditLogService.recordEvent(orgId, null, "WORKLOAD_STOPPED", "Workload", workloadId.toString(), null, "{}", null);
        return dto;
    }

    @Transactional
    public WorkloadDTO restartWorkload(UUID orgId, UUID workloadId, String algorithm) {
        Workload workload = workloadRepository.findByIdAndOrganisationId(workloadId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WORKLOAD_NOT_FOUND", "Workload not found"));

        if (workload.getAssignedNode() != null || workload.getActualState() == WorkloadState.RUNNING) {
            schedulerService.deschedule(orgId, workloadId);
        } else if (workload.getActualState() != WorkloadState.STOPPED && workload.getActualState() != WorkloadState.PENDING) {
            workload.transitionTo(WorkloadState.STOPPED);
            workloadRepository.save(workload);
        }
        WorkloadDTO scheduled = schedulerService.schedule(orgId, workloadId, algorithm);
        auditLogService.recordEvent(orgId, null, "WORKLOAD_RESTARTED", "Workload", workloadId.toString(), null, "{\"algorithm\":\"" + algorithm + "\"}", null);
        return scheduled;
    }

    @Transactional
    public WorkloadDTO suspendWorkload(UUID orgId, UUID workloadId) {
        Workload workload = workloadRepository.findByIdAndOrganisationId(workloadId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WORKLOAD_NOT_FOUND", "Workload not found"));

        workload.transitionTo(WorkloadState.SUSPENDED);
        workload = workloadRepository.save(workload);
        log.info("Suspended workload {}", workload.getName());

        return WorkloadDTO.fromEntity(workload);
    }

    @Transactional
    public void deleteWorkload(UUID orgId, UUID workloadId) {
        Workload workload = workloadRepository.findByIdAndOrganisationId(workloadId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WORKLOAD_NOT_FOUND", "Workload not found"));

        if (workload.getAssignedNode() != null) {
            Node assignedNode = workload.getAssignedNode();
            assignedNode.release(workload.getCpuCoresRequired(), workload.getMemoryMbRequired());
            nodeRepository.save(assignedNode);
        }

        workloadRepository.delete(workload);
        log.info("Deleted workload: {}", workload.getName());
        auditLogService.recordEvent(orgId, null, "WORKLOAD_DELETED", "Workload", workloadId.toString(), null, "{}", null);
    }
}
