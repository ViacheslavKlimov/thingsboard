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

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.stats.KpiEntry;
import org.thingsboard.server.common.data.stats.KpiKey;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.GetTenantsIdsRequestMsg;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrometheusReportingService {
    private final CollectorRegistry collectorRegistry;
    private final KpiStatsService kpiStatsService;
    private final TransportService transportService;

    private final Map<KpiKey, Counter> counters = new EnumMap<>(KpiKey.class);
    private final Map<KpiKey, Gauge> gauges = new EnumMap<>(KpiKey.class);

    @Scheduled(initialDelay = 0, fixedDelay = 60 * 1000)
    public void updateEntitiesKpiStats() {
        List<TransportProtos.Id> tenantsIds = transportService.getTenantsIds(GetTenantsIdsRequestMsg.getDefaultInstance()).getTenantsIdsList();

        tenantsIds.forEach(this::updateEntitiesKpiStatsForTenant);
        updateEntitiesKpiStatsForTenant(TransportProtos.Id.newBuilder()
                .setMsb(TenantId.SYS_TENANT_ID.getId().getMostSignificantBits())
                .setLsb(TenantId.SYS_TENANT_ID.getId().getLeastSignificantBits())
                .build());
    }

    private void updateEntitiesKpiStatsForTenant(TransportProtos.Id tenantId) {
        List<KpiEntry> entitiesKpiStats = kpiStatsService.requestEntitiesKpiStats(TransportProtos.GetEntitiesKpiStatsRequestMsg.newBuilder()
                .setTenantIdMSB(tenantId.getMsb())
                .setTenantIdLSB(tenantId.getLsb())
                .setNewCreatedDevicesTimeFrom(-1)
                .build());
        entitiesKpiStats.forEach(kpiEntry -> {
            Gauge gauge = getGauge(kpiEntry.getKey());
            gauge.labels(new UUID(tenantId.getMsb(), tenantId.getLsb()).toString()).set(kpiEntry.getValue());
        });
    }

    public void onKpiStatsUpdate(TenantId tenantId, List<KpiEntry> kpiEntries) {
        kpiEntries.forEach(kpiEntry -> {
            Counter counter = getCounter(kpiEntry.getKey());
            counter.labels(tenantId.toString()).inc(kpiEntry.getValue());
        });

        Arrays.stream(KpiKey.values())
                .filter(KpiKey::isComputed)
                .forEach(kpiKey -> getGauge(kpiKey).labels(tenantId.toString())
                        .set(kpiKey.compute(key -> {
                            return (long) getCounter(key).labels(tenantId.toString()).get();
                        })));
    }

    private Counter getCounter(KpiKey kpiKey) {
        return counters.computeIfAbsent(kpiKey, key -> Counter.build()
                .name(key.name().toLowerCase())
                .help(key.name().toLowerCase())
                .labelNames("tenantId")
                .register(collectorRegistry));
    }

    private Gauge getGauge(KpiKey kpiKey) {
        return gauges.computeIfAbsent(kpiKey, key -> Gauge.build()
                .name(key.name().toLowerCase())
                .help(key.name().toLowerCase())
                .labelNames("tenantId")
                .register(collectorRegistry));
    }

}
