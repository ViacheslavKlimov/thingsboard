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
package org.thingsboard.reporting.netcool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
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
        private Long ruleId;
        private String state;
        private Tags tags;

        @Data
        public static class Tags {
            private String severity;
            private String type;
        }
    }

}
