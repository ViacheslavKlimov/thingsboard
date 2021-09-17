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
import org.thingsboard.reporting.service.MonitoringServiceApiClient;
import org.thingsboard.reporting.util.SnmpAgent;
import org.thingsboard.server.common.data.stats.KpiKey;
import org.thingsboard.server.common.data.stats.KpiStats;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NagiosReportingService {
    @Value("${snmp.binding_port}")
    private int snmpPort;
    @Value("${snmp.community}")
    private String snmpCommunity;
    @Value("${nagios.kpi_statistics.oid}")
    private String baseOid;

    private final MonitoringServiceApiClient monitoringServiceApiClient;

    private final List<KpiKey> kpiKeysToReport = Arrays.stream(KpiKey.values())
            .filter(kpiKey -> kpiKey.getId() != null)
            .collect(Collectors.toList());

    @PostConstruct
    public void run() throws Exception {
        SnmpAgent snmpAgent = new SnmpAgent(snmpPort, snmpCommunity);
        snmpAgent.start();

        List<Integer> ids = Arrays.stream(KpiKey.values()).map(KpiKey::getId).filter(Objects::nonNull).collect(Collectors.toList());
        snmpAgent.registerVariables(ids, () -> {
            KpiStats kpiStats;
            try {
                kpiStats = monitoringServiceApiClient.requestKpiStatsForNagios();
                if (kpiStats == null || kpiStats.getEntries() == null) {
                    throw new IllegalStateException("empty response");
                }
            } catch (Exception e) {
                log.error("Failed to get KPI stats", e);
                kpiStats = new KpiStats();
            }
            return toValues(kpiStats);
        }, baseOid);
    }

    private Map<Integer, Object> toValues(KpiStats kpiStats) {
        return kpiKeysToReport.stream()
                .collect(Collectors.toMap(
                        KpiKey::getId, kpiKey -> kpiStats.getOrDefault(kpiKey, kpiKey.getDefaultValue())
                ));
    }

}
