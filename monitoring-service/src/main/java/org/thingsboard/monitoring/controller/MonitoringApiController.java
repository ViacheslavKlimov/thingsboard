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
