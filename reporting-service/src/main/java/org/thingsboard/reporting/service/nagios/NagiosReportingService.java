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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.reporting.service.mapping.PayloadMapper;
import org.thingsboard.reporting.snmp.SnmpAgent;
import org.thingsboard.reporting.snmp.SnmpAgent.StringSnmpVariable;
import org.thingsboard.server.common.data.stats.KpiStatistics;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private String kpiStatsOid;
    @Value("${nagios.kpi_statistics.updating_period}")
    private int kpiStatsUpdatingPeriod;
    @Value("${nagios.kpi_statistics.load_on_request}")
    private Boolean loadKpiStatsOnRequest;

    private final TransportService transportService;
    private final DataDecodingEncodingService dataDecodingEncodingService;
    private final PayloadMapper payloadMapper;

    @AfterStartUp
    public void run() throws IOException {
        snmpAgent = new SnmpAgent(snmpPort, snmpCommunity);
        snmpAgent.start();

        if (loadKpiStatsOnRequest) {
            snmpAgent.registerStringVariable(kpiStatsOid, () -> {
                return mapToString(getKpiStatistics());
            });
        } else {
            StringSnmpVariable kpiStatisticsVariable = snmpAgent.registerStringVariable(kpiStatsOid);
            Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
                try {
                    kpiStatisticsVariable.setValue(mapToString(getKpiStatistics()));
                } catch (Exception e) {
                    log.error("Failed to retrieve KPI statistics: {}", ExceptionUtils.getRootCauseMessage(e));
                }
            }, 0, kpiStatsUpdatingPeriod, TimeUnit.MILLISECONDS);
        }

    }

    @SneakyThrows
    private KpiStatistics getKpiStatistics() {
        TransportProtos.GetKpiStatisticsResponseMsg responseMsg = transportService.getKpiStatistics(
                TransportProtos.GetKpiStatisticsRequestMsg.getDefaultInstance()
        ).get(2, TimeUnit.SECONDS);
        return (KpiStatistics) dataDecodingEncodingService.decode(responseMsg.getData().toByteArray())
                .orElseThrow(() -> new IllegalStateException("failed to decode"));
    }

    private String mapToString(KpiStatistics kpiStatistics) {
        return payloadMapper.convertKpiStatisticsToString(kpiStatistics);
    }
}
