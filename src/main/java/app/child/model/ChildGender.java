package app.child.model;

import lombok.Getter;

@Getter
public enum ChildGender {

    MALE ("Boy"),
    FEMALE ("Girl");

    private final String displayName;

    ChildGender(String displayName) {
        this.displayName = displayName;
    }

}
