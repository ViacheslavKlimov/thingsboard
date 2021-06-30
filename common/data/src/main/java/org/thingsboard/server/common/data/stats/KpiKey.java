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
    FAILED_API_CALLS(ApiUsageRecordKey.FAILED_REST_API_CALLS_COUNT),
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
    SUCCESSFUL_DOWNLINK_MESSAGES(5_2),
    FAILED_DOWNLINK_MESSAGES(ApiUsageRecordKey.FAILED_DOWNLINK_MSG_COUNT), // no ack in 3 minutes
    DOWNLINK_MESSAGES_SUCCESS_RATE(5_4),

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

    RULE_ENGINE_EXECUTIONS(7_1, ApiUsageRecordKey.RE_EXEC_COUNT),
    FAILED_RULE_ENGINE_EXECUTIONS(7_2, ApiUsageRecordKey.FAILED_RE_EXEC_COUNT),

    CREATED_ALARMS(8_1, ApiUsageRecordKey.CREATED_ALARMS_COUNT),

//    WBC_TRAFFIC,
//    E111_TRAFFIC,
//    TMA_TRAFFIC
    ;

    private Integer id;
    private ApiUsageRecordKey apiUsageRecordKey;
    private Long defaultValue = 0L;
    private boolean isAccumulated = true;
    private boolean isComputed = false;

    KpiKey(Integer id) {
        this.id = id;
    }

    KpiKey(ApiUsageRecordKey apiUsageRecordKey) {
        this.apiUsageRecordKey = apiUsageRecordKey;
    }

    KpiKey(Integer id, ApiUsageRecordKey apiUsageRecordKey) {
        this.id = id;
        this.apiUsageRecordKey = apiUsageRecordKey;
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
