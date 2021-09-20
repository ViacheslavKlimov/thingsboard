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
package org.thingsboard.server.service.report;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.MagentaReportType;
import org.thingsboard.server.common.data.stats.KpiKey;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.mail.DefaultMailService;
import org.thingsboard.server.service.mail.MailTemplates;

import javax.annotation.PostConstruct;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.thingsboard.server.utils.TimeUtils.toDate;

@Service
@RequiredArgsConstructor
@Slf4j
@TbCoreComponent

@RestController // TODO: remove trigger endpoint when the final testing is finished
public class ServiceManagementReportService {
    private final TimeseriesService tsService;
    private final ApiUsageStateService apiUsageStateService;
    private final DefaultMailService defaultMailService;
    private final DeviceService deviceService;
    private final PartitionService partitionService;
    private final TenantService tenantService;

    @Value("${reports.magenta.enabled}")
    private boolean magentaReportsEnabled;

    @Value("${reports.magenta.report_type}")
    private MagentaReportType reportType;

    private static final LocalTime REPORT_EMAIL_SENDING_TIME = LocalTime.of(23, 0);
    private static final DateFormat REPORT_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");

    @PostConstruct
    private void sendServiceManagementReportsBySchedule() {
        if (!magentaReportsEnabled) return;

        SchedulerUtils.scheduleForEachDayAtSpecificTime(() -> {
            if (LocalDate.now().getDayOfMonth() == LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth() &&
                    partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition()) {
                sendServiceManagementReports();
            }
        }, REPORT_EMAIL_SENDING_TIME, Executors.newSingleThreadScheduledExecutor());
    }

    @GetMapping("/api/reports/service_management_report/{tenantId}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    public String sendServiceManagementReport(@PathVariable String tenantId) throws ThingsboardException {
        try {
            sendServiceManagementReportForTenant(tenantService.findTenantById(new TenantId(UUID.fromString(tenantId))));
            return "sent successfully";
        } catch (Exception e) {
            return e.toString();
        }
    }

    public void sendServiceManagementReports() {
        tenantService.forEachTenant(tenant -> {
            try {
                sendServiceManagementReportForTenant(tenant);
                log.info("Sent service management report for tenant {}", tenant.getId());
            } catch (Exception e) {
                log.error("Failed to send service management report for tenant {}", tenant.getId(), e);
            }
        }, tenant -> StringUtils.isNotBlank(tenant.getEmail()));
    }

    private void sendServiceManagementReportForTenant(Tenant tenant) throws ThingsboardException {
        Map<String, String> reportInfo = getServiceManagementReportInfo(tenant);
        defaultMailService.sendMagentaReportEmail(tenant.getId(), null, tenant.getEmail(), reportInfo,
                reportType, MailTemplates.SERVICE_MANAGEMENT_REPORT, getReportName(tenant));
    }

    @SneakyThrows
    private Map<String, String> getServiceManagementReportInfo(Tenant tenant) {
        Date periodStart = toDate(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay());
        Date periodEnd = toDate(LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59));

        List<KpiKey> kpiKeys = Arrays.stream(KpiKey.values()).filter(kpiKey -> kpiKey.getApiUsageRecordKey() != null).collect(Collectors.toList());
        Map<String, Long> kpisValues = getKpisValues(tenant.getId(), kpiKeys.stream().filter(Predicate.not(KpiKey::isSystemMetric)).collect(Collectors.toList()), periodStart, periodEnd);
        kpisValues.putAll(getKpisValues(TenantId.SYS_TENANT_ID, kpiKeys.stream().filter(KpiKey::isSystemMetric).collect(Collectors.toList()), periodStart, periodEnd));

        Map<KpiKey, Long> kpisInfo = new EnumMap<>(KpiKey.class);
        kpiKeys.forEach(kpiKey -> {
            kpisInfo.put(kpiKey, kpisValues.getOrDefault(kpiKey.getApiUsageRecordKey().getApiCountKey(), kpiKey.getDefaultValue()));
        });

        kpisInfo.put(KpiKey.SUCCESSFUL_DOWNLINK_MESSAGES, KpiKey.SUCCESSFUL_DOWNLINK_MESSAGES.compute(kpisInfo::get));
        kpisInfo.put(KpiKey.NEW_PROVISIONED_DEVICES, deviceService.countByTenantIdAndCreatedTimeBetween(tenant.getId(), periodStart.getTime(), periodEnd.getTime()));

        Map<String, String> result = kpisInfo.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString()));

        result.put("PERIOD_START_DATE", REPORT_DATE_FORMAT.format(periodStart));
        result.put("PERIOD_END_DATE", REPORT_DATE_FORMAT.format(periodEnd));
        result.put("TENANT_NAME", tenant.getName());

        return result;
    }

    private Map<String, Long> getKpisValues(TenantId tenantId, List<KpiKey> kpiKeys, Date periodStart, Date periodEnd) throws ExecutionException, InterruptedException {
        return tsService.getLongTsValuesForKeysAndPeriod(tenantId, apiUsageStateService.findTenantApiUsageState(tenantId).getId(),
                kpiKeys.stream()
                        .map(KpiKey::getApiUsageRecordKey)
                        .map(ApiUsageRecordKey::getApiCountKey)
                        .collect(Collectors.toList()),
                periodStart.getTime(), periodEnd.getTime());
    }

    private String getReportName(Tenant tenant) {
        LocalDate time = LocalDate.now();
        return String.format("service_management_report_%s_%s.%s",
                StringUtils.leftPad(time.getMonthValue() + "" + time.getYear(), 6, "0"),
                tenant.getId(), reportType.name().toLowerCase());
    }

}
