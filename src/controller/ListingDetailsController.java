package controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.HouseholdPreferences;
import model.Listing;
import model.Message;
import model.PermanentListing;
import model.TemporaryListing;
import model.User;
import service.DataStore;
import service.Session;
import ui.Dialogs;
import ui.Inputs;
import ui.ListingCard;
import ui.Photos;
import util.Format;

/** Everything about one household: photos, costs, criteria and how to get in touch. */
public class ListingDetailsController {
    private static final double PHOTO_WIDTH = 640;
    private static final double PHOTO_HEIGHT = 380;

    @FXML private Label bannerLabel;
    @FXML private Label titleLabel;
    @FXML private HBox badgesBox;
    @FXML private Label locationLabel;
    @FXML private Label priceLabel;
    @FXML private Label totalLabel;
    @FXML private StackPane mainPhotoHolder;
    @FXML private HBox thumbnailsBox;
    @FXML private Button prevPhotoButton;
    @FXML private Button nextPhotoButton;
    @FXML private Label residentsLabel;
    @FXML private Label descriptionLabel;
    @FXML private VBox criteriaBox;
    @FXML private Label costNoteLabel;
    @FXML private GridPane costGrid;
    @FXML private VBox availabilityBox;
    @FXML private VBox compatibilityCard;
    @FXML private VBox compatibilityBox;
    @FXML private Label contactTitle;
    @FXML private VBox ownerBox;
    @FXML private Label contactedLabel;
    @FXML private Button contactButton;
    @FXML private HBox ownerActions;

    private ShellController shell;
    private Listing listing;
    private User owner;
    private int photoIndex;

    public void init(ShellController shell, Listing listing, String bannerMessage) {
        this.shell = shell;
        this.listing = listing;
        this.owner = DataStore.getInstance().findUser(listing.getOwnerEmail()).orElse(null);
        if (bannerMessage != null) {
            Inputs.showMessage(bannerLabel, bannerMessage);
        }
        showHeader();
        showPhoto(0);
        showHousehold();
        showCosts();
        showAvailability();
        showCompatibility();
        showContact();
    }

    private void showHeader() {
        titleLabel.setText(listing.getTitle());
        Label roomType = new Label(listing.getRoomType().toString());
        roomType.getStyleClass().add("tag");
        badgesBox.getChildren().setAll(ListingCard.typeBadge(listing.getType()), roomType);
        locationLabel.setText(listing.getLocation() + ", Boston  ·  General area only. The exact address is shared once you connect.");
        priceLabel.setText(listing.getPriceLabel());
        totalLabel.setText(listing.getTotalCostLabel());
    }

    // ---------------------------------------------------------------- photos

    private void showPhoto(int index) {
        List<String> photos = listing.getPhotoPaths();
        photoIndex = photos.isEmpty() ? 0 : Math.floorMod(index, photos.size());
        String path = photos.isEmpty() ? null : photos.get(photoIndex);
        mainPhotoHolder.getChildren().setAll(Photos.box(path, listing.getLocation(), PHOTO_WIDTH, PHOTO_HEIGHT, 8));

        thumbnailsBox.getChildren().clear();
        for (int i = 0; i < photos.size(); i++) {
            StackPane thumb = Photos.box(photos.get(i), "", 84, 56, 4);
            thumb.getStyleClass().add(i == photoIndex ? "thumb-selected" : "thumb");
            thumb.setCursor(Cursor.HAND);
            int target = i;
            thumb.setOnMouseClicked(event -> showPhoto(target));
            thumbnailsBox.getChildren().add(thumb);
        }
        if (photos.isEmpty()) {
            Label none = new Label("The household hasn't added photos yet.");
            none.getStyleClass().add("muted");
            thumbnailsBox.getChildren().add(none);
        }
        boolean canBrowse = photos.size() > 1;
        prevPhotoButton.setDisable(!canBrowse);
        nextPhotoButton.setDisable(!canBrowse);
    }

    @FXML
    private void previousPhoto() {
        showPhoto(photoIndex - 1);
    }

    @FXML
    private void nextPhoto() {
        showPhoto(photoIndex + 1);
    }

