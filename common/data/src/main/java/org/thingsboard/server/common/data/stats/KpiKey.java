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
package org.thingsboard.server.common.data.stats;

import lombok.Getter;
import org.thingsboard.server.common.data.ApiUsageRecordKey;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

@Getter
public enum KpiKey {
    TOTAL_DEVICES(1_1, false),
    ONLINE_DEVICES(1_2, false),
    OFFLINE_DEVICES(1_3, false),

    NEW_PROVISIONED_DEVICES(2_1),

    API_CALLS(3_1, ApiUsageRecordKey.REST_API_CALLS_COUNT),
    FAILED_API_CALLS(3_2, ApiUsageRecordKey.FAILED_REST_API_CALLS_COUNT),
    API_CALLS_SUCCESS_RATE(3_3, 100L, true) {
        @Override
        public long compute(Function<KpiKey, Long> kpiValueProvider) {
            return calculateSuccessRate(kpiValueProvider.apply(API_CALLS), kpiValueProvider.apply(FAILED_API_CALLS));
        }
    },

    UPLINK_MESSAGES(4_1, ApiUsageRecordKey.UPLINK_MSG_COUNT), // messages to core from devices
    FAILED_UPLINK_MESSAGES(4_2, ApiUsageRecordKey.FAILED_UPLINK_MSG_COUNT),
    UPLINK_MESSAGES_SUCCESS_RATE(4_3, 100L, true) {
        @Override
        public long compute(Function<KpiKey, Long> kpiValueProvider) {
            return calculateSuccessRate(kpiValueProvider.apply(UPLINK_MESSAGES), kpiValueProvider.apply(FAILED_UPLINK_MESSAGES));
        }
    },

    DOWNLINK_MESSAGES(5_1, ApiUsageRecordKey.DOWNLINK_MSG_COUNT), // responses from core to devices (server rpc, etc.)
    SUCCESSFUL_DOWNLINK_MESSAGES(5_2, 0L, true) {
        @Override
        public long compute(Function<KpiKey, Long> kpiValueProvider) {
            return kpiValueProvider.apply(DOWNLINK_MESSAGES) - kpiValueProvider.apply(FAILED_DOWNLINK_MESSAGES);
        }
    },
    FAILED_DOWNLINK_MESSAGES(5_3, ApiUsageRecordKey.FAILED_DOWNLINK_MSG_COUNT), // no ack in 3 minutes
    DOWNLINK_MESSAGES_SUCCESS_RATE(5_4, 100L, true) {
        @Override
        public long compute(Function<KpiKey, Long> kpiValueProvider) {
            return calculateSuccessRate(kpiValueProvider.apply(DOWNLINK_MESSAGES), kpiValueProvider.apply(FAILED_DOWNLINK_MESSAGES));
        }
    },

    ONE_WAY_RPC_REQUESTS(6_1, ApiUsageRecordKey.ONE_WAY_RPC_REQUEST_COUNT),
    FAILED_ONE_WAY_RPC_REQUESTS(6_2, ApiUsageRecordKey.FAILED_ONE_WAY_RPC_REQUEST_COUNT),
    ONE_WAY_RPC_REQUESTS_SUCCESS_RATE(6_3, 100L, true) {
        @Override
        public long compute(Function<KpiKey, Long> kpiValueProvider) {
            return calculateSuccessRate(kpiValueProvider.apply(ONE_WAY_RPC_REQUESTS), kpiValueProvider.apply(FAILED_ONE_WAY_RPC_REQUESTS));
        }
    },
    TWO_WAY_RPC_REQUESTS(6_4, ApiUsageRecordKey.TWO_WAY_RPC_REQUEST_COUNT),
    FAILED_TWO_WAY_RPC_REQUESTS(6_5, ApiUsageRecordKey.FAILED_TWO_WAY_RPC_REQUEST_COUNT),
    TWO_WAY_RPC_REQUESTS_SUCCESS_RATE(6_6, 100L, true) {
        @Override
        public long compute(Function<KpiKey, Long> kpiValueProvider) {
            return calculateSuccessRate(kpiValueProvider.apply(TWO_WAY_RPC_REQUESTS), kpiValueProvider.apply(FAILED_TWO_WAY_RPC_REQUESTS));
        }
    },
    QUEUED_PERSISTENT_RPC_REQUESTS(6_7, ApiUsageRecordKey.QUEUED_PERSISTENT_RPC_REQUEST_COUNT),
    DELIVERED_PERSISTENT_RPC_REQUESTS(6_8, ApiUsageRecordKey.DELIVERED_PERSISTENT_RPC_REQUEST_COUNT),
    SUCCESSFUL_PERSISTENT_RPC_REQUESTS(6_9, ApiUsageRecordKey.SUCCESSFUL_PERSISTENT_RPC_REQUEST_COUNT),
    TIMED_OUT_PERSISTENT_RPC_REQUESTS(6_10, ApiUsageRecordKey.TIMED_OUT_PERSISTENT_RPC_REQUEST_COUNT),
    FAILED_PERSISTENT_RPC_REQUESTS(6_11, ApiUsageRecordKey.FAILED_PERSISTENT_RPC_REQUEST_COUNT),

