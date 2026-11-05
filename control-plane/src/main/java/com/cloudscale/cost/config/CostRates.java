package com.cloudscale.cost.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "cloudscale.pricing")
public class CostRates {

    // Configurable sample pricing (industry standard reference rates)
    private BigDecimal cpuCoreHourUsd = new BigDecimal("0.048");
    private BigDecimal memoryGbHourUsd = new BigDecimal("0.006");
    private BigDecimal hoursPerMonth = new BigDecimal("730");

    public BigDecimal getCpuCoreHourUsd() {
        return cpuCoreHourUsd;
    }

    public void setCpuCoreHourUsd(BigDecimal cpuCoreHourUsd) {
        this.cpuCoreHourUsd = cpuCoreHourUsd;
    }

    public BigDecimal getMemoryGbHourUsd() {
        return memoryGbHourUsd;
    }

    public void setMemoryGbHourUsd(BigDecimal memoryGbHourUsd) {
        this.memoryGbHourUsd = memoryGbHourUsd;
    }

    public BigDecimal getHoursPerMonth() {
        return hoursPerMonth;
    }

    public void setHoursPerMonth(BigDecimal hoursPerMonth) {
        this.hoursPerMonth = hoursPerMonth;
    }
}
