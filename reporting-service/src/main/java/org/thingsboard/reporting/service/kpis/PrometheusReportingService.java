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
import io.prometheus.client.Gauge;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.stats.KpiEntry;
import org.thingsboard.server.common.data.stats.KpiKey;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrometheusReportingService {
    private final CollectorRegistry collectorRegistry;

    private final Map<KpiKey, Gauge> gauges = new EnumMap<>(KpiKey.class);

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

    public void onKpiStatsUpdate(TenantId tenantId, List<KpiEntry> kpiEntries) {
        kpiEntries.forEach(kpiEntry -> {
            gauges.get(kpiEntry.getKey()).labels(tenantId.toString()).inc(kpiEntry.getValue());
        });

        Arrays.stream(KpiKey.values())
                .filter(KpiKey::isComputed)
                .forEach(kpiKey -> gauges.get(kpiKey).labels(tenantId.toString())
                        .set(kpiKey.compute(key -> (long) gauges.get(key).labels(tenantId.toString()).get())));
    }

}
