package org.thingsboard.reporting.alarms;

public enum AlarmSeverity {
    CLEARED(1),
    MINOR(2),
    WARNING(3),
    MAJOR(4),
    CRITICAL(5);

    private final int id;

    AlarmSeverity(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
