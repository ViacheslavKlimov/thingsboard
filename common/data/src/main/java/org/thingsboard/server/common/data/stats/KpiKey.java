package org.thingsboard.server.common.data.stats;

import lombok.Getter;
import org.thingsboard.server.common.data.ApiUsageRecordKey;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum KpiKey {
    TOTAL_DEVICES(1_1),
    ONLINE_DEVICES(1_2),
    OFFLINE_DEVICES(1_3),

    NEW_PROVISIONED_DEVICES(2_1),

    API_CALLS(3_1, ApiUsageRecordKey.REST_API_CALLS_COUNT), // yet counted on system level
    FAILED_API_CALLS(3_2, ApiUsageRecordKey.FAILED_REST_API_CALLS_COUNT),
    API_CALLS_SUCCESS_RATE(3_3, 100L),

    UPLINK_MESSAGES(4_1, ApiUsageRecordKey.UPLINK_MSG_COUNT), // messages to core from devices
    FAILED_UPLINK_MESSAGES(4_2, ApiUsageRecordKey.FAILED_UPLINK_MSG_COUNT),
    UPLINK_MESSAGES_SUCCESS_RATE(4_3, 100L),

    DOWNLINK_MESSAGES(5_1, ApiUsageRecordKey.DOWNLINK_MSG_COUNT), // responses from core to devices (server rpc, etc.)
    SUCCESSFUL_DOWNLINK_MESSAGES(5_2),
    FAILED_DOWNLINK_MESSAGES(ApiUsageRecordKey.FAILED_DOWNLINK_MSG_COUNT), // no ack in 3 minutes
    DOWNLINK_MESSAGES_SUCCESS_RATE(5_4),

//    RPC(10),

    RULE_ENGINE_EXECUTIONS(10_1, ApiUsageRecordKey.RE_EXEC_COUNT),
    FAILED_RULE_ENGINE_EXECUTIONS(10_2, ApiUsageRecordKey.FAILED_RE_EXEC_COUNT),

    CREATED_ALARMS(11_1, ApiUsageRecordKey.CREATED_ALARMS_COUNT),

//    WBC_TRAFFIC,
//    E111_TRAFFIC,
//    TMA_TRAFFIC
    ;

    private Integer id;
    private ApiUsageRecordKey apiUsageRecordKey;
    private Long defaultValue = 0L;

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

    KpiKey(Integer id, Long defaultValue) {
        this.id = id;
        this.defaultValue = defaultValue;
    }

    public static Optional<KpiKey> forApiUsageRecordKey(ApiUsageRecordKey apiUsageRecordKey) {
        return Arrays.stream(values())
                .filter(kpiKey -> apiUsageRecordKey == kpiKey.getApiUsageRecordKey())
                .findFirst();
    }

    public static Optional<KpiKey> forApiUsageRecordKey(String apiUsageRecordKeyName) {
        return forApiUsageRecordKey(ApiUsageRecordKey.valueOf(apiUsageRecordKeyName));
    }

}
