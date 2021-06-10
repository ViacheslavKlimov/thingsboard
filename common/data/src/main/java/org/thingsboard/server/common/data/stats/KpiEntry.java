package org.thingsboard.server.common.data.stats;

import lombok.Data;

@Data
public class KpiEntry {
    private final KpiKey key;
    private Long value;

    public KpiEntry(KpiKey key, Long value) {
        this.key = key;
        this.value = value;
    }
}
