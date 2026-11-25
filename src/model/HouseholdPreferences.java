package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** What the current residents allow and who they would like to live with. */
public class HouseholdPreferences implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean petFriendly;
    private boolean smokingAllowed;
    private GenderPreference genderPreference = GenderPreference.ANY;
    private int minAge; // 0 means no minimum
    private int maxAge; // 0 means no maximum

    /** Reasons the given user would not fit this household. An empty list means they are compatible. */
    public List<String> getCompatibilityIssues(User user) {
        List<String> issues = new ArrayList<>();
        if (user == null) {
            return issues;
        }
        if (user.isSmoker() && !smokingAllowed) {
            issues.add("This is a non-smoking household");
        }
        if (user.hasPets() && !petFriendly) {
            issues.add("Pets are not allowed");
        }
        if (!genderPreference.accepts(user.getGender())) {
            issues.add("The household prefers " + genderPreference.getDescription());
        }
        if (user.getAge() > 0 && !acceptsAge(user.getAge())) {
            issues.add("The household prefers roommates aged " + getAgeRangeLabel());
        }
        return issues;
    }

    public boolean acceptsAge(int age) {
        return (minAge == 0 || age >= minAge) && (maxAge == 0 || age <= maxAge);
    }

    /** Short labels shown as tags on listing cards. */
    public List<String> getSummaryTags() {
        List<String> tags = new ArrayList<>();
        tags.add(petFriendly ? "Pet-friendly" : "No pets");
        tags.add(smokingAllowed ? "Smoking OK" : "Non-smoking");
        if (genderPreference != GenderPreference.ANY) {
            tags.add(genderPreference.toString());
        }
        if (minAge > 0 || maxAge > 0) {
            tags.add("Ages " + getAgeRangeLabel());
        }
        return tags;
    }

    public String getAgeRangeLabel() {
        if (minAge > 0 && maxAge > 0) {
            return minAge + "–" + maxAge;
        }
        if (minAge > 0) {
            return minAge + "+";
        }
        if (maxAge > 0) {
            return "up to " + maxAge;
        }
        return "any age";
    }

    public boolean isPetFriendly() {
        return petFriendly;
    }

    public void setPetFriendly(boolean petFriendly) {
        this.petFriendly = petFriendly;
    }

    public boolean isSmokingAllowed() {
        return smokingAllowed;
    }

    public void setSmokingAllowed(boolean smokingAllowed) {
        this.smokingAllowed = smokingAllowed;
    }

    public GenderPreference getGenderPreference() {
        return genderPreference;
    }

    public void setGenderPreference(GenderPreference genderPreference) {
        this.genderPreference = genderPreference == null ? GenderPreference.ANY : genderPreference;
    }

    public int getMinAge() {
        return minAge;
    }

    public void setMinAge(int minAge) {
        this.minAge = minAge;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }
}
