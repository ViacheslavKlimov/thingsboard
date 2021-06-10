package org.thingsboard.server.common.data.stats;

import lombok.Getter;
import org.thingsboard.server.common.data.ApiUsageRecordKey;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum KpiKey {
    TOTAL_DEVICES(2, true),
    ONLINE_DEVICES(4, true),
    OFFLINE_DEVICES(6, true),

    NEW_PROVISIONED_DEVICES(8),

    RULE_ENGINE_EXECUTIONS(10, ApiUsageRecordKey.RE_EXEC_COUNT),
    TRANSPORT_MESSAGES(12, ApiUsageRecordKey.TRANSPORT_MSG_COUNT);

    private final int id;
    private final ApiUsageRecordKey apiUsageRecordKey;
    private final boolean isEntityKpi;

    KpiKey(int id) {
        this(id, false, null);
    }

    KpiKey(int id, ApiUsageRecordKey apiUsageRecordKey) {
        this(id, false, apiUsageRecordKey);
    }

    KpiKey(int id, boolean isEntityKpi) {
        this(id, isEntityKpi, null);
    }

    KpiKey(int id, boolean isEntityKpi, ApiUsageRecordKey apiUsageRecordKey) {
        this.id = id;
        this.apiUsageRecordKey = apiUsageRecordKey;
        this.isEntityKpi = isEntityKpi;
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