    RULE_ENGINE_EXECUTIONS(7_1, ApiUsageRecordKey.RE_EXEC_COUNT),
    FAILED_RULE_ENGINE_EXECUTIONS(7_2, ApiUsageRecordKey.FAILED_RE_EXEC_COUNT),

    CREATED_ALARMS(8_1, ApiUsageRecordKey.CREATED_ALARMS_COUNT),

    TMA_VPN_DATA_IN(9_1, ApiUsageRecordKey.TMA_VPN_DATA_IN, true),
    TMA_VPN_DATA_OUT(9_2, ApiUsageRecordKey.TMA_VPN_DATA_OUT, true),
    WBC_VPN_DATA_IN(9_3, ApiUsageRecordKey.WBC_VPN_DATA_IN, true),
    WBC_VPN_DATA_OUT(9_4, ApiUsageRecordKey.WBC_VPN_DATA_OUT, true),
    ERICSSON_AMSTERDAM_VPN_DATA_IN(9_5, ApiUsageRecordKey.ERICSSON_AMSTERDAM_VPN_DATA_IN, true),
    ERICSSON_AMSTERDAM_VPN_DATA_OUT(9_6, ApiUsageRecordKey.ERICSSON_AMSTERDAM_VPN_DATA_OUT, true),
    ERICSSON_STOCKHOLM_VPN_DATA_IN(9_7, ApiUsageRecordKey.ERICSSON_STOCKHOLM_VPN_DATA_IN, true),
    ERICSSON_STOCKHOLM_VPN_DATA_OUT(9_8, ApiUsageRecordKey.ERICSSON_STOCKHOLM_VPN_DATA_OUT, true),

    COMMUNICATION_SYSTEM_ALARMS(ApiUsageRecordKey.COMMUNICATION_SYSTEM_ALARMS_COUNT, true),
    QUALITY_OF_SERVICE_SYSTEM_ALARMS(ApiUsageRecordKey.QUALITY_OF_SERVICE_SYSTEM_ALARMS_COUNT, true),
    PROCESSING_SYSTEM_ALARMS(ApiUsageRecordKey.PROCESSING_SYSTEM_ALARMS_COUNT, true),
    EQUIPMENT_SYSTEM_ALARMS(ApiUsageRecordKey.EQUIPMENT_SYSTEM_ALARMS_COUNT, true),
    ENVIRONMENTAL_SYSTEM_ALARMS(ApiUsageRecordKey.ENVIRONMENTAL_SYSTEM_ALARMS_COUNT, true);

    private Integer id;
    private ApiUsageRecordKey apiUsageRecordKey;
    private Long defaultValue = 0L;
    private boolean isAccumulated = true;
    private boolean isComputed = false;
    private boolean isSystemMetric = false;

    KpiKey(Integer id) {
        this.id = id;
    }

    KpiKey(Integer id, ApiUsageRecordKey apiUsageRecordKey) {
        this.id = id;
        this.apiUsageRecordKey = apiUsageRecordKey;
    }

    KpiKey(Integer id, ApiUsageRecordKey apiUsageRecordKey, boolean isSystemMetric) {
        this.id = id;
        this.apiUsageRecordKey = apiUsageRecordKey;
        this.isSystemMetric = isSystemMetric;
    }

    KpiKey(Integer id, Long defaultValue, boolean isComputed) {
        this.id = id;
        this.defaultValue = defaultValue;
        this.isComputed = isComputed;
    }

    KpiKey(Integer id, boolean isAccumulated) {
        this.id = id;
        this.isAccumulated = isAccumulated;
    }

    KpiKey(ApiUsageRecordKey apiUsageRecordKey, boolean isSystemMetric) {
        this.apiUsageRecordKey = apiUsageRecordKey;
        this.isSystemMetric = isSystemMetric;
    }

    public static Optional<KpiKey> forApiUsageRecordKey(ApiUsageRecordKey apiUsageRecordKey) {
        return Arrays.stream(values())
                .filter(kpiKey -> apiUsageRecordKey == kpiKey.getApiUsageRecordKey())
                .findFirst();
    }

    public static Optional<KpiKey> forApiUsageRecordKey(String apiUsageRecordKeyName) {
        return forApiUsageRecordKey(ApiUsageRecordKey.valueOf(apiUsageRecordKeyName));
    }

    public long compute(Function<KpiKey, Long> kpiValueProvider) {
        return 0L;
    }

    protected long calculateSuccessRate(long all, long failed) {
        if (all > 0) {
            return (long) (((double) (all - failed) / all) * 100);
        } else {
            return getDefaultValue();
        }
    }

}
