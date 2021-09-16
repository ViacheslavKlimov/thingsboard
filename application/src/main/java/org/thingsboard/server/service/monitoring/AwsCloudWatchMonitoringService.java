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
package org.thingsboard.server.service.monitoring;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@ConditionalOnProperty(name = "monitoring.aws.enabled", havingValue = "true")
public class AwsCloudWatchMonitoringService {
    @Value("${monitoring.aws.cloudwatch.iam_user.access_key_id}")
    private String iamUserAccessKeyId;
    @Value("${monitoring.aws.cloudwatch.iam_user.secret_access_key}")
    private String iamUserSecretAccessKey;
    @Value("${monitoring.aws.cloudwatch.region}")
    private String awsRegion;

    @Value("${monitoring.aws.vpn.tma}")
    private String tmaVpnId;
    @Value("${monitoring.aws.vpn.wbc}")
    private String wbcVpnId;
    @Value("${monitoring.aws.vpn.ericsson_amsterdam}")
    private String ericssonAmsterdamVpnId;
    @Value("${monitoring.aws.vpn.ericsson_stockholm}")
    private String ericssonStockholmVpnId;

    private final TbApiUsageReportClient apiUsageReportClient;
    private final PartitionService partitionService;

    private AmazonCloudWatch cloudWatchClient;
    private Map<String, Pair<ApiUsageRecordKey, ApiUsageRecordKey>> monitoredVpnsInfo;

    private static final long METRICS_QUERYING_PERIOD_MS = 5 * 60 * 1000;

    @PostConstruct
    public void init() {
        this.monitoredVpnsInfo = Map.of(
                tmaVpnId, Pair.of(ApiUsageRecordKey.TMA_VPN_DATA_IN, ApiUsageRecordKey.TMA_VPN_DATA_OUT),
                wbcVpnId, Pair.of(ApiUsageRecordKey.WBC_VPN_DATA_IN, ApiUsageRecordKey.WBC_VPN_DATA_OUT),
                ericssonAmsterdamVpnId, Pair.of(ApiUsageRecordKey.ERICSSON_AMSTERDAM_VPN_DATA_IN, ApiUsageRecordKey.ERICSSON_AMSTERDAM_VPN_DATA_OUT),
                ericssonStockholmVpnId, Pair.of(ApiUsageRecordKey.ERICSSON_STOCKHOLM_VPN_DATA_IN, ApiUsageRecordKey.ERICSSON_STOCKHOLM_VPN_DATA_OUT)
        );
        this.cloudWatchClient = AmazonCloudWatchClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(iamUserAccessKeyId, iamUserSecretAccessKey)))
                .withRegion(awsRegion)
                .build();
    }

    @Scheduled(initialDelay = METRICS_QUERYING_PERIOD_MS, fixedRate = METRICS_QUERYING_PERIOD_MS)
    private void reportVpnsTraffic() {
        if (partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition()) {
            Date startTime = new Date(System.currentTimeMillis() - METRICS_QUERYING_PERIOD_MS);
            Date endTime = new Date();

            monitoredVpnsInfo.forEach((vpnId, usageKeys) -> {
                reportVpnTraffic(vpnId, usageKeys, startTime, endTime);
            });
        }
    }

    private void reportVpnTraffic(String vpnId, Pair<ApiUsageRecordKey, ApiUsageRecordKey> usageKeys, Date startTime, Date endTime) {
        GetMetricStatisticsRequest vpnTrafficMetricRequest = new GetMetricStatisticsRequest()
                .withNamespace("AWS/VPN")
                .withMetricName("TunnelDataIn")
                .withDimensions(Collections.singletonList(new Dimension().withName("VpnId").withValue(vpnId)))
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withPeriod((int) (METRICS_QUERYING_PERIOD_MS / 1000))
                .withStatistics("Sum");

        long vpnDataIn = getTotalSum(cloudWatchClient.getMetricStatistics(vpnTrafficMetricRequest).getDatapoints());
        apiUsageReportClient.report(TenantId.SYS_TENANT_ID, null, usageKeys.getFirst(), vpnDataIn);

        vpnTrafficMetricRequest.setMetricName("TunnelDataOut");

        long vpnDataOut = getTotalSum(cloudWatchClient.getMetricStatistics(vpnTrafficMetricRequest).getDatapoints());
        apiUsageReportClient.report(TenantId.SYS_TENANT_ID, null, usageKeys.getSecond(), vpnDataOut);
    }

    private long getTotalSum(Collection<Datapoint> datapoints) {
        double total = 0;
        for (Datapoint datapoint : datapoints) {
            total += datapoint.getSum();
        }
        return (long) total;
    }

}
