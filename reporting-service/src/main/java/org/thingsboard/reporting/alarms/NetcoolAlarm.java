package org.thingsboard.reporting.alarms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NetcoolAlarm {
    private String title;
    private AlarmCategory category;
    private AlarmSeverity severity;
    /* ... */
}
