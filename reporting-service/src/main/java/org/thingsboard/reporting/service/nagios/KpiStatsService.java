package org.thingsboard.reporting.service.nagios;

import lombok.Getter;
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
import org.thingsboard.server.queue.util.AfterStartUp;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

    public KpiStats getCurrentKpiStats() {
        try {
            requestEntitiesKpiStats().forEach(kpiEntry -> {
                currentKpiStats.set(kpiEntry.getKey(), kpiEntry.getValue());
            });
        } catch (Exception e) {
            log.error("Failed to update entities KPI stats: {}", ExceptionUtils.getRootCauseMessage(e));
        }
        return currentKpiStats;
    }

//    @AfterStartUp
//    public void initEntitiesKpiStatsUpdating() {
//        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
//            try {
//
//            } catch (Exception e) {
//                log.error("Failed to update entities KPI stats: {}", ExceptionUtils.getRootCauseMessage(e));
//            }
//        }, 0, 10, TimeUnit.SECONDS);
//    }

    private void onKpiStatsUpdate(TransportProtos.KpiUpdateMsg kpiUpdateMsg) {
        kpiUpdateMsg.getKpiKVsList().stream()
                .map(this::toKpiEntry)
                .forEach(kpiEntry -> {
                    currentKpiStats.increase(kpiEntry.getKey(), kpiEntry.getValue());
                });
    }

    private List<KpiEntry> requestEntitiesKpiStats() throws Exception {
        TransportProtos.GetEntitiesKpiStatsResponseMsg responseMsg = getEntitiesKpiStats(
                TransportProtos.GetEntitiesKpiStatsRequestMsg.getDefaultInstance()
        );
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
