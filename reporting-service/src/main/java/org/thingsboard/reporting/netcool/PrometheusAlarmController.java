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

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PrometheusAlarmController {
    private final NetcoolReportingService netcoolReportingService;

    @PostMapping("/alarm")
    public void postAlarm(@RequestBody PrometheusAlarm alarm) {
        log.info("Received new alarm from Prometheus: {}", alarm);
        try {
            netcoolReportingService.onAlarm(toNetcoolAlarm(alarm));
        } catch (Exception e) {
            log.error("Failed to process Prometheus alarm {}", alarm, e);
        }
    }

    private NetcoolAlarm toNetcoolAlarm(PrometheusAlarm prometheusAlarm) {
        if (StringUtils.isAnyEmpty(prometheusAlarm.getStatus(), prometheusAlarm.getAlertName(), prometheusAlarm.getSeverity())) {
            throw new IllegalArgumentException("some of required fields is empty");
        }

        NetcoolAlarm netcoolAlarm = new NetcoolAlarm();
        netcoolAlarm.setTitle(getAlarmTitle(prometheusAlarm));
        netcoolAlarm.setCategory(resolveAlarmCategory(prometheusAlarm));
        netcoolAlarm.setSeverity(resolveAlarmSeverity(prometheusAlarm));
        return netcoolAlarm;
    }

    private String getAlarmTitle(PrometheusAlarm prometheusAlarm) {
        return prometheusAlarm.getDescription();
    }

    private AlarmSeverity resolveAlarmSeverity(PrometheusAlarm prometheusAlarm) {
        if (StringUtils.equalsIgnoreCase(prometheusAlarm.getStatus(), "resolved")) {
            return AlarmSeverity.CLEARED;
        } else {
            String prometheusAlarmSeverity = prometheusAlarm.getSeverity();
            switch (prometheusAlarmSeverity) {
                case "critical":
                    return AlarmSeverity.CRITICAL;
                case "info":
                case "warning":
                    return AlarmSeverity.WARNING;
                default:
                    throw new IllegalArgumentException("unknown alarm severity " + prometheusAlarmSeverity);
            }
        }
    }

    private AlarmCategory resolveAlarmCategory(PrometheusAlarm alarm) {
        return AlarmCategory.forAlert(alarm.getAlertName())
                .orElseThrow(() -> new IllegalArgumentException("failed to resolve alarm category"));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class PrometheusAlarm {
        private String status; // firing, resolved
        private List<Alert> alerts;
        private Map<String, String> groupLabels;
        private Map<String, String> commonLabels;
        private Map<String, String> commonAnnotations;

        @Data
        public static class Alert {
            private String status; // firing, resolved
            private Map<String, String> labels;
            private Map<String, String> annotations;
        }

        public String getSeverity() { // info, warning, critical
            return Optional.ofNullable(commonLabels)
                    .map(labels -> labels.get("severity"))
                    .orElse(null);
        }

        public String getAlertName() {
            return Optional.ofNullable(commonLabels)
                    .map(labels -> labels.get("alertname"))
                    .orElse(null);
        }

        public String getDescription() {
            return Optional.ofNullable(commonAnnotations)
                    .map(annotations -> annotations.get("description"))
                    .orElse(null);
        }

        public String getSummary() {
            return Optional.ofNullable(commonAnnotations)
                    .map(annotations -> annotations.get("summary"))
                    .orElse(null);
        }

    }

}
