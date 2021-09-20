package org.thingsboard.reporting.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.stats.KpiStats;
import org.thingsboard.server.common.data.stats.SystemAlarm;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Service
public class MonitoringServiceApiClient {
    @Value("${monitoring_service_url}")
    private String monitoringServiceUrl;

    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.of(10, ChronoUnit.SECONDS))
            .build();


    public KpiStats requestKpiStatsForNagios() {
        return restTemplate.getForObject(monitoringServiceUrl + "/api/monitoring/nagios/kpi_stats", KpiStats.class);
    }

    public void publishSystemAlarm(SystemAlarm systemAlarm) {
        restTemplate.postForObject(monitoringServiceUrl + "/api/monitoring/system_alarm", systemAlarm, Void.class);
    }

}
