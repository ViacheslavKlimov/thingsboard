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
package org.thingsboard.monitoring.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.monitoring.service.nagios.NagiosKpisService;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.stats.AlarmCategory;
import org.thingsboard.server.common.data.stats.AlarmSeverity;
import org.thingsboard.server.common.data.stats.KpiStats;
import org.thingsboard.server.common.data.stats.SystemAlarm;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringApiController {
    private final NagiosKpisService nagiosKpisService;
    private final TbApiUsageReportClient apiUsageReportClient;

    @GetMapping("/nagios/kpi_stats")
    public KpiStats getCurrentKpiStatsForNagios() {
        return nagiosKpisService.getCurrentKpiStats();
    }

    @PostMapping("/system_alarm")
    public void onSystemAlarm(@RequestBody SystemAlarm systemAlarm) {
        AlarmSeverity severity = systemAlarm.getSeverity();
        AlarmCategory category = systemAlarm.getCategory();

        if (severity == AlarmSeverity.MAJOR || severity == AlarmSeverity.CRITICAL) {
            ApiUsageRecordKey systemAlarmCountKey;
            switch (category) {
                case COMMUNICATION:
                    systemAlarmCountKey = ApiUsageRecordKey.COMMUNICATION_SYSTEM_ALARMS_COUNT;
                    break;
                case QUALITY_OF_SERVICE:
                    systemAlarmCountKey = ApiUsageRecordKey.QUALITY_OF_SERVICE_SYSTEM_ALARMS_COUNT;
                    break;
                case PROCESSING:
                    systemAlarmCountKey = ApiUsageRecordKey.PROCESSING_SYSTEM_ALARMS_COUNT;
                    break;
                case EQUIPMENT:
                    systemAlarmCountKey = ApiUsageRecordKey.EQUIPMENT_SYSTEM_ALARMS_COUNT;
                    break;
                case ENVIRONMENTAL:
                    systemAlarmCountKey = ApiUsageRecordKey.ENVIRONMENTAL_SYSTEM_ALARMS_COUNT;
                    break;
                default:
                    systemAlarmCountKey = null;
            }

            if (systemAlarmCountKey != null) {
                apiUsageReportClient.report(TenantId.SYS_TENANT_ID, null, systemAlarmCountKey);
            }
        }
    }

}
