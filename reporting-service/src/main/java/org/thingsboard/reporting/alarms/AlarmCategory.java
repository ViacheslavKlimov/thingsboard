package org.thingsboard.reporting.alarms;

public enum AlarmCategory {
    COMMUNICATION(1),
    QUALITY_OF_SERVICE(2),
    PROCESSING(3),
    EQUIPMENT(4),
    ENVIRONMENTAL(5);

    private final int id;

    AlarmCategory(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

}
