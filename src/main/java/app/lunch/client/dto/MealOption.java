package app.lunch.client.dto;

import lombok.Getter;

@Getter
public enum MealOption {
    FRIED_CHICKEN_WITH_YOGURT_SOUS("Fried chicken with yogurt sous"),
    BAKED_FISH_WITH_VEGETABLES("Baked fish with vegetables"),
    BEAN_WITH_SALAD("Bean with salad"),
    BACKED_TURKEY_WITH_PATATOES("Baked turkey with potatoes"),
    MEAT_BOLLS_WITH_TOMATOES_SOUS("Meat balls with tomatoes sous");

    private final String displayName;

    MealOption(String displayName) {
        this.displayName = displayName;
    }
}

