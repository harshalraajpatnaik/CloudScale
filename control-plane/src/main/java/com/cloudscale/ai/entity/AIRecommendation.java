package com.cloudscale.ai.entity;

import com.cloudscale.tenancy.entity.Organisation;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendations")
public class AIRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @Column(nullable = false, length = 50)
    private String category; // RIGHTSIZING, NODE_REBALANCING, SCALING_TUNING, COST_OPTIMIZATION

    @Column(columnDefinition = "TEXT", nullable = false)
    private String explanation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supporting_metrics", columnDefinition = "jsonb")
    private String supportingMetrics = "{}";

    @Column(name = "confidence_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal confidenceScore = new BigDecimal("0.900");

    @Column(name = "expected_impact", nullable = false, length = 50)
    private String expectedImpact;

    @Column(name = "suggested_action", nullable = false, length = 50)
    private String suggestedAction; // SCALE_WORKLOAD, MIGRATE_WORKLOAD, DRAIN_NODE, NOOP

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_payload", columnDefinition = "jsonb")
    private String actionPayload = "{}";

    @Column(name = "approval_status", nullable = false, length = 30)
    private String approvalStatus = "PENDING"; // PENDING, APPROVED, REJECTED, APPLIED

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AIRecommendation() {}

    public AIRecommendation(Organisation organisation, String category, String explanation,
                            String supportingMetrics, BigDecimal confidenceScore,
                            String expectedImpact, String suggestedAction, String actionPayload) {
        this.organisation = organisation;
        this.category = category;
        this.explanation = explanation;
        this.supportingMetrics = supportingMetrics != null ? supportingMetrics : "{}";
        this.confidenceScore = confidenceScore != null ? confidenceScore : new BigDecimal("0.850");
        this.expectedImpact = expectedImpact;
        this.suggestedAction = suggestedAction;
        this.actionPayload = actionPayload != null ? actionPayload : "{}";
        this.approvalStatus = "PENDING";
        this.evaluatedAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Organisation getOrganisation() { return organisation; }
    public void setOrganisation(Organisation organisation) { this.organisation = organisation; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getSupportingMetrics() { return supportingMetrics; }
    public void setSupportingMetrics(String supportingMetrics) { this.supportingMetrics = supportingMetrics; }

    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getExpectedImpact() { return expectedImpact; }
    public void setExpectedImpact(String expectedImpact) { this.expectedImpact = expectedImpact; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    public String getActionPayload() { return actionPayload; }
    public void setActionPayload(String actionPayload) { this.actionPayload = actionPayload; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public UUID getApprovedByUserId() { return approvedByUserId; }
    public void setApprovedByUserId(UUID approvedByUserId) { this.approvedByUserId = approvedByUserId; }

    public Instant getEvaluatedAt() { return evaluatedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
