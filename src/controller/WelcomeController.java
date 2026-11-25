package controller;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import application.Navigator;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import model.Listing;
import service.DataStore;
import ui.ListingCard;
import ui.Photos;
import util.Format;

/** The landing page: a pitch for the app, a live preview of real listings, and the way in. */
public class WelcomeController {
    private static final double PREVIEW_WIDTH = 280;

    @FXML
    private ScrollPane scroll;

    @FXML
    private HBox statsBox;

    @FXML
    private Pane previewPane;

    @FXML
    private VBox howItWorks;

    @FXML
    private void initialize() {
        List<Listing> listings = DataStore.getInstance().getAllListings();
        showStats(listings);
        showPreview(listings);
    }

    /** Headline numbers worked out from the listings that are actually in the app. */
    private void showStats(List<Listing> listings) {
        if (listings.isEmpty()) {
            statsBox.setVisible(false);
            statsBox.setManaged(false);
            return;
        }
        int openSpots = 0;
        Set<String> neighborhoods = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        double cheapest = Double.MAX_VALUE;
        for (Listing listing : listings) {
            openSpots += listing.getOpenSpots();
            neighborhoods.add(listing.getLocation().trim());
            cheapest = Math.min(cheapest, listing.getMonthlyEquivalentCost());
        }
        statsBox.getChildren().setAll(
                stat(String.valueOf(openSpots), openSpots == 1 ? "open spot" : "open spots"),
                stat(String.valueOf(neighborhoods.size()), neighborhoods.size() == 1 ? "neighborhood" : "neighborhoods"),
                stat(Format.money(cheapest), "lowest monthly total"));
    }

    private static Node stat(String value, String caption) {
        Label number = new Label(value);
        number.getStyleClass().add("stat-value");
        Label text = new Label(caption);
        text.getStyleClass().add("stat-caption");
        return new VBox(2, number, text);
    }

    /** Two of the newest listings, fanned out like cards on a table, with a couple of floating callouts. */
    private void showPreview(List<Listing> listings) {
        List<Listing> newest = listings.stream()
                .sorted(Comparator.comparing(Listing::getCreatedAt).reversed())
                .limit(2)
                .toList();
        if (newest.isEmpty()) {
            return;
        }

        if (newest.size() > 1) {
            Node back = previewCard(newest.get(1));
            back.setLayoutX(118);
            back.setLayoutY(18);
            back.setRotate(5);
            back.setOpacity(0.92);
            previewPane.getChildren().add(back);
        }
        Node front = previewCard(newest.get(0));
        front.setLayoutX(20);
        front.setLayoutY(70);
        front.setRotate(-3);
        previewPane.getChildren().add(front);

        Label match = callout("✓  Matches your profile", "callout-good");
        match.setLayoutX(0);
        match.setLayoutY(34);
        Label cost = callout("Utilities included in the total", "callout");
        cost.setLayoutX(196);
        cost.setLayoutY(392);
        previewPane.getChildren().addAll(match, cost);

        floatGently(front, 0, 6);
        floatGently(match, 600, 5);
        floatGently(cost, 1200, 5);
    }

    private static Node previewCard(Listing listing) {
        StackPane photo = Photos.cover(listing.getPhotoPaths(), listing.getLocation(), PREVIEW_WIDTH - 20, 150, 10);
        Label badge = ListingCard.typeBadge(listing.getType());
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(10));
        photo.getChildren().add(badge);
        // The demo data has no photos; "No photos yet" is fine in the app but noisy in a showcase.
        photo.lookupAll(".placeholder-subtitle").forEach(node -> node.setVisible(false));

        Label title = new Label(listing.getTitle());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        Label place = new Label(listing.getLocation() + "  ·  " + listing.getRoomType());
        place.getStyleClass().add("muted");
        Label price = new Label(listing.getPriceLabel());
        price.getStyleClass().add("card-price");

        FlowPane tags = new FlowPane(6, 6);
        for (String tag : listing.getPreferences().getSummaryTags()) {
            Label label = new Label(tag);
            label.getStyleClass().add("tag");
            tags.getChildren().add(label);
        }

        VBox card = new VBox(7, photo, title, place, price, tags);
        card.getStyleClass().add("preview-card");
        card.setPrefWidth(PREVIEW_WIDTH);
        card.setMaxWidth(PREVIEW_WIDTH);
        card.setMouseTransparent(true);
        return card;
    }

    private static Label callout(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setMouseTransparent(true);
        return label;
    }

    /** A slow up-and-down drift so the hero doesn't feel static. */
    private static void floatGently(Node node, double delayMillis, double distance) {
        TranslateTransition drift = new TranslateTransition(Duration.seconds(3.2), node);
        drift.setFromY(0);
        drift.setToY(-distance);
        drift.setAutoReverse(true);
        drift.setCycleCount(TranslateTransition.INDEFINITE);
        drift.setInterpolator(Interpolator.EASE_BOTH);
        drift.setDelay(Duration.millis(delayMillis));
        drift.play();
    }

    @FXML
    private void scrollToHowItWorks() {
        double contentHeight = scroll.getContent().getBoundsInLocal().getHeight();
        double viewportHeight = scroll.getViewportBounds().getHeight();
        double target = contentHeight <= viewportHeight ? 0
                : Math.min(1, howItWorks.getBoundsInParent().getMinY() / (contentHeight - viewportHeight));
        new Timeline(new KeyFrame(Duration.millis(450),
                new KeyValue(scroll.vvalueProperty(), target, Interpolator.EASE_BOTH))).play();
    }

    @FXML
    private void goToLogin() {
        Navigator.showLogin();
    }

    @FXML
    private void goToRegister() {
        Navigator.showRegister();
    }
}
