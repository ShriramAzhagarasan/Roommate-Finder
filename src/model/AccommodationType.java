package model;

/** Whether a household is offering a short-term stay or a long-term room. */
public enum AccommodationType {
    PERMANENT("Permanent"),
    TEMPORARY("Temporary");

    private final String label;

    AccommodationType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
