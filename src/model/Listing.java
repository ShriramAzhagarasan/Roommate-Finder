package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * A post by a current resident advertising open spots in their household.
 * Subclasses decide how the room is priced and when it is available.
 */
public abstract class Listing implements Serializable, Searchable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String ownerEmail;
    private final LocalDateTime createdAt;

    private String title = "";
    private String description = "";
    private String location = "";
    private RoomType roomType = RoomType.PRIVATE;
    private int currentResidents;
    private int openSpots = 1;
    // Shared monthly expenses, per person
    private double wifiCost;
    private double electricityCost;
    private double laundryCost;
    private LocalDate availableFrom = LocalDate.now();
    private List<String> photoPaths = new ArrayList<>();
    private HouseholdPreferences preferences = new HouseholdPreferences();

    protected Listing(String ownerEmail) {
        this(UUID.randomUUID().toString(), ownerEmail, LocalDateTime.now());
    }

    protected Listing(String id, String ownerEmail, LocalDateTime createdAt) {
        this.id = id;
        this.ownerEmail = ownerEmail;
        this.createdAt = createdAt;
    }

    public abstract AccommodationType getType();

    /** Everything a person pays per month (rent plus utilities). Used for budget filters and sorting. */
    public abstract double getMonthlyEquivalentCost();

    /** Headline price shown on cards, e.g. "$950 / month". */
    public abstract String getPriceLabel();

    /** One-line summary of the total cost including utilities. */
    public abstract String getTotalCostLabel();

    /** Whether a person could move in on {@code moveIn} and (optionally) stay until {@code moveOut}. */
    public abstract boolean isAvailableFor(LocalDate moveIn, LocalDate moveOut);

    public abstract String getAvailabilityLabel();

    public double getMonthlyUtilities() {
        return wifiCost + electricityCost + laundryCost;
    }

    public boolean isOwnedBy(User user) {
        return user != null && user.getEmail().equalsIgnoreCase(ownerEmail);
    }

    @Override
    public boolean matchesKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String query = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(title, query)
                || contains(description, query)
                || contains(location, query)
                || contains(roomType.toString(), query)
                || contains(getType().toString(), query);
    }

    private static boolean contains(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }

    public String getId() {
        return id;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public int getCurrentResidents() {
        return currentResidents;
    }

    public void setCurrentResidents(int currentResidents) {
        this.currentResidents = currentResidents;
    }

    public int getOpenSpots() {
        return openSpots;
    }

    public void setOpenSpots(int openSpots) {
        this.openSpots = openSpots;
    }

    public double getWifiCost() {
        return wifiCost;
    }

    public void setWifiCost(double wifiCost) {
        this.wifiCost = wifiCost;
    }

    public double getElectricityCost() {
        return electricityCost;
    }

    public void setElectricityCost(double electricityCost) {
        this.electricityCost = electricityCost;
    }

    public double getLaundryCost() {
        return laundryCost;
    }

    public void setLaundryCost(double laundryCost) {
        this.laundryCost = laundryCost;
    }

    public LocalDate getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDate availableFrom) {
        this.availableFrom = availableFrom;
    }

    public List<String> getPhotoPaths() {
        return photoPaths;
    }

    public void setPhotoPaths(List<String> photoPaths) {
        this.photoPaths = new ArrayList<>(photoPaths);
    }

    public HouseholdPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(HouseholdPreferences preferences) {
        this.preferences = preferences;
    }
}
