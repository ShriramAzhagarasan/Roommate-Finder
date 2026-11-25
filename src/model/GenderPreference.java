package model;

/** The gender a household would prefer its new roommate to be. */
public enum GenderPreference {
    ANY("No gender preference", "anyone"),
    MALE("Male only", "male roommates"),
    FEMALE("Female only", "female roommates");

    private final String label;
    private final String description;

    GenderPreference(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public boolean accepts(Gender gender) {
        return switch (this) {
            case ANY -> true;
            case MALE -> gender == Gender.MALE;
            case FEMALE -> gender == Gender.FEMALE;
        };
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return label;
    }
}
