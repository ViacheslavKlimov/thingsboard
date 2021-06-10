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

    API_CALLS(3_1),
//    API_CALLS_SUCCESS_RATE(3_2),

//    UPLINK_MESSAGES(4_1), // transport messages
//    UPLINK_MESSAGES_SUCCESS_RATE(4_2),
//    DOWNLINK_MESSAGES(5_1),
//    DOWNLINK_MESSAGES_SUCCESS_RATE(5_2),

    SUCCESSFUL_DOWNLINK_MESSAGES(6_1),
    UNSUCCESSFUL_DOWNLINK_MESSAGES(6_3),

    RPC(10),

//    RULE_ENGINE_SUCCESSFUL_EXECUTIONS(10_1, ApiUsageRecordKey.RE_EXEC_COUNT),
//    RULE_ENGINE_ERROR_EXECUTIONS(10_2),

//    CREATED_ALARMS(11_1)
    ;

    private final int id;
    private final ApiUsageRecordKey apiUsageRecordKey;

    KpiKey(int id) {
        this(id, null);
    }

    KpiKey(int id, ApiUsageRecordKey apiUsageRecordKey) {
        this.id = id;
        this.apiUsageRecordKey = apiUsageRecordKey;
    }

    public String composeOid(String baseOid) {
        return String.format("%s.%s", baseOid, getId());
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
