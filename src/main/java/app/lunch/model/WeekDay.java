package app.lunch.model;

public enum WeekDay {
    MONDAY ("Monday"),
    TUESDAY ("Tuesday"),
    WEDNESDAY("Wednesday"),
    THURSDAY("Thursday"),
    FRIDAY("Friday");

    private final String displayName;

    WeekDay(String displayName) {
        this.displayName = displayName;
    }
}
