package app.parent.model;

public enum ParentRole {
    ROLE_USER ("User"),
    ROLE_ADMIN ("Admin");

    private final String displayName;

    ParentRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
