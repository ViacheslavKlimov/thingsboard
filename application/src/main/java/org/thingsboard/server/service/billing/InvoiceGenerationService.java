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
package org.thingsboard.server.service.billing;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.billing.UsageDataKey;
import org.thingsboard.server.common.data.billing.UsageDataValue;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration.SubscriptionPlanInfo;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.integration.IntegrationDao;
import org.thingsboard.server.dao.rule.RuleChainDao;
import org.thingsboard.server.dao.scheduler.SchedulerEventDao;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.billing.Invoice.InvoiceHeader;
import org.thingsboard.server.service.billing.Invoice.InvoiceRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.utils.TimeUtils.toDate;

@Slf4j
@Service
@RequiredArgsConstructor
@TbCoreComponent
public class InvoiceGenerationService {
    private final TbTenantProfileCache tenantProfileCache;
    private final TimeseriesService tsService;
    private final ApiUsageStateService apiUsageStateService;
    private final WhiteLabelingService whiteLabelingService;

    private final DeviceDao deviceDao;
    private final AssetDao assetDao;
    private final UserDao userDao;
    private final DashboardDao dashboardDao;
    private final RuleChainDao ruleChainDao;
    private final IntegrationDao integrationDao;
    private final ConverterDao converterDao;
    private final SchedulerEventDao schedulerEventDao;

    private static final int MONTHLY_RECURRING_CHARGES_SUBDOC_ID = 2;

    public Invoice generateInvoiceForTenant(Tenant tenant) {
        TenantProfile tenantProfile = tenantProfileCache.get(tenant.getId());
        DefaultTenantProfileConfiguration tenantProfileConfiguration = tenantProfile.getProfileConfiguration()
                .orElseThrow(() -> new IllegalStateException("Tenant profile is not configured"));
        SubscriptionPlanInfo subscriptionPlanInfo = Optional.ofNullable(tenantProfileConfiguration.getSubscriptionPlanInfo())
                .orElseThrow(() -> new IllegalStateException("Subscription plan info not set"));

        Invoice invoice = new Invoice();

        Date billingPeriodStart = toDate(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay());
        Date billingPeriodEnd = toDate(LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59));

        setInvoiceHeader(invoice, tenant, billingPeriodStart, billingPeriodEnd);
        calculateMonthlyRecurringCharges(invoice, tenant);
        calculateUsageCharges(invoice, tenant.getId(), tenantProfileConfiguration, subscriptionPlanInfo);

