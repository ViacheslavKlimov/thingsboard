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

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public enum AlarmCategory {
    COMMUNICATION(1, Set.of(
            "NodeNetworkReceiveErrs",
            "NodeNetworkTransmitErrs",
            "NodeNetworkInterfaceFlapping",
            "NodeHighNumberConntrackEntriesUsed",
            "PrometheusNotConnectedToAlertmanagers",
            "PrometheusRemoteStorageFailures",
            "PrometheusRemoteWriteBehind",
            "PrometheusErrorSendingAlertsToAnyAlertmanager"
    )),
    QUALITY_OF_SERVICE(2, Set.of(
            "KubePodCrashLooping",
            "KubePodNotReady",
            "KubeDeploymentGenerationMismatch",
            "KubeDeploymentReplicasMismatch",
            "KubeStatefulSetReplicasMismatch",
            "KubeStatefulSetGenerationMismatch",
            "KubeStatefulSetUpdateNotRolledOut",
            "KubeDaemonSetRolloutStuck",
            "KubeContainerWaiting",
            "KubeDaemonSetNotScheduled",
            "KubeDaemonSetMisScheduled",
            "KubeNodeNotReady",
            "KubeNodeUnreachable",
            "KubeNodeReadinessFlapping",
            "KubeletPlegDurationHigh",
            "KubeletPodStartUpLatencyHigh"
    )),
    PROCESSING(3, Set.of(
            "KubeJobCompletion",
            "KubeJobFailed",
            "KubeQuotaAlmostFull",
            "KubeQuotaFullyUsed",
            "KubeQuotaExceeded",
//            "PrometheusBadConfig",
            "PrometheusNotificationQueueRunningFull",
            "PrometheusErrorSendingAlertsToSomeAlertmanagers",
            "PrometheusNotIngestingSamples",
            "PrometheusDuplicateTimestamps",
            "PrometheusOutOfOrderTimestamps",
            "PrometheusRuleFailures",
            "PrometheusMissingRuleEvaluations",
            "PrometheusTargetLimitHit",
            "PrometheusLabelLimitHit",
            "PrometheusTargetSyncFailure",
            "NodeTextFileCollectorScrapeError"
    )),
    EQUIPMENT(4, Set.of(
            "NodeFilesystemSpaceFillingUp",
            "NodeFilesystemAlmostOutOfSpace",
            "NodeFilesystemFilesFillingUp",
            "NodeFilesystemAlmostOutOfFiles",
            "NodeRAIDDegraded",
            "NodeRAIDDiskFailure",
            "PrometheusTSDBReloadsFailing",
            "PrometheusTSDBCompactionsFailing"
    )),
    ENVIRONMENTAL(5, Set.of(
            "PodCPUQuotaOverflow",
            "PodMemoryQuotaOverflow",
            "KubePersistentVolumeFillingUp",
            "KubePersistentVolumeErrors",
            "KubeCPUOvercommit",
            "CPUThrottlingHigh",
            "KubeMemoryOvercommit",
            "KubeCPUQuotaOvercommit",
            "KubeMemoryQuotaOvercommit",
            "KubeletTooManyPods",
            "KubeletDown",
            "PrometheusRemoteWriteDesiredShards",
            "NodeClockSkewDetected",
            "NodeClockNotSynchronising"
    ));

    private final int id;
    private final Set<String> alerts;

    AlarmCategory(int id, Set<String> alerts) {
        this.id = id;
        this.alerts = alerts;
    }

    public int getId() {
        return id;
    }

    public static Optional<AlarmCategory> forAlert(String alertName) {
        return Arrays.stream(values())
                .filter(category -> category.alerts.contains(alertName))
                .findFirst();
    }

}
