package org.thingsboard.reporting.service.kpis;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.stats.KpiEntry;
import org.thingsboard.server.common.data.stats.KpiKey;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PrometheusReportingService {
    private final KpiStatsService kpiStatsService;
    private final CollectorRegistry collectorRegistry;

    private final Map<KpiKey, Gauge> gauges = new EnumMap<>(KpiKey.class);
    private final Map<TenantId, Map<KpiKey, Gauge>> kpiStats = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        Arrays.stream(KpiKey.values()).forEach(kpiKey -> {
            gauges.put(kpiKey, Gauge.build()
                    .name(kpiKey.name().toLowerCase())
                    .help(kpiKey.name().toLowerCase())
                    .labelNames("tenantId")
                    .register(collectorRegistry));
        });
    }

    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 60 * 1000)
    public void updateEntitiesKpiStats() {
//        kpiStatsService.requestEntitiesKpiStats(TransportProtos.GetEntitiesKpiStatsRequestMsg.newBuilder()
//                .setNewCreatedDevicesTimeFrom(0L)
//                .setTenantIdMSB()
//                .build());
    }

    public void onKpiStatsUpdate(TenantId tenantId, List<KpiEntry> kpiEntries) {
        kpiEntries.forEach(kpiEntry -> {
            gauges.get(kpiEntry.getKey()).labels(tenantId.toString()).inc(kpiEntry.getValue());
        });
    }

}
