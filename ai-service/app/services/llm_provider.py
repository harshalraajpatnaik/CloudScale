import json
import logging
import re
from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional
from app.core.config import settings

logger = logging.getLogger(__name__)

def clean_json_response(raw_text: str) -> Dict[str, Any]:
    """Strips markdown code fences and parses JSON safely."""
    cleaned = raw_text.strip()
    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```[a-zA-Z]*\n", "", cleaned)
        cleaned = re.sub(r"\n```$", "", cleaned)
        cleaned = cleaned.strip()
    try:
        return json.loads(cleaned)
    except Exception:
        # Fallback heuristic if slightly malformed
        match = re.search(r"\{.*\}", cleaned, re.DOTALL)
        if match:
            return json.loads(match.group(0))
        raise

class LLMProvider(ABC):
    @abstractmethod
    async def diagnose_incident(self, incident_data: Dict[str, Any]) -> Dict[str, Any]:
        pass

    @abstractmethod
    async def generate_recommendations(self, cluster_context: Dict[str, Any]) -> List[Dict[str, Any]]:
        pass

    @abstractmethod
    async def chat(self, user_message: str, history: List[Dict[str, str]], cluster_context: Dict[str, Any]) -> str:
        pass

class GeminiProvider(LLMProvider):
    def __init__(self, api_key: str, model_name: str = "gemini-3.5-flash-lite"):
        self.api_key = api_key
        self.model_name = model_name
        self._client = None

        if self.api_key:
            try:
                from google import genai
                self._client = genai.Client(api_key=self.api_key)
                logger.info(f"[GeminiProvider] Initialized google-genai client with model: {self.model_name}")
            except Exception as e:
                logger.warning(f"[GeminiProvider] Error initializing google-genai client: {e}")

    async def diagnose_incident(self, incident_data: Dict[str, Any]) -> Dict[str, Any]:
        if not self._client:
            return MockLLMProvider().diagnose_incident_sync(incident_data)

        system_instruction = (
            "You are CloudScale's Senior Site Reliability & Cloud Infrastructure Diagnostic Engineer.\n"
            "Analyze the infrastructure incident telemetry, identify root causes (e.g. CPU saturation, memory exhaustion, noisy neighbor, thrashing), and propose actionable remediation.\n"
            "Output strictly valid JSON with keys:\n"
            "- 'title': concise summary\n"
            "- 'root_cause': detailed explanation\n"
            "- 'severity': 'CRITICAL', 'HIGH', 'MEDIUM', or 'LOW'\n"
            "- 'affected_components': list of host/workload names\n"
            "- 'remediation_steps': list of immediate operational steps\n"
            "- 'confidence_score': float between 0.0 and 1.0"
        )

        prompt = f"INCIDENT TELEMETRY DATA:\n{json.dumps(incident_data, indent=2)}\n\nProvide JSON diagnostic report:"

        try:
            response = self._client.models.generate_content(
                model=self.model_name,
                contents=prompt,
                config={"system_instruction": system_instruction, "response_mime_type": "application/json"}
            )
            return clean_json_response(response.text)
        except Exception as e:
            logger.warning(f"[GeminiProvider] Error during diagnose_incident: {e}. Falling back to rule-based analysis.")
            return MockLLMProvider().diagnose_incident_sync(incident_data)

    async def generate_recommendations(self, cluster_context: Dict[str, Any]) -> List[Dict[str, Any]]:
        if not self._client:
            return MockLLMProvider().generate_recommendations_sync(cluster_context)

        system_instruction = (
            "You are CloudScale's Infrastructure Intelligence & Capacity Optimization Advisor.\n"
            "Analyze current cluster capacity, node saturation, and workload allocations.\n"
            "Generate actionable optimization recommendations (e.g. rightsizing overprovisioned workloads, rebalancing hot nodes, adjusting scaling thresholds).\n"
            "Output strictly valid JSON as an array of recommendation objects. Each object must have:\n"
            "- 'category': 'RIGHTSIZING', 'NODE_REBALANCING', 'SCALING_TUNING', or 'COST_OPTIMIZATION'\n"
            "- 'title': brief headline\n"
            "- 'explanation': comprehensive rationale with metrics\n"
            "- 'confidence_score': float between 0.0 and 1.0\n"
            "- 'expected_impact': expected benefit (e.g. 'Reduces CPU overhead by 25%')\n"
            "- 'suggested_action': 'SCALE_WORKLOAD', 'MIGRATE_WORKLOAD', 'UPDATE_POLICY', or 'DRAIN_NODE'\n"
            "- 'action_payload': key-value JSON parameters for execution"
        )

        prompt = f"CLUSTER TELEMETRY CONTEXT:\n{json.dumps(cluster_context, indent=2)}\n\nProvide JSON recommendations array:"

        try:
            response = self._client.models.generate_content(
                model=self.model_name,
                contents=prompt,
                config={"system_instruction": system_instruction, "response_mime_type": "application/json"}
            )
            data = clean_json_response(response.text)
            if isinstance(data, list):
                return data
            elif isinstance(data, dict) and "recommendations" in data:
                return data["recommendations"]
            return [data]
        except Exception as e:
            logger.warning(f"[GeminiProvider] Error during generate_recommendations: {e}. Falling back.")
            return MockLLMProvider().generate_recommendations_sync(cluster_context)

    async def chat(self, user_message: str, history: List[Dict[str, str]], cluster_context: Dict[str, Any]) -> str:
        if not self._client:
            return MockLLMProvider().chat_sync(user_message, history, cluster_context)

        system_instruction = (
            "You are the CloudScale Intelligent Infrastructure Assistant, pair programming and operating with a Cloud DevOps engineer.\n"
            "Answer questions accurately using the provided real-time cluster state and metrics.\n"
            "Be concise, technical, precise, and polite. When mentioning hosts or workloads, use code formatting.\n"
            "If asked about actions, explain the operational consequences and trade-offs clearly."
        )

        context_summary = f"CURRENT CLUSTER STATE:\n{json.dumps(cluster_context, indent=2)}\n\n"
        full_prompt = f"{context_summary}USER QUERY: {user_message}"

        try:
            response = self._client.models.generate_content(
                model=self.model_name,
                contents=full_prompt,
                config={"system_instruction": system_instruction}
            )
            return response.text
        except Exception as e:
            logger.warning(f"[GeminiProvider] Error during chat: {e}. Falling back.")
            return MockLLMProvider().chat_sync(user_message, history, cluster_context)


