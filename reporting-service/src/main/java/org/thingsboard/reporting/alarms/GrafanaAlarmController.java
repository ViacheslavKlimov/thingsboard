package org.thingsboard.reporting.alarms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.reporting.util.SnmpGatewayComponent;

@RestController
@RequiredArgsConstructor
@Slf4j
@SnmpGatewayComponent
public class GrafanaAlarmController {
    private final NetcoolReportingService netcoolReportingService;

    @PostMapping("/grafana/alarm")
    public void postAlarm(@RequestBody GrafanaAlarm alarm) {
        log.info("Received new alarm from Grafana: {}", alarm);
        try {
            netcoolReportingService.onAlarm(toNetcoolAlarm(alarm));
        } catch (Exception e) {
            log.error("Failed to process Grafana alarm", e);
        }
    }

    private NetcoolAlarm toNetcoolAlarm(GrafanaAlarm grafanaAlarm) {
        NetcoolAlarm netcoolAlarm = new NetcoolAlarm();
        netcoolAlarm.setTitle(grafanaAlarm.getRuleName());
        netcoolAlarm.setCategory(AlarmCategory.valueOf(grafanaAlarm.getTags().getType().toUpperCase()));
        netcoolAlarm.setSeverity(StringUtils.equalsIgnoreCase(grafanaAlarm.getState(), "ok") ?
                AlarmSeverity.CLEARED : AlarmSeverity.valueOf(grafanaAlarm.getTags().getSeverity().toUpperCase()));
        return netcoolAlarm;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class GrafanaAlarm {
        private String ruleName;
        private Integer ruleId;
        private String state;
        private Tags tags;

        @Data
        public static class Tags {
            private String severity;
            private String type;
        }
    }

}
