package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import util.Format;

/** A long-term room priced per month, with a minimum lease. */
public class PermanentListing extends Listing {
    private static final long serialVersionUID = 1L;

    private double monthlyRent;
    private int minLeaseMonths = 12;

    public PermanentListing(String ownerEmail) {
        super(ownerEmail);
    }

    public PermanentListing(String id, String ownerEmail, LocalDateTime createdAt) {
        super(id, ownerEmail, createdAt);
    }

    @Override
    public AccommodationType getType() {
        return AccommodationType.PERMANENT;
    }

    @Override
    public double getMonthlyEquivalentCost() {
        return monthlyRent + getMonthlyUtilities();
    }

    @Override
    public String getPriceLabel() {
        return Format.money(monthlyRent) + " / month";
    }

    @Override
    public String getTotalCostLabel() {
        return Format.money(getMonthlyEquivalentCost()) + " / month with utilities";
    }

    @Override
    public boolean isAvailableFor(LocalDate moveIn, LocalDate moveOut) {
        // A permanent room is open from its start date onwards; any move-out date works.
        return moveIn == null || !getAvailableFrom().isAfter(moveIn);
    }

    @Override
    public String getAvailabilityLabel() {
        return "Available from " + Format.date(getAvailableFrom());
    }

    public double getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(double monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public int getMinLeaseMonths() {
        return minLeaseMonths;
    }

    public void setMinLeaseMonths(int minLeaseMonths) {
        this.minLeaseMonths = minLeaseMonths;
    }
}
