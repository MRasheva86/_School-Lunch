package app.parent.model;

import lombok.Getter;

@Getter
public enum ParentRole {
    ROLE_USER ("User"),
    ROLE_ADMIN ("Admin");

    private final String displayName;

    ParentRole(String displayName) {
        this.displayName = displayName;
    }

}
