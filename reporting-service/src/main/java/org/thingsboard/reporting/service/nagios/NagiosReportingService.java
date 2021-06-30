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
package org.thingsboard.reporting.service.nagios;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.reporting.snmp.SnmpAgent;
import org.thingsboard.server.common.data.stats.KpiKey;
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
    private SnmpAgent snmpAgent;

    @Value("${snmp.binding_port}")
    private int snmpPort;
    @Value("${snmp.community}")
    private String snmpCommunity;
    @Value("${nagios.kpi_statistics.oid}")
    private String kpiStatsBaseOid;

    private final KpiStatsService kpiStatsService;

    private final AtomicLong lastRequestTime = new AtomicLong();

    @AfterStartUp
    public void run() throws Exception {
        snmpAgent = new SnmpAgent(snmpPort, snmpCommunity);
        snmpAgent.start();

        List<Integer> ids = Arrays.stream(KpiKey.values()).map(KpiKey::getId).filter(Objects::nonNull).collect(Collectors.toList());
        snmpAgent.registerVariables(ids, () -> {
            if (System.currentTimeMillis() - lastRequestTime.get() <= TimeUnit.SECONDS.toMillis(5)) {
                return toValues(kpiStatsService.getRawKpiStats());
            }

            KpiStats kpiStats = kpiStatsService.getCurrentKpiStats();
            Map<Integer, Object> values = toValues(kpiStats);

            kpiStats.nullify(KpiKey::isAccumulated);
            lastRequestTime.set(System.currentTimeMillis());

            return values;
        }, kpiStatsBaseOid);
    }

    private Map<Integer, Object> toValues(KpiStats kpiStats) {
        Map<Integer, Object> values = new HashMap<>();
        getKpiKeysToReport().forEach(kpiKey -> {
            values.put(kpiKey.getId(), kpiKey + "=" + kpiStats.getOrDefault(kpiKey, kpiKey.getDefaultValue()));
        });
        return values;
    }

    private List<KpiKey> getKpiKeysToReport() {
        return Arrays.stream(KpiKey.values())
                .filter(kpiKey -> kpiKey.getId() != null)
                .collect(Collectors.toList());
    }

}
