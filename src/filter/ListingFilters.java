package filter;

import java.time.LocalDate;

import model.AccommodationType;
import model.Listing;
import model.PermanentListing;
import model.RoomType;
import model.TemporaryListing;
import model.User;

/** The individual criteria available on the listings page. */
public final class ListingFilters {

    private ListingFilters() {
    }

    public static Filter<Listing> keyword(String query) {
        return listing -> listing.matchesKeyword(query);
    }

    public static Filter<Listing> type(AccommodationType type) {
        return listing -> listing.getType() == type;
    }

    public static Filter<Listing> location(String location) {
        return listing -> listing.getLocation().equalsIgnoreCase(location);
    }

    public static Filter<Listing> roomType(RoomType roomType) {
        return listing -> listing.getRoomType() == roomType;
    }

    /** Monthly cost per person including utilities. Either bound may be null. */
    public static Filter<Listing> budget(Double min, Double max) {
        return listing -> {
            double cost = listing.getMonthlyEquivalentCost();
            return (min == null || cost >= min) && (max == null || cost <= max);
        };
    }

    public static Filter<Listing> availability(LocalDate moveIn, LocalDate moveOut) {
        return listing -> listing.isAvailableFor(moveIn, moveOut);
    }

    public static Filter<Listing> petFriendly() {
        return listing -> listing.getPreferences().isPetFriendly();
    }

    public static Filter<Listing> nonSmoking() {
        return listing -> !listing.getPreferences().isSmokingAllowed();
    }

    public static Filter<Listing> compatibleWith(User user) {
        return listing -> listing.getPreferences().getCompatibilityIssues(user).isEmpty();
    }

    /** Detailed budget filter for permanent rooms. Temporary stays are not affected. */
    public static Filter<Listing> maxMonthlyUtilities(double max) {
        return listing -> !(listing instanceof PermanentListing permanent) || permanent.getMonthlyUtilities() <= max;
    }

    /** Detailed budget filter for temporary stays. Permanent rooms are not affected. */
    public static Filter<Listing> maxDailyCost(double max) {
        return listing -> !(listing instanceof TemporaryListing temporary) || temporary.getTotalDailyCost() <= max;
    }
}
