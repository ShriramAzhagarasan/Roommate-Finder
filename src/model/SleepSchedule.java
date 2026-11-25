package model;

public enum SleepSchedule {
    EARLY_BIRD("Early bird"),
    NIGHT_OWL("Night owl"),
    FLEXIBLE("Flexible");

    private final String label;

    SleepSchedule(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
