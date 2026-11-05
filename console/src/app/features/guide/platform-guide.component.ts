import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

export interface GuideSection {
  id: string;
  title: string;
  badge: string;
  route: string;
  icon: string;
  summary: string;
  whyUse: string;
  whatItDoes: string[];
  buttonsAndControls: { name: string; action: string; tooltip: string }[];
  scenario: string;
  bestPractices: string[];
}

@Component({
  selector: 'app-platform-guide',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './platform-guide.component.html',
  styleUrls: ['./platform-guide.component.scss']
})
export class PlatformGuideComponent {
  searchQuery = signal<string>('');
  selectedCategory = signal<string>('all');

  categories = [
    { id: 'all', label: 'All Modules' },
    { id: 'infrastructure', label: 'Infrastructure & Compute' },
    { id: 'orchestration', label: 'Workloads & Algorithms' },
    { id: 'ai', label: 'AI & Intelligence' },
    { id: 'operations', label: 'FinOps, Governance & SRE' }
  ];

  sections: (GuideSection & { category: string })[] = [
    {
      id: 'nodes',
      category: 'infrastructure',
      title: 'Compute Nodes',
      badge: 'Core Fleet',
      route: '/nodes',
      icon: '🖥️',
      summary: 'Centralized registry and hardware telemetry monitoring for physical servers, virtual machines, and cloud compute hosts.',
      whyUse: 'Enables cluster operators to track available CPU cores, memory limits, and host liveness across regions and availability zones. Prevents scheduling on saturated or failing hosts.',
      whatItDoes: [
        'Live tracking of allocated vs available CPU cores and RAM across the cluster.',
        'Continuous heartbeat liveness checks (marks nodes UNREACHABLE if no ping is received within 90s).',
        'Multi-stage maintenance workflows that gracefully isolate hosts from new workload assignments.',
        'Real-time visualization of host saturation percentages.'
      ],
      buttonsAndControls: [
        { name: '+ Register Host', action: 'Opens modal to enroll a new bare-metal, VM, or cloud instance with designated CPU/RAM limits.', tooltip: 'Enrolls host into cluster scheduler pool.' },
        { name: 'Maintenance', action: 'Toggles host into MAINTENANCE mode, preventing the scheduler from assigning new pods while keeping existing workloads intact.', tooltip: 'Use before performing OS patches or hardware upgrades.' },
        { name: 'Drain', action: 'Transitions host to DRAINING mode, stopping new workload assignments and marking the host for migration.', tooltip: 'Graceful host evacuation.' },
        { name: 'Activate', action: 'Re-enables a maintenance or drained node, returning it to ACTIVE state ready for workload scheduling.', tooltip: 'Restores host to active duty.' },
        { name: 'Decommission', action: 'Permanently removes an empty compute host from the infrastructure registry.', tooltip: 'Purges decommissioned server.' }
      ],
      scenario: 'You need to perform kernel upgrades on a physical hypervisor. You click "Maintenance", verify workloads finish or migrate, apply patches, reboot, and click "Activate" to return the host to production duty.',
      bestPractices: [
        'Always maintain at least 15% CPU headroom on critical nodes for traffic spikes.',
        'Drain nodes gracefully before terminating cloud instances to avoid HTTP 502 connection drops.'
      ]
    },
    {
      id: 'workloads',
      category: 'orchestration',
      title: 'Workloads Orchestration',
      badge: 'Deterministic Placement',
      route: '/workloads',
      icon: '⚡',
      summary: 'Stateful and stateless container lifecycle management powered by deterministic scheduling algorithms and distributed Redis locking.',
      whyUse: 'Guarantees reliable, conflict-free workload placement without race conditions. Ensures high-priority microservices always secure compute headroom while low-priority batch jobs yield resources.',
      whatItDoes: [
        'Enforces multi-algorithm placement strategies tailored to workload characteristics.',
        'Pessimistic concurrency locking with Redis prevents split-brain double-allocations.',
        'Strict state machine transitions: PENDING → RUNNING → STOPPING → STOPPED / SUSPENDED.',
        'Monitors per-workload CPU and memory allocations with organizational quota validation.'
      ],
      buttonsAndControls: [
        { name: '+ Deploy Workload', action: 'Opens modal dialog to specify service name, type (Service, Batch Job, Database, Ingress, Worker), priority class, and CPU/RAM requests.', tooltip: 'Submits new workload in PENDING state.' },
        { name: 'Algorithm Selector', action: 'Dropdown to pick between RESOURCE_AWARE, BEST_FIT, FIRST_FIT, and PRIORITY.', tooltip: 'Switches placement logic.' },
        { name: 'Schedule', action: 'Executes cluster evaluation algorithm and binds workload to the optimal compute host with distributed locking.', tooltip: 'Runs deterministic scheduling.' },
        { name: 'Stop Workload', action: 'Gracefully halts workload execution, releases allocated host CPU/RAM back to the node pool, and marks state STOPPED.', tooltip: 'Two-phase resource release.' },
        { name: 'Restart', action: 'Releases current node allocation and immediately re-evaluates the cluster to bind onto a fresh optimal host.', tooltip: 'Reschedules workload.' },
        { name: 'Suspend', action: 'Temporarily freezes workload execution while preserving registration.', tooltip: 'Pauses workload.' }
      ],
      scenario: 'High-volume payment microservice needs deployment during peak hours. You choose "RESOURCE_AWARE" with "CRITICAL" priority. The scheduler places it on the host with the lowest combined CPU/RAM pressure.',
      bestPractices: [
        'Use BEST_FIT for non-critical services to maximize bin-packing density and cut cloud host count.',
        'Use RESOURCE_AWARE for latency-sensitive APIs to avoid resource hot-spotting.'
      ]
    },
    {
      id: 'metrics',
      category: 'infrastructure',
      title: 'Metrics & Telemetry',
      badge: 'Real-Time Streaming',
      route: '/metrics',
      icon: '📊',
      summary: 'Sub-second real-time telemetry streaming from distributed Python node agents into the time-series cluster engine.',
      whyUse: 'Gives DevOps engineers immediate visibility into CPU utilization, RAM consumption, disk I/O throughput, and network transfer rates across all cluster nodes.',
      whatItDoes: [
        'Renders live SVG performance curves with dynamic min/max scaling and saturation threshold lines.',
        'Integrates with standalone Python node agents reporting CPU, memory, disk, and network stats.',
        'Aggregates cluster-wide saturation averages to detect fleet-level bottlenecks.'
      ],
      buttonsAndControls: [
        { name: 'Node Selector Dropdown', action: 'Filters telemetry views to inspect a specific compute host.', tooltip: 'Switches telemetry source.' },
        { name: 'Time Window Pills (15m, 30m, 1h, 24h)', action: 'Adjusts the historical query window to analyze transient spikes vs long-term trends.', tooltip: 'Changes zoom window.' },
        { name: 'Refresh Interval Dropdown', action: 'Configures auto-polling interval (5s, 10s, 30s, or Manual).', tooltip: 'Controls live sampling rate.' },
        { name: 'Refresh Now', action: 'Triggers immediate telemetry fetch from the control plane API.', tooltip: 'Pulls latest metrics.' }
      ],
      scenario: 'An alert warns of slow API response times. You open Metrics, select the affected node, switch to the 15-minute window, and identify that memory utilization climbed to 98% during a batch import.',
      bestPractices: [
        'Configure alerts at 75% CPU and 80% RAM to catch saturation before user traffic is impacted.',
        'Inspect Network Rx/Tx bytes alongside CPU to distinguish network-bound from compute-bound issues.'
      ]
    },
    {
      id: 'autoscaling',
      category: 'orchestration',
      title: 'Autoscaling Engine',
      badge: 'Dynamic Elasticity',
      route: '/autoscaling',
      icon: '🔄',
      summary: 'Horizontal workload autoscaling rules engine with hysteresis damping, cooldown periods, and manual evaluation simulation.',
      whyUse: 'Protects critical microservices from crashing under traffic surges while automatically downsizing during idle hours to eliminate wasted infrastructure spend.',
      whatItDoes: [
        'Evaluates CPU and memory thresholds against moving averages to prevent flapping.',
        'Enforces cooldown periods (e.g. 300s) to allow newly spawned replicas to warm up before further scaling.',
        'Maintains minimum and maximum replica boundaries to protect organizational budgets.'
      ],
      buttonsAndControls: [
        { name: '+ Create Policy', action: 'Opens policy creator to configure target service, CPU/RAM upper/lower limits, min/max replicas, and step size.', tooltip: 'Configures autoscaling rule.' },
        { name: 'Run Simulator', action: 'Tests how the policy reacts to a simulated CPU/RAM saturation level without altering production pods.', tooltip: 'Dry-run evaluation.' },
        { name: 'Enable / Disable', action: 'Toggles policy evaluation on or off.', tooltip: 'Pause policy without deleting.' },
        { name: 'Delete Policy', action: 'Removes the autoscaling policy from the target workload.', tooltip: 'Deletes scaling rule.' }
      ],
      scenario: 'Ahead of Black Friday, you configure an autoscaling policy on your catalog service with min 3 replicas, max 15 replicas, scale-up at 75% CPU (+2 pods), and scale-down at 25% CPU (-1 pod). You verify the behavior using the simulator.',
      bestPractices: [
        'Always set cooldown period to at least 180s to prevent rapid oscillation (thrashing).',
        'Pair horizontal autoscaling with CloudScale AI forecast models to anticipate traffic ramps.'
      ]
    },
    {
      id: 'ai-assistant',
      category: 'ai',
      title: 'AI Intelligence Assistant',
      badge: 'Powered by Gemini',
      route: '/ai-assistant',
      icon: '✨',
      summary: 'Conversational cluster intelligence powered by Google Gemini, automated rightsizing recommendations, and telemetry root-cause diagnostics.',
      whyUse: 'Replaces tedious manual log grepping with instant natural-language cluster queries and AI-grounded architectural recommendations tailored to live cluster state.',
      whatItDoes: [
        'Answers natural-language questions with real-time awareness of active nodes and workloads.',
        'Generates automated rightsizing and rebalancing recommendations based on telemetry analysis.',
        'Diagnoses incident spikes and outputs root causes, affected components, and step-by-step remediation commands.'
      ],
      buttonsAndControls: [
        { name: 'Generate Recommendations', action: 'Scans the cluster and invokes AI to identify over-provisioned workloads and imbalanced nodes.', tooltip: 'Runs cluster optimization audit.' },
        { name: 'Approve Recommendation', action: 'Marks an AI recommendation as approved by operator for audit compliance.', tooltip: 'Accepts optimization.' },
        { name: 'Reject Recommendation', action: 'Dismisses suggestion with operator reasoning.', tooltip: 'Rejects optimization.' },
        { name: 'Send Prompt (Chat)', action: 'Submits natural language query to Gemini assistant with injected live cluster context.', tooltip: 'Query cluster assistant.' },
        { name: 'Suggested Prompt Chips', action: 'Quick-fills popular operational queries like "Summarize cluster health" or "Analyze CPU spikes".', tooltip: 'One-click prompt helper.' }
      ],
      scenario: 'You notice cluster costs trending upward. You click "Generate Recommendations". The AI suggests downsizing an idle analytics worker from 8 cores to 2 cores, saving $240/month. You click "Approve".',
      bestPractices: [
        'Use the AI Assistant during active incidents to get rapid initial triage hypotheses.',
        'Audit all approved recommendations via the Audit Logs tab for change management governance.'
      ]
    },
    {
      id: 'cost',
      category: 'operations',
      title: 'FinOps & Cost Intelligence',
      badge: 'Spend Optimization',
      route: '/cost',
      icon: '💰',
      summary: 'Cloud expenditure analytics, real-time burn rate tracking, instance tier cost breakdown, and idle resource waste analysis.',
      whyUse: 'Gives engineering leads and FinOps teams direct visibility into cloud cost drivers. Prevents unexpected end-of-month cloud bill shocks.',
      whatItDoes: [
        'Calculates real-time hourly, daily, and projected monthly infrastructure expenditure.',
        'Breaks down compute spend by individual node, instance tier, and tenant workload.',
        'Detects zombie workloads running on over-provisioned nodes with <10% utilization.'
      ],
      buttonsAndControls: [
        { name: 'Summary KPIs', action: 'Displays Total Cluster Cost, Monthly Burn Rate, Active Cores Cost, and Idle Waste.', tooltip: 'High-level financial overview.' },
        { name: 'Node Cost Table', action: 'Detailed per-host breakdown of hourly rate, monthly projection, and resource efficiency.', tooltip: 'Inspects host economics.' },
        { name: 'Waste Analysis Card', action: 'Highlights potential savings by reclaiming unallocated CPU cores and unused RAM.', tooltip: 'Identifies cost reduction targets.' }
      ],
      scenario: 'FinOps requests a quarterly infrastructure efficiency report. You open Cost Intelligence to view your current $1,840/mo burn rate and identify 3 under-utilized nodes that can be merged to save $450/mo.',
      bestPractices: [
        'Review the Waste Analysis card weekly to maintain >70% resource efficiency.',
        'Tag simulated and test workloads so their financial impact is easily differentiated from production.'
      ]
    },
    {
      id: 'incidents',
      category: 'operations',
      title: 'Incident Management',
      badge: 'SRE Triage',
      route: '/incidents',
      icon: '🚨',
      summary: 'Centralized incident response board displaying saturation alerts, hardware failures, and high-severity cluster events.',
      whyUse: 'Enables Site Reliability Engineers to immediately detect, triage, and resolve infrastructure anomalies before they escalate into end-user outages.',
      whatItDoes: [
        'Aggregates alerts by severity level (CRITICAL, HIGH, WARNING, INFO).',
        'Tracks alert lifecycle states: ACTIVE → ACKNOWLEDGED → RESOLVED.',
        'Correlates alerts directly with the offending compute host and workload.',
        'Provides built-in simulation to validate alert notification pipelines.'
      ],
      buttonsAndControls: [
        { name: 'Simulate Alert', action: 'Generates a mock CRITICAL host saturation alert to verify monitoring workflows and notification handlers.', tooltip: 'Tests incident pipeline.' },
        { name: 'Resolve Alert', action: 'Marks an active alert as RESOLVED, timestamping the resolution and updating operational metrics.', tooltip: 'Closes incident.' },
        { name: 'Filter by Status', action: 'Filters board view to show All, Open, or Resolved incidents.', tooltip: 'Toggles incident filter.' },
        { name: 'Diagnose with AI', action: 'Sends the incident context to the Gemini AI service for instant automated root cause analysis.', tooltip: 'AI incident report.' }
      ],
      scenario: 'A memory leak causes a host alert. The on-call engineer opens Incidents, reviews the alert details, clicks "Diagnose with AI" for remediation advice, restarts the leaky container, and clicks "Resolve".',
      bestPractices: [
        'Review open alert metrics regularly to minimize Mean Time to Resolution (MTTR).',
        'Always verify host metrics return to safe zones before resolving critical alerts.'
      ]
    },
    {
      id: 'audit',
      category: 'operations',
      title: 'Audit Governance & Compliance',
      badge: 'Immutable Ledger',
      route: '/audit',
      icon: '📜',
      summary: 'Tamper-evident governance ledger recording every administrative mutation, scheduling event, and lifecycle transition.',
      whyUse: 'Satisfies enterprise SOC 2, HIPAA, and ISO 27001 audit requirements. Provides forensic accountability showing who changed what, when, and from what IP.',
      whatItDoes: [
        'Logs actor identity, action type (e.g. WORKLOAD_CREATED, NODE_DRAINED), resource ID, and client IP.',
        'Stores structured JSONB before-and-after state diffs for complete forensic replay.',
        'Secured with multi-tenant tenant isolation so organizations only access their own audit trail.'
      ],
      buttonsAndControls: [
        { name: 'Action Filter Dropdown', action: 'Filters audit logs by action category (Node, Workload, Alert, Auth, Policy).', tooltip: 'Filters audit event types.' },
        { name: 'Search Query Input', action: 'Free-text search across actor emails, resource IDs, and action descriptions.', tooltip: 'Searches audit events.' },
        { name: 'Export / Inspect DTO', action: 'Expands log entry to view exact previous and new state JSON payload.', tooltip: 'Inspects state snapshot.' }
      ],
      scenario: 'An auditor requires proof of all production configuration changes over the past 30 days. You filter the Audit Logs tab by date and export the tamper-evident log records.',
      bestPractices: [
        'Regularly review authentication failure audit entries for early signs of brute-force attacks.',
        'Store audit logs for at least 365 days to meet compliance standards.'
      ]
    },
    {
      id: 'health',
      category: 'infrastructure',
      title: 'System Health & Probes',
      badge: 'Platform Liveness',
      route: '/health',
      icon: '🟢',
      summary: 'Multi-tier distributed health probe monitoring the Control Plane, AI Service, Node Agent, PostgreSQL, and Redis connectivity.',
      whyUse: 'Provides transparent observability into whether all backend microservices, databases, and message brokers are operating nominally.',
      whatItDoes: [
        'Pings Spring Boot Actuator, FastAPI health endpoints, and database connection pools.',
        'Reports response latency in milliseconds to detect network degradation.',
        'Publicly accessible without authentication for external uptime monitor integration.'
      ],
      buttonsAndControls: [
        { name: 'Refresh Health Probes', action: 'Initiates immediate parallel HTTP GET probes to all registered platform services.', tooltip: 'Checks all subsystem statuses.' },
        { name: 'Service Endpoint Cards', action: 'Inspects individual service URLs, reported uptime, latency, and operational mode.', tooltip: 'Subsystem detail.' }
      ],
      scenario: 'Before starting a major workload deployment, an operator opens System Health to verify all databases and AI components show 100% green health status.',
      bestPractices: [
        'Add the `/actuator/health` and `/api/v1/health` endpoints to your external synthetics monitor (e.g. Pingdom or Datadog).',
        'Investigate any probe latency exceeding 250ms as a leading indicator of connection pool saturation.'
      ]
    }
  ];

  filteredSections = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    const cat = this.selectedCategory();

    return this.sections.filter(sec => {
      const matchesCat = cat === 'all' || sec.category === cat;
      const matchesQuery = !q || 
        sec.title.toLowerCase().includes(q) ||
        sec.summary.toLowerCase().includes(q) ||
        sec.whyUse.toLowerCase().includes(q) ||
        sec.whatItDoes.some(item => item.toLowerCase().includes(q)) ||
        sec.buttonsAndControls.some(b => b.name.toLowerCase().includes(q) || b.action.toLowerCase().includes(q));

      return matchesCat && matchesQuery;
    });
  });

  setCategory(catId: string) {
    this.selectedCategory.set(catId);
  }

  scrollToSection(id: string) {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
}
