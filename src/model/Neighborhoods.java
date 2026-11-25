package model;

import java.util.List;
import java.util.Map;

/** General areas a listing can be in. Listings only ever show the area, never an exact address. */
public final class Neighborhoods {
    public static final List<String> ALL = List.of(
            "Allston",
            "Back Bay",
            "Beacon Hill",
            "Brighton",
            "Brookline",
            "Central Square",
            "Charlestown",
            "Chinatown",
            "Davis Square",
            "Dorchester",
            "East Boston",
            "Fenway",
            "Harvard Square",
            "Jamaica Plain",
            "Kendall Square",
            "Mission Hill",
            "North End",
            "Roslindale",
            "Roxbury",
            "Seaport",
            "South Boston",
            "South End",
            "Union Square",
            "West End");

    /** Area names used by older versions of the app, and what they are called now. */
    public static final Map<String, String> RENAMED = Map.of(
            "Fenway-Kenmore", "Fenway",
            "Huntington Avenue", "Fenway",
            "Newbury Street", "Back Bay");

    private Neighborhoods() {
    }
}
