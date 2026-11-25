package ui;

import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.AccommodationType;
import model.Listing;
import model.PermanentListing;
import model.TemporaryListing;
import model.User;
import util.Format;

/**
 * The summary card for one listing: a large photo first, then the few facts people scan for
 * (where, what, when, how much). Everything else lives on the details page.
 */
public final class ListingCard {
    public static final double DEFAULT_WIDTH = 290;
    private static final double PHOTO_RATIO = 0.66;
    /** Two lines of title, so every card in a row is the same height. */
    private static final double TITLE_HEIGHT = 42;

    private ListingCard() {
    }

    public static VBox create(Listing listing, User viewer, Consumer<Listing> onOpen) {
        return create(listing, viewer, onOpen, DEFAULT_WIDTH);
    }

    public static VBox create(Listing listing, User viewer, Consumer<Listing> onOpen, double width) {
        VBox card = new VBox();
        card.getStyleClass().add("listing-card");
        card.setPrefWidth(width);
        card.setMinWidth(Region.USE_PREF_SIZE);
        card.setMaxWidth(Region.USE_PREF_SIZE);
        card.setCursor(Cursor.HAND);

        // Photo with the type and match badges on top of it
        String cover = listing.getPhotoPaths().isEmpty() ? null : listing.getPhotoPaths().get(0);
        CoverImage photo = new CoverImage(cover, listing.getLocation(), 14, true);
        photo.prefWidthProperty().bind(card.prefWidthProperty());
        photo.prefHeightProperty().bind(card.prefWidthProperty().multiply(PHOTO_RATIO));
        photo.minHeightProperty().bind(photo.prefHeightProperty());
        StackPane photoLayer = new StackPane(photo);

        Label type = typePill(listing.getType());
        StackPane.setAlignment(type, Pos.TOP_LEFT);
        StackPane.setMargin(type, new Insets(12));
        photoLayer.getChildren().add(type);

        if (listing.isOwnedBy(viewer)) {
            Label own = new Label("Your listing");
            own.getStyleClass().addAll("photo-pill", "pill-owner");
            StackPane.setAlignment(own, Pos.TOP_RIGHT);
            StackPane.setMargin(own, new Insets(12));
            photoLayer.getChildren().add(own);
        }

        // Text
        Label location = new Label(listing.getLocation());
        location.getStyleClass().add("card-location");
        Label roomType = new Label("·  " + listing.getRoomType());
        roomType.getStyleClass().add("card-meta");
        HBox where = new HBox(6, location, roomType);
        where.setAlignment(Pos.CENTER_LEFT);
        if (!listing.isOwnedBy(viewer) && listing.getPreferences().getCompatibilityIssues(viewer).isEmpty()) {
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            Label match = new Label("Match", Icons.of("check", 12, "icon-match"));
            match.getStyleClass().add("card-match");
            match.setMinWidth(Region.USE_PREF_SIZE);
            where.getChildren().addAll(gap, match);
        }

        Label title = new Label(listing.getTitle());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        title.setAlignment(Pos.TOP_LEFT);
        title.setMinHeight(TITLE_HEIGHT);
        title.setPrefHeight(TITLE_HEIGHT);
        title.setMaxHeight(TITLE_HEIGHT);

        Label when = new Label(availability(listing) + "  ·  " + Format.plural(listing.getOpenSpots(), "spot") + " open");
        when.getStyleClass().add("card-meta");

        Region divider = new Region();
        divider.getStyleClass().add("card-divider");

        card.getChildren().addAll(photoLayer, padded(where, title, when, divider, priceRow(listing)));
        card.setOnMouseClicked(event -> onOpen.accept(listing));
        return card;
    }

    /** Resizes a card made by {@link #create}; the photo follows. */
    public static void setWidth(Region card, double width) {
        card.setPrefWidth(width);
    }

    public static Label typeBadge(AccommodationType type) {
        Label badge = new Label(type.toString());
        badge.getStyleClass().addAll("badge", type == AccommodationType.PERMANENT ? "badge-permanent" : "badge-temporary");
        return badge;
    }

    private static VBox padded(javafx.scene.Node... nodes) {
        VBox body = new VBox(6, nodes);
        body.getStyleClass().add("card-body");
        return body;
    }

    private static Label typePill(AccommodationType type) {
        Label pill = new Label(type.toString());
        pill.getStyleClass().addAll("photo-pill", type == AccommodationType.PERMANENT ? "pill-permanent" : "pill-temporary");
        return pill;
    }

    private static String availability(Listing listing) {
        if (listing instanceof TemporaryListing temporary) {
            return Format.dateRange(temporary.getAvailableFrom(), temporary.getAvailableUntil());
        }
        return "From " + Format.shortDate(listing.getAvailableFrom());
    }

    private static HBox priceRow(Listing listing) {
        double price;
        double total;
        String unit;
        if (listing instanceof PermanentListing permanent) {
            price = permanent.getMonthlyRent();
            total = permanent.getMonthlyEquivalentCost();
            unit = "/ month";
        } else {
            TemporaryListing temporary = (TemporaryListing) listing;
            price = temporary.getDailyRate();
            total = Math.round(temporary.getTotalDailyCost() * 100) / 100.0;
            unit = "/ day";
        }
        Label amount = new Label(Format.money(price));
        amount.getStyleClass().add("card-price");
        Label per = new Label(unit);
        per.getStyleClass().add("card-price-unit");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label withUtilities = new Label(total > price ? Format.money(total) + " with utilities" : "Utilities included");
        withUtilities.getStyleClass().add("card-total");

        HBox row = new HBox(4, amount, per, spacer, withUtilities);
        row.setAlignment(Pos.BASELINE_LEFT);
        return row;
    }
}
