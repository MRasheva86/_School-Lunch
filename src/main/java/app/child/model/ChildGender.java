package app.child.model;

public enum ChildGender {
    MALE ("Boy"),
    FEMALE ("Girl");

    private String displayName;

    ChildGender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
