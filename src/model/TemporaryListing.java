package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import util.Format;

/** A short-term stay priced per day, available between two dates. */
public class TemporaryListing extends Listing {
    private static final long serialVersionUID = 1L;
    private static final double DAYS_PER_MONTH = 30.0;

    private double dailyRate;
    private LocalDate availableUntil = LocalDate.now().plusMonths(1);

    public TemporaryListing(String ownerEmail) {
        super(ownerEmail);
    }

    public TemporaryListing(String id, String ownerEmail, LocalDateTime createdAt) {
        super(id, ownerEmail, createdAt);
    }

    @Override
    public AccommodationType getType() {
        return AccommodationType.TEMPORARY;
    }

    /** Utilities are entered per month; spread them over each day of the stay. */
    public double getDailyUtilities() {
        return getMonthlyUtilities() / DAYS_PER_MONTH;
    }

    public double getTotalDailyCost() {
        return dailyRate + getDailyUtilities();
    }

    public long getStayLengthDays() {
        return ChronoUnit.DAYS.between(getAvailableFrom(), availableUntil);
    }

    @Override
    public double getMonthlyEquivalentCost() {
        return getTotalDailyCost() * DAYS_PER_MONTH;
    }

    @Override
    public String getPriceLabel() {
        return Format.money(dailyRate) + " / day";
    }

    @Override
    public String getTotalCostLabel() {
        return Format.money(Math.round(getTotalDailyCost() * 100) / 100.0) + " / day with utilities";
    }

    @Override
    public boolean isAvailableFor(LocalDate moveIn, LocalDate moveOut) {
        LocalDate start = getAvailableFrom();
        if (moveIn != null && (moveIn.isBefore(start) || moveIn.isAfter(availableUntil))) {
            return false;
        }
        return moveOut == null || !(moveOut.isAfter(availableUntil) || moveOut.isBefore(start));
    }

    @Override
    public String getAvailabilityLabel() {
        return Format.date(getAvailableFrom()) + " – " + Format.date(availableUntil);
    }

    public double getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(double dailyRate) {
        this.dailyRate = dailyRate;
    }

    public LocalDate getAvailableUntil() {
        return availableUntil;
    }

    public void setAvailableUntil(LocalDate availableUntil) {
        this.availableUntil = availableUntil;
    }
}
