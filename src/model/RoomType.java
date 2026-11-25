package model;

public enum RoomType {
    PRIVATE("Private room"),
    SHARED("Shared room");

    private final String label;

    RoomType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
