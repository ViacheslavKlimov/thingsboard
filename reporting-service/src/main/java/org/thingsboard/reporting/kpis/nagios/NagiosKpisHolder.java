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
package org.thingsboard.reporting.kpis.nagios;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.reporting.kpis.KpiStats;
import org.thingsboard.reporting.kpis.KpiStatsService;
import org.thingsboard.reporting.util.MonitoringComponent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.stats.KpiEntry;
import org.thingsboard.server.common.data.stats.KpiKey;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequiredArgsConstructor
@Slf4j
@MonitoringComponent
public class NagiosKpisHolder {
    private final KpiStatsService kpiStatsService;

    private final KpiStats currentKpiStats = new KpiStats();
    private final AtomicLong lastRequestTime = new AtomicLong();

    @GetMapping("/kpi_stats")
    public KpiStats getCurrentKpiStats() {
        if (lastRequestTime.get() == 0) {
            lastRequestTime.set(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1));
        }

        KpiStats kpiStats = collectKpiStats();

        lastRequestTime.set(System.currentTimeMillis());
        currentKpiStats.nullify(KpiKey::isAccumulated);

        return kpiStats;
    }

    private KpiStats collectKpiStats() {
        TransportProtos.GetEntitiesKpiStatsRequestMsg request = TransportProtos.GetEntitiesKpiStatsRequestMsg.newBuilder()
                .setNewCreatedDevicesTimeFrom(lastRequestTime.get())
                .setTenantIdMSB(TenantId.SYS_TENANT_ID.getId().getMostSignificantBits())
                .setTenantIdLSB(TenantId.SYS_TENANT_ID.getId().getLeastSignificantBits())
                .build();
        try {
            kpiStatsService.requestEntitiesKpiStats(request).forEach(kpiEntry -> {
                currentKpiStats.set(kpiEntry.getKey(), kpiEntry.getValue());
            });
        } catch (Exception e) {
            log.error("Failed to update entities KPI stats", e);
        }

        Arrays.stream(KpiKey.values())
                .filter(KpiKey::isComputed)
                .forEach(kpiKey -> {
                    currentKpiStats.set(kpiKey, kpiKey.compute(key -> currentKpiStats.getOrDefault(key, 0L)));
                });

        return currentKpiStats.clone();
    }

    public void onKpiStatsUpdate(List<KpiEntry> kpiEntries) {
        kpiEntries.forEach(kpiEntry -> {
            currentKpiStats.increase(kpiEntry.getKey(), kpiEntry.getValue());
        });
    }

}
