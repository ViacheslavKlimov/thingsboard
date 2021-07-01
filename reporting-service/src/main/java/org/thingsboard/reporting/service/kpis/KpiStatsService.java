/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.reporting.service.kpis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KpiStatsService extends DefaultTransportService {
    private final NagiosReportingService nagiosReportingService;
    private final PrometheusReportingService prometheusReportingService;

    public KpiStatsService(TbServiceInfoProvider serviceInfoProvider, TbTransportQueueFactory queueProvider,
                           TbQueueProducerProvider producerProvider, PartitionService partitionService, StatsFactory statsFactory,
                           TransportDeviceProfileCache deviceProfileCache, TransportTenantProfileCache tenantProfileCache,
                           TbApiUsageReportClient apiUsageClient, TransportRateLimitService rateLimitService,
                           DataDecodingEncodingService dataDecodingEncodingService, SchedulerComponent scheduler,
                           TransportResourceCache transportResourceCache, ApplicationEventPublisher eventPublisher,
                           @Lazy NagiosReportingService nagiosReportingService, @Lazy PrometheusReportingService prometheusReportingService) {
        super(serviceInfoProvider, queueProvider, producerProvider, partitionService, statsFactory,
                deviceProfileCache, tenantProfileCache, apiUsageClient, rateLimitService,
                dataDecodingEncodingService, scheduler, transportResourceCache, eventPublisher);
        this.nagiosReportingService = nagiosReportingService;
        this.prometheusReportingService = prometheusReportingService;
    }

    public List<KpiEntry> requestEntitiesKpiStats(TransportProtos.GetEntitiesKpiStatsRequestMsg requestMsg) {
        TransportProtos.GetEntitiesKpiStatsResponseMsg responseMsg = getEntitiesKpiStats(requestMsg);
        return responseMsg.getKpiKVsList().stream()
                .map(KpiStatsService::toKpiEntry)
                .collect(Collectors.toList());
    }

    public static KpiEntry toKpiEntry(TransportProtos.KpiKV kpiKV) {
        KpiKey kpiKey = KpiKey.valueOf(kpiKV.getKey());
        Long value = kpiKV.getValue();

        return new KpiEntry(kpiKey, value);
    }

    @Override
    protected void processToTransportMsg(TransportProtos.ToTransportMsg toSessionMsg) {
        super.processToTransportMsg(toSessionMsg);
        if (toSessionMsg.hasKpiUpdateMsg()) {
            TransportProtos.KpiUpdateMsg kpiUpdateMsg = toSessionMsg.getKpiUpdateMsg();

            TenantId tenantId = new TenantId(new UUID(kpiUpdateMsg.getTenantIdMSB(), kpiUpdateMsg.getTenantIdLSB()));
            List<KpiEntry> kpiEntries = kpiUpdateMsg.getKpiKVsList().stream()
                    .map(KpiStatsService::toKpiEntry)
                    .collect(Collectors.toList());

            if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
                nagiosReportingService.onKpiStatsUpdate(kpiEntries);
            }
            prometheusReportingService.onKpiStatsUpdate(tenantId, kpiEntries);
        }
    }

}