    // ---------------------------------------------------------------- household

    private void showHousehold() {
        int residents = listing.getCurrentResidents();
        residentsLabel.setText((residents == 0 ? "No current residents" : Format.plural(residents, "current resident"))
                + "  ·  " + Format.plural(listing.getOpenSpots(), "open spot"));
        descriptionLabel.setText(listing.getDescription());

        HouseholdPreferences prefs = listing.getPreferences();
        criteriaBox.getChildren().setAll(
                criterion("Pets", prefs.isPetFriendly() ? "Pet-friendly household" : "No pets, please"),
                criterion("Smoking", prefs.isSmokingAllowed() ? "Smoking is allowed" : "Non-smoking household"),
                criterion("Gender", prefs.getGenderPreference().toString()),
                criterion("Age", prefs.getMinAge() == 0 && prefs.getMaxAge() == 0 ? "Any age" : prefs.getAgeRangeLabel()));
    }

    private static HBox criterion(String name, String value) {
        Label key = new Label(name);
        key.getStyleClass().add("field-label");
        key.setMinWidth(80);
        Label text = new Label(value);
        text.setWrapText(true);
        HBox row = new HBox(10, key, text);
        return row;
    }

    // ---------------------------------------------------------------- costs

    private void showCosts() {
        costGrid.getChildren().clear();
        int row = 0;
        if (listing instanceof PermanentListing permanent) {
            costNoteLabel.setText("Per person, per month.");
            row = costRow(row, "Rent", permanent.getMonthlyRent(), false);
            row = costRow(row, "Wi-Fi", listing.getWifiCost(), false);
            row = costRow(row, "Electricity", listing.getElectricityCost(), false);
            row = costRow(row, "Laundry", listing.getLaundryCost(), false);
            costRow(row, "Total / month", permanent.getMonthlyEquivalentCost(), true);
        } else if (listing instanceof TemporaryListing temporary) {
            costNoteLabel.setText("Per person, per day. Monthly utilities are spread over 30 days.");
            row = costRow(row, "Daily rate", temporary.getDailyRate(), false);
            row = costRow(row, "Wi-Fi", listing.getWifiCost() / 30, false);
            row = costRow(row, "Electricity", listing.getElectricityCost() / 30, false);
            row = costRow(row, "Laundry", listing.getLaundryCost() / 30, false);
            row = costRow(row, "Total / day", temporary.getTotalDailyCost(), true);
            long days = temporary.getStayLengthDays();
            costRow(row, "Full stay (" + Format.plural((int) days, "day") + ")", temporary.getTotalDailyCost() * days, true);
        }
    }

    private int costRow(int row, String name, double amount, boolean total) {
        Label label = new Label(name);
        Label value = new Label(Format.money(Math.round(amount * 100) / 100.0));
        if (total) {
            label.getStyleClass().add("cost-total");
            value.getStyleClass().add("cost-total");
        }
        costGrid.addRow(row, label, value);
        return row + 1;
    }

    private void showAvailability() {
        availabilityBox.getChildren().clear();
        if (listing instanceof PermanentListing permanent) {
            availabilityBox.getChildren().addAll(
                    new Label("Move in from " + Format.date(listing.getAvailableFrom())),
                    new Label("Minimum lease: " + Format.plural(permanent.getMinLeaseMonths(), "month")));
        } else if (listing instanceof TemporaryListing temporary) {
            availabilityBox.getChildren().addAll(
                    new Label("From " + Format.date(temporary.getAvailableFrom())),
                    new Label("Until " + Format.date(temporary.getAvailableUntil())));
        }
        Label posted = new Label("Posted " + Format.date(listing.getCreatedAt().toLocalDate()));
        posted.getStyleClass().add("hint");
        availabilityBox.getChildren().add(posted);
    }

    // ---------------------------------------------------------------- compatibility and contact

