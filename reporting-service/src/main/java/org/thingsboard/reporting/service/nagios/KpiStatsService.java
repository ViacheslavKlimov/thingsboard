package org.thingsboard.reporting.service.nagios;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.stats.KpiEntry;
import org.thingsboard.server.common.data.stats.KpiKey;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportResourceCache;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.provider.TbTransportQueueFactory;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KpiStatsService extends DefaultTransportService {

    public KpiStatsService(TbServiceInfoProvider serviceInfoProvider, TbTransportQueueFactory queueProvider,
                           TbQueueProducerProvider producerProvider, PartitionService partitionService, StatsFactory statsFactory,
                           TransportDeviceProfileCache deviceProfileCache, TransportTenantProfileCache tenantProfileCache,
                           TbApiUsageReportClient apiUsageClient, TransportRateLimitService rateLimitService,
                           DataDecodingEncodingService dataDecodingEncodingService, SchedulerComponent scheduler,
                           TransportResourceCache transportResourceCache, ApplicationEventPublisher eventPublisher) {
        super(serviceInfoProvider, queueProvider, producerProvider, partitionService, statsFactory,
                deviceProfileCache, tenantProfileCache, apiUsageClient, rateLimitService,
                dataDecodingEncodingService, scheduler, transportResourceCache, eventPublisher);
    }

    private final KpiStats currentKpiStats = new KpiStats();

    private volatile long lastRequestTime;

    public KpiStats getCurrentKpiStats() {
        collectAdditionalKpiStats();
        lastRequestTime = System.currentTimeMillis();
        return currentKpiStats;
    }

    private void collectAdditionalKpiStats() {
        if (lastRequestTime == 0) {
            lastRequestTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1);
        }

        TransportProtos.GetEntitiesKpiStatsRequestMsg request = TransportProtos.GetEntitiesKpiStatsRequestMsg.newBuilder()
                .setNewCreatedDevicesTimeFrom(lastRequestTime)
                .build();
        try {
            requestEntitiesKpiStats(request).forEach(kpiEntry -> {
                currentKpiStats.set(kpiEntry.getKey(), kpiEntry.getValue());
            });
        } catch (Exception e) {
            log.error("Failed to update entities KPI stats: {}", ExceptionUtils.getRootCauseMessage(e));
        }

        Long allApiCalls = currentKpiStats.getOrDefault(KpiKey.API_CALLS, 0L);
        Long failedApiCalls = currentKpiStats.getOrDefault(KpiKey.FAILED_API_CALLS, 0L);
        if (allApiCalls > 0) {
            currentKpiStats.set(KpiKey.API_CALLS_SUCCESS_RATE, (long) (((double) (allApiCalls - failedApiCalls) / allApiCalls) * 100));
        }
    }

    private void onKpiStatsUpdate(TransportProtos.KpiUpdateMsg kpiUpdateMsg) {
        kpiUpdateMsg.getKpiKVsList().stream()
                .map(this::toKpiEntry)
                .forEach(kpiEntry -> {
                    currentKpiStats.increase(kpiEntry.getKey(), kpiEntry.getValue());
                });
        log.info("KPI stats update: {}. Current stats: {}", kpiUpdateMsg.getKpiKVsList().toString().replace(System.lineSeparator(), " "), currentKpiStats);
    }

    private List<KpiEntry> requestEntitiesKpiStats(TransportProtos.GetEntitiesKpiStatsRequestMsg requestMsg) {
        TransportProtos.GetEntitiesKpiStatsResponseMsg responseMsg = getEntitiesKpiStats(requestMsg);
        return responseMsg.getKpiKVsList().stream()
                .map(this::toKpiEntry)
                .collect(Collectors.toList());
    }

    private KpiEntry toKpiEntry(TransportProtos.KpiKV kpiKV) {
        KpiKey kpiKey = KpiKey.valueOf(kpiKV.getKey());
        Long value = kpiKV.getValue();

        return new KpiEntry(kpiKey, value);
    }

    @Override
    protected void processToTransportMsg(TransportProtos.ToTransportMsg toSessionMsg) {
        super.processToTransportMsg(toSessionMsg);
        if (toSessionMsg.hasKpiUpdateMsg()) {
            onKpiStatsUpdate(toSessionMsg.getKpiUpdateMsg());
        }
    }

}