class MockLLMProvider(LLMProvider):
    """Deterministic, resilient rule-based engine providing high-fidelity fallback."""

    def diagnose_incident_sync(self, incident_data: Dict[str, Any]) -> Dict[str, Any]:
        node_name = incident_data.get("hostname", "unknown-node")
        cpu = incident_data.get("cpu_utilization_percent", 85.0)
        mem = incident_data.get("memory_utilization_percent", 70.0)

        if cpu >= 85.0:
            return {
                "title": f"Host CPU Starvation on {node_name}",
                "root_cause": f"Host {node_name} is operating at {cpu}% CPU saturation. Compute allocation density exceeds safe threshold without sufficient scheduling headroom.",
                "severity": "CRITICAL" if cpu >= 90.0 else "HIGH",
                "affected_components": [node_name],
                "remediation_steps": [
                    f"Drain non-critical batch workloads from {node_name}",
                    "Trigger autoscaling controller to provision replica headroom",
                    "Verify if noisy-neighbor process is looping"
                ],
                "confidence_score": 0.94
            }
        else:
            return {
                "title": f"Telemetry Alert on {node_name}",
                "root_cause": f"Resource utilization detected at {cpu}% CPU and {mem}% memory. Workloads are operating near capacity limits.",
                "severity": "MEDIUM",
                "affected_components": [node_name],
                "remediation_steps": [
                    "Monitor saturation trends over the next 15 minutes",
                    "Review scaling policy thresholds"
                ],
                "confidence_score": 0.88
            }

    async def diagnose_incident(self, incident_data: Dict[str, Any]) -> Dict[str, Any]:
        return self.diagnose_incident_sync(incident_data)

    def generate_recommendations_sync(self, cluster_context: Dict[str, Any]) -> List[Dict[str, Any]]:
        nodes = cluster_context.get("nodes", [])
        recs = []

        for node in nodes:
            cpu = node.get("cpuUtilizationPercent", 0.0)
            hostname = node.get("hostname", "node")
            if cpu >= 80.0:
                recs.append({
                    "category": "NODE_REBALANCING",
                    "title": f"Rebalance Workloads on Saturated Host {hostname}",
                    "explanation": f"Host {hostname} is currently at {cpu}% CPU saturation. Migrating non-critical services will restore 25% scheduling headroom and balance variance.",
                    "confidence_score": 0.92,
                    "expected_impact": "Reduces host CPU saturation from " + str(cpu) + "% to ~55%",
                    "suggested_action": "DRAIN_NODE",
                    "action_payload": {"nodeId": node.get("id"), "action": "DRAIN"}
                })
            elif cpu <= 25.0 and node.get("status") == "ACTIVE":
                recs.append({
                    "category": "RIGHTSIZING",
                    "title": f"Consolidate Underutilized Host {hostname}",
                    "explanation": f"Host {hostname} is consuming baseline power with only {cpu}% CPU utilization. Workloads can be consolidated using Best-Fit scheduler.",
                    "confidence_score": 0.86,
                    "expected_impact": "Saves 1 idle compute node overhead",
                    "suggested_action": "MIGRATE_WORKLOAD",
                    "action_payload": {"nodeId": node.get("id"), "action": "CONSOLIDATE"}
                })

        if not recs:
            recs.append({
                "category": "SCALING_TUNING",
                "title": "Cluster Operating in Optimal Equilibrium",
                "explanation": "Compute nodes and workloads are well balanced. No immediate rebalancing or rightsizing required.",
                "confidence_score": 0.95,
                "expected_impact": "Maintain current operational state",
                "suggested_action": "NOOP",
                "action_payload": {}
            })

        return recs

    async def generate_recommendations(self, cluster_context: Dict[str, Any]) -> List[Dict[str, Any]]:
        return self.generate_recommendations_sync(cluster_context)

    def chat_sync(self, user_message: str, history: List[Dict[str, str]], cluster_context: Dict[str, Any]) -> str:
        nodes = cluster_context.get("nodes", [])
        total_nodes = len(nodes)
        workloads = cluster_context.get("workloads", [])
        total_workloads = len(workloads)

        msg_lower = user_message.lower()
        if "highest cpu" in msg_lower or "hot node" in msg_lower:
            if nodes:
                sorted_nodes = sorted(nodes, key=lambda n: n.get("cpuUtilizationPercent", 0), reverse=True)
                hot = sorted_nodes[0]
                return f"Host `{hot.get('hostname')}` currently has the highest CPU utilization at **{hot.get('cpuUtilizationPercent')}%** with {hot.get('allocatedCpuCores')}/{hot.get('totalCpuCores')} cores allocated."
            return "No compute nodes are currently reporting telemetry."
        elif "workload" in msg_lower or "running" in msg_lower:
            return f"The cluster currently hosts **{total_workloads}** workloads across **{total_nodes}** compute nodes. All scheduler assignments are tracked with deterministic capacity reservation."
        else:
            return f"CloudScale is monitoring **{total_nodes} compute hosts** and **{total_workloads} workloads**. Cluster health is nominal. Let me know if you would like me to diagnose an incident, evaluate rightsizing, or inspect node saturation."

    async def chat(self, user_message: str, history: List[Dict[str, str]], cluster_context: Dict[str, Any]) -> str:
        return self.chat_sync(user_message, history, cluster_context)


def get_llm_provider() -> LLMProvider:
    if settings.GEMINI_API_KEY:
        return GeminiProvider(api_key=settings.GEMINI_API_KEY, model_name=settings.GEMINI_MODEL)
    return MockLLMProvider()
