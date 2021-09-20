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
package org.thingsboard.reporting.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.reporting.service.MonitoringServiceApiClient;
import org.thingsboard.reporting.service.netcool.NetcoolReportingService;
import org.thingsboard.server.common.data.stats.AlarmCategory;
import org.thingsboard.server.common.data.stats.AlarmSeverity;
import org.thingsboard.server.common.data.stats.SystemAlarm;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PrometheusAlarmController {
    private final NetcoolReportingService netcoolReportingService;
    private final MonitoringServiceApiClient monitoringServiceApiClient;

    @PostMapping("/alarm")
    public void processAlarm(@RequestBody PrometheusAlarm alarm) {
        try {
            toSystemAlarms(alarm).forEach(systemAlarm -> {
                netcoolReportingService.onAlarm(systemAlarm);
                monitoringServiceApiClient.publishSystemAlarm(systemAlarm);
            });
        } catch (Exception e) {
            log.error("Failed to process Prometheus alarm {}", alarm, e);
        }
    }

    private List<SystemAlarm> toSystemAlarms(PrometheusAlarm prometheusAlarm) {
        return prometheusAlarm.getAlerts().stream()
                .peek(alert -> {
                    if (StringUtils.isEmpty(alert.getStatus())) {
                        throw new IllegalArgumentException("alert status is not specified");
                    }
                    if (StringUtils.isEmpty(alert.getAlertName())) {
                        throw new IllegalArgumentException("alert name is not specified");
                    }
                    if (StringUtils.isEmpty(alert.getSeverity())) {
                        throw new IllegalArgumentException("alert severity is not specified");
                    }
                    if (StringUtils.isEmpty(alert.getDescription()) && StringUtils.isEmpty(alert.getSummary())) {
                        throw new IllegalArgumentException("neither alert description nor summary is specified");
                    }
                })
                .map(alert -> {
                    SystemAlarm systemAlarm = new SystemAlarm();
                    systemAlarm.setTitle(getAlarmTitle(alert));
                    systemAlarm.setCategory(resolveAlarmCategory(alert));
                    systemAlarm.setSeverity(resolveAlarmSeverity(alert));
                    return systemAlarm;
                })
                .collect(Collectors.toList());
    }

    private String getAlarmTitle(PrometheusAlarm.Alert alert) {
        return StringUtils.defaultIfEmpty(alert.getDescription(), alert.getSummary());
    }

    private AlarmSeverity resolveAlarmSeverity(PrometheusAlarm.Alert alert) {
        if (StringUtils.equalsIgnoreCase(alert.getStatus(), "resolved")) {
            return AlarmSeverity.CLEARED;
        } else {
            String prometheusAlarmSeverity = alert.getSeverity();
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

    private AlarmCategory resolveAlarmCategory(PrometheusAlarm.Alert alert) {
        return AlarmCategory.forAlertName(alert.getAlertName())
                .orElseThrow(() -> new IllegalArgumentException("failed to resolve alarm category"));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class PrometheusAlarm {
        private List<Alert> alerts;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Alert {
            private String status; // firing, resolved
            private Map<String, String> labels;
            private Map<String, String> annotations;

            public String getSeverity() { // info, warning, critical
                return Optional.ofNullable(labels)
                        .map(labels -> labels.get("severity"))
                        .orElse(null);
            }

            public String getAlertName() {
                return Optional.ofNullable(labels)
                        .map(labels -> labels.get("alertname"))
                        .orElse(null);
            }

            public String getDescription() {
                return Optional.ofNullable(annotations)
                        .map(annotations -> annotations.get("description"))
                        .orElse(null);
            }

            public String getSummary() {
                return Optional.ofNullable(annotations)
                        .map(annotations -> annotations.get("summary"))
                        .orElse(null);
            }

        }

    }

}