    private void showCompatibility() {
        User viewer = Session.getCurrentUser();
        if (listing.isOwnedBy(viewer)) {
            compatibilityCard.setVisible(false);
            compatibilityCard.setManaged(false);
            return;
        }
        List<String> issues = listing.getPreferences().getCompatibilityIssues(viewer);
        compatibilityBox.getChildren().clear();
        if (issues.isEmpty()) {
            Label good = new Label("✓ Your profile fits everything this household is looking for.");
            good.getStyleClass().add("match-good");
            good.setWrapText(true);
            compatibilityBox.getChildren().add(good);
        } else {
            for (String issue : issues) {
                Label bad = new Label("✗ " + issue);
                bad.getStyleClass().add("match-bad");
                bad.setWrapText(true);
                compatibilityBox.getChildren().add(bad);
            }
        }
        Label note = new Label("Based on the lifestyle details in your profile.");
        note.getStyleClass().add("hint");
        compatibilityBox.getChildren().add(note);
    }

    private void showContact() {
        User viewer = Session.getCurrentUser();
        boolean isOwner = listing.isOwnedBy(viewer);
        ownerBox.getChildren().clear();

        if (isOwner) {
            contactTitle.setText("Your listing");
            long inquiries = DataStore.getInstance().countInquiries(listing);
            ownerBox.getChildren().add(new Label(inquiries == 0
                    ? "No one has contacted you about this listing yet."
                    : Format.plural((int) inquiries, "message") + " received. See them under Profile → Messages."));
            contactButton.setVisible(false);
            contactButton.setManaged(false);
            ownerActions.setVisible(true);
            ownerActions.setManaged(true);
            return;
        }

        if (owner == null) {
            ownerBox.getChildren().add(new Label("This member's account is no longer available."));
            contactButton.setDisable(true);
            return;
        }
        Label name = new Label(owner.getName() + (owner.getAge() > 0 ? ", " + owner.getAge() : ""));
        name.getStyleClass().add("field-label");
        ownerBox.getChildren().add(name);
        ownerBox.getChildren().add(new Label(owner.getSleepSchedule() + "  ·  " + (owner.isSmoker() ? "Smoker" : "Non-smoker")
                + "  ·  " + (owner.hasPets() ? "Has pets" : "No pets")));
        if (!owner.getBio().isBlank()) {
            Label bio = new Label(owner.getBio());
            bio.setWrapText(true);
            bio.getStyleClass().add("muted");
            ownerBox.getChildren().add(bio);
        }
        Label email = new Label("Email: " + owner.getEmail());
        ownerBox.getChildren().add(email);
        if (!owner.getPhone().isBlank()) {
            ownerBox.getChildren().add(new Label("Phone: " + owner.getPhone()));
        }
        contactButton.setText("Message " + owner.getFirstName());
        updateContactedLabel();
    }

    private void updateContactedLabel() {
        Optional<Message> last = DataStore.getInstance().lastMessageAbout(Session.getCurrentUser().getEmail(), listing);
        if (last.isPresent()) {
            Inputs.showMessage(contactedLabel, "You messaged this household on " + Format.dateTime(last.get().sentAt()) + ".");
        }
    }

    @FXML
    private void contactOwner() {
        User viewer = Session.getCurrentUser();
        String greeting = "Hi " + owner.getFirstName() + ", I'm interested in your listing \"" + listing.getTitle()
                + "\". A bit about me: ";
        Optional<String> text = Dialogs.composeMessage("Contact household",
                "Message " + owner.getName() + " about \"" + listing.getTitle() + "\"", greeting);
        if (text.isEmpty()) {
            return;
        }
        DataStore.getInstance().sendMessage(new Message(viewer.getEmail(), viewer.getName(), owner.getEmail(),
                listing.getId(), listing.getTitle(), text.get(), LocalDateTime.now()));
        updateContactedLabel();
        Dialogs.showInfo("Message sent", owner.getFirstName() + " will see your message in their inbox and can reply to you there"
                + " or at " + viewer.getEmail() + ".");
    }

    @FXML
    private void editListing() {
        shell.showPostEditor(listing);
    }

    @FXML
    private void deleteListing() {
        if (Dialogs.confirm("Delete listing", "Delete \"" + listing.getTitle() + "\"? This can't be undone.", "Delete")) {
            DataStore.getInstance().deleteListing(listing);
            shell.goBack();
        }
    }

    @FXML
    private void goBack() {
        shell.goBack();
    }
}
