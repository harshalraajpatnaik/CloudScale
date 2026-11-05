package com.cloudscale;

import com.cloudscale.common.controller.HealthController;
import com.cloudscale.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CloudScaleHealthControllerTest {

    private final HealthController healthController = new HealthController();

    @Test
    void healthEndpointReturnsUpStatus() {
        ResponseEntity<ApiResponse<Map<String, Object>>> response = healthController.getHealth();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        ApiResponse<Map<String, Object>> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.success());
        assertEquals("UP", body.data().get("status"));
        assertEquals("cloudscale-control-plane", body.data().get("service"));
    }
}