        return invoice;
    }

    private void setInvoiceHeader(Invoice invoice, Tenant tenant, Date billingPeriodStart, Date billingPeriodEnd) {
        invoice.setHeader(InvoiceHeader.builder()
                .platformId("0THB")
                .partnerId(tenant.getId().toString())
                .invoiceNumber("INV" + getInvoiceNumber(tenant.getId()))
                .creationDate(new Date())
                .billingPeriodStart(billingPeriodStart)
                .billingPeriodEnd(billingPeriodEnd)
                .debtor(tenant.getMagentaCustomerId())
                .customerReference(tenant.getCustomerReference())
                .currency("EUR")
                .build());
    }

    private void calculateMonthlyRecurringCharges(Invoice invoice, Tenant tenant) {
        Map<Long, TenantProfileId> tenantProfileChangeHistory = Optional.ofNullable(tenant.getAdditionalInfo().get("tenantProfileChangeHistory"))
                .map(jsonNode -> JacksonUtil.convertValue(jsonNode, new TypeReference<Map<String, String>>() {}))
                .orElse(Collections.emptyMap())
                .entrySet().stream()
                .collect(Collectors.toMap(entry -> Long.parseLong(entry.getKey()), entry -> new TenantProfileId(UUID.fromString(entry.getValue()))));

        long billingPeriodStartTime = invoice.getHeader().getBillingPeriodStart().getTime();
        long billingPeriodEndTime = invoice.getHeader().getBillingPeriodEnd().getTime();
        long billingPeriodDuration = billingPeriodEndTime - billingPeriodStartTime;

        Map<TenantProfileId, Long> usedSubscriptionPlans = new LinkedHashMap<>();

        long subscriptionPlanEndTime = billingPeriodEndTime;
        for (Long tenantProfileChangeTime : tenantProfileChangeHistory.keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
            TenantProfileId tenantProfileId = tenantProfileChangeHistory.get(tenantProfileChangeTime);
            if (tenantProfileChangeTime > billingPeriodStartTime) {
                usedSubscriptionPlans.put(tenantProfileId, usedSubscriptionPlans.getOrDefault(tenantProfileId, 0L) + (subscriptionPlanEndTime - tenantProfileChangeTime));
                subscriptionPlanEndTime = tenantProfileChangeTime;
            } else {
                usedSubscriptionPlans.put(tenantProfileId, usedSubscriptionPlans.getOrDefault(tenantProfileId, 0L) + (subscriptionPlanEndTime - billingPeriodStartTime));
                break;
            }
        }

        if (usedSubscriptionPlans.isEmpty()) {
            usedSubscriptionPlans.put(tenant.getTenantProfileId(), Math.min(billingPeriodEndTime - tenant.getCreatedTime(), billingPeriodDuration));
        }

        List<InvoiceRecord> subscriptionPlansRecords = new LinkedList<>();
        List<InvoiceRecord> whiteLabelingUsageRecords = new LinkedList<>();

        usedSubscriptionPlans.forEach((tenantProfileId, subscriptionPlanDuration) -> {
            TenantProfile tenantProfile = tenantProfileCache.get(tenantProfileId);

            if (tenantProfile == null) {
                return;
            }
            SubscriptionPlanInfo subscriptionPlanInfo = tenantProfile.getProfileConfiguration()
                    .map(DefaultTenantProfileConfiguration::getSubscriptionPlanInfo)
                    .orElse(null);
            if (subscriptionPlanInfo == null) {
                return;
            }

            InvoiceRecord subscriptionPlanRecord = new InvoiceRecord();
            subscriptionPlanRecord.setId(MONTHLY_RECURRING_CHARGES_SUBDOC_ID);
            subscriptionPlanRecord.setType("Monthly Recurring Charges");
            subscriptionPlanRecord.setMaterialNumber(subscriptionPlanInfo.getMaterialNumber());
            subscriptionPlanRecord.setDescription("Monthly recurring charges " + tenantProfile.getName());
            subscriptionPlanRecord.setChargedUnit(1);
            subscriptionPlanRecord.setPricePerUnit(subscriptionPlanInfo.getPrice());
            subscriptionPlanRecord.setUnitsAmount((double) subscriptionPlanDuration / billingPeriodDuration);
            subscriptionPlanRecord.setTotalPrice(subscriptionPlanRecord.getUnitsAmount() * subscriptionPlanRecord.getPricePerUnit());
            subscriptionPlansRecords.add(subscriptionPlanRecord);

            if (whiteLabelingService.isWhiteLabelingAllowed(tenant.getId(), tenant.getId())) {
                InvoiceRecord whiteLabelingUsageRecord = new InvoiceRecord();
                whiteLabelingUsageRecord.setId(MONTHLY_RECURRING_CHARGES_SUBDOC_ID);
                whiteLabelingUsageRecord.setType("Monthly Recurring Charges");
                whiteLabelingUsageRecord.setMaterialNumber(UsageDataKey.WHITE_LABELING.getMaterialNumber());
                whiteLabelingUsageRecord.setDescription(UsageDataKey.WHITE_LABELING.getDescription());
                whiteLabelingUsageRecord.setChargedUnit(1);
                whiteLabelingUsageRecord.setPricePerUnit(Optional.ofNullable(subscriptionPlanInfo.getPerUnitPrices())
                        .flatMap(perUnitPrices -> perUnitPrices.getPriceForFeature(UsageDataKey.WHITE_LABELING))
                        .orElse(0.0));
                whiteLabelingUsageRecord.setUnitsAmount((double) subscriptionPlanDuration / billingPeriodDuration);
                whiteLabelingUsageRecord.setTotalPrice(whiteLabelingUsageRecord.getUnitsAmount() * whiteLabelingUsageRecord.getPricePerUnit());
                whiteLabelingUsageRecords.add(whiteLabelingUsageRecord);
            }
        });

        invoice.getRecords().addAll(subscriptionPlansRecords);
        invoice.getRecords().addAll(whiteLabelingUsageRecords);
    }

    private void calculateUsageCharges(Invoice invoice, TenantId tenantId, DefaultTenantProfileConfiguration tenantProfileConfiguration, SubscriptionPlanInfo subscriptionPlanInfo) {
        Map<UsageDataKey, UsageDataValue> usageInfo = getUsageInfo(tenantId, tenantProfileConfiguration, invoice.getHeader().getBillingPeriodStart(), invoice.getHeader().getBillingPeriodEnd());
        for (Map.Entry<UsageDataKey, UsageDataValue> usageDataEntry : usageInfo.entrySet()) {
            UsageDataKey usageDataKey = usageDataEntry.getKey();
            UsageDataValue usageDataValue = usageDataEntry.getValue();
            if (usageDataValue.getUsed() == 0) continue;

            InvoiceRecord record = new InvoiceRecord();
            record.setId(usageDataKey.getSubdocId());
            record.setType(usageDataKey.getInvoiceType());
            record.setMaterialNumber(usageDataKey.getMaterialNumber());
            record.setDescription(usageDataKey.getDescription());
            record.setChargedUnit(usageDataKey.getChargedUnit());
            record.setPricePerUnit(Optional.ofNullable(subscriptionPlanInfo.getPerUnitPrices())
                    .flatMap(perUnitPrices -> perUnitPrices.getPriceForFeature(usageDataKey))
                    .orElse(0.0));
            record.setUnitsAmount(usageDataValue.getUsed().doubleValue());
            record.setTotalPrice(((double) record.getUnitsAmount() / record.getChargedUnit()) * record.getPricePerUnit());
            if (record.getTotalPrice() != 0.0) {
                invoice.getRecords().add(record);
            }
        }
    }

    @SneakyThrows
    private Map<UsageDataKey, UsageDataValue> getUsageInfo(TenantId tenantId, DefaultTenantProfileConfiguration tenantProfileConfiguration, Date billingPeriodStart, Date billingPeriodEnd) {
        Map<UsageDataKey, UsageDataValue> usageInfo = new LinkedHashMap<>();

        usageInfo.put(UsageDataKey.DEVICES, new UsageDataValue(deviceDao.countByTenantId(tenantId), tenantProfileConfiguration.getMaxDevices()));
        usageInfo.put(UsageDataKey.ASSETS, new UsageDataValue(assetDao.countByTenantId(tenantId), tenantProfileConfiguration.getMaxAssets()));
        usageInfo.put(UsageDataKey.USERS, new UsageDataValue(userDao.countByTenantId(tenantId), tenantProfileConfiguration.getMaxUsers()));
        usageInfo.put(UsageDataKey.DASHBOARDS, new UsageDataValue(dashboardDao.countByTenantId(tenantId), tenantProfileConfiguration.getMaxDashboards()));
        usageInfo.put(UsageDataKey.RULE_CHAINS, new UsageDataValue(ruleChainDao.countByTenantId(tenantId), tenantProfileConfiguration.getMaxRuleChains()));
        usageInfo.put(UsageDataKey.INTEGRATIONS, new UsageDataValue(integrationDao.countByTenantId(tenantId), tenantProfileConfiguration.getMaxIntegrations()));
        usageInfo.put(UsageDataKey.CONVERTERS, new UsageDataValue(converterDao.countByTenantId(tenantId), tenantProfileConfiguration.getMaxConverters()));
        usageInfo.put(UsageDataKey.SCHEDULER_EVENTS, new UsageDataValue(schedulerEventDao.countByTenantId(tenantId), tenantProfileConfiguration.getMaxSchedulerEvents()));

        List<UsageDataKey> apiUsageDataKeys = Arrays.stream(UsageDataKey.values())
                .filter(usageDataKey -> usageDataKey.getApiUsageRecordKey() != null)
                .collect(Collectors.toList());
        Map<String, Long> apiUsageStats = tsService.getLongTsValuesForKeysAndPeriod(tenantId, apiUsageStateService.findTenantApiUsageState(tenantId).getId(), apiUsageDataKeys.stream()
                .map(UsageDataKey::getApiUsageRecordKey)
                .map(ApiUsageRecordKey::getApiCountKey)
                .collect(Collectors.toList()), billingPeriodStart.getTime(), billingPeriodEnd.getTime());
        apiUsageDataKeys.forEach(usageDataKey -> {
            usageInfo.put(usageDataKey, new UsageDataValue(
                    apiUsageStats.getOrDefault(usageDataKey.getApiUsageRecordKey().getApiCountKey(), 0L),
                    tenantProfileConfiguration.getProfileThreshold(usageDataKey.getApiUsageRecordKey())
            ));
        });

        return usageInfo;
    }

    public static String getInvoiceNumber(TenantId id) {
        int number = Math.abs((int) (id.getId().hashCode() % 1e6));
        return StringUtils.leftPad(String.valueOf(number), 6, '0') + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

}
