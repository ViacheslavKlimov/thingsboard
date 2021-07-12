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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.reporting.snmp.SnmpAgent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.stats.KpiEntry;
import org.thingsboard.server.common.data.stats.KpiKey;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NagiosReportingService {

    @Value("${snmp.binding_port}")
    private int snmpPort;
    @Value("${snmp.community}")
    private String snmpCommunity;
    @Value("${nagios.kpi_statistics.oid}")
    private String kpiStatsBaseOid;

    private final KpiStatsService kpiStatsService;

    private final KpiStats currentKpiStats = new KpiStats();
    private final AtomicLong lastRequestTime = new AtomicLong();

    @AfterStartUp
    public void run() throws Exception {
        SnmpAgent snmpAgent = new SnmpAgent(snmpPort, snmpCommunity);
        snmpAgent.start();

        List<Integer> ids = Arrays.stream(KpiKey.values()).map(KpiKey::getId).filter(Objects::nonNull).collect(Collectors.toList());
        snmpAgent.registerVariables(ids, () -> {
            if (System.currentTimeMillis() - lastRequestTime.get() <= TimeUnit.SECONDS.toMillis(2)) {
                return toValues(currentKpiStats);
            }

            KpiStats kpiStats = collectKpiStats();
            Map<Integer, Object> values = toValues(kpiStats);

            kpiStats.nullify(KpiKey::isAccumulated);

            return values;
        }, kpiStatsBaseOid);
    }

    private KpiStats collectKpiStats() {
        if (lastRequestTime.get() == 0) {
            lastRequestTime.set(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1));
        }

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

        lastRequestTime.set(System.currentTimeMillis());

        return currentKpiStats;
    }

    public void onKpiStatsUpdate(List<KpiEntry> kpiEntries) {
        kpiEntries.forEach(kpiEntry -> {
            currentKpiStats.increase(kpiEntry.getKey(), kpiEntry.getValue());
        });
    }

    private Map<Integer, Object> toValues(KpiStats kpiStats) {
        Map<Integer, Object> values = new HashMap<>();
        getKpiKeysToReport().forEach(kpiKey -> {
            values.put(kpiKey.getId(), kpiStats.getOrDefault(kpiKey, kpiKey.getDefaultValue()));
        });
        return values;
    }

    private List<KpiKey> getKpiKeysToReport() {
        return Arrays.stream(KpiKey.values())
                .filter(kpiKey -> kpiKey.getId() != null)
                .collect(Collectors.toList());
    }

}
