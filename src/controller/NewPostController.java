package controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import application.Navigator;
import javafx.animation.PauseTransition;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import model.GenderPreference;
import model.HouseholdPreferences;
import model.Listing;
import model.Neighborhoods;
import model.PermanentListing;
import model.RoomType;
import model.TemporaryListing;
import model.User;
import service.DataStore;
import service.Session;
import ui.Icons;
import ui.Inputs;
import ui.ListingCard;
import ui.Photos;
import util.Format;

/** Form for creating a new roommate post or editing one of your own, with a live preview of the card. */
public class NewPostController {
    private static final int MAX_PHOTOS = 10;
    private static final int MAX_TITLE_LENGTH = 80;
    private static final int MIN_DESCRIPTION_LENGTH = 20;
    private static final double PREVIEW_WIDTH = 300;

    @FXML private Button backButton;
    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;
    @FXML private TextField titleField;
    @FXML private Label titleCounter;
    @FXML private ToggleGroup typeGroup;
    @FXML private ToggleButton permanentRadio;
    @FXML private ToggleButton temporaryRadio;
    @FXML private ComboBox<String> locationBox;
    @FXML private ToggleGroup roomTypeGroup;
    @FXML private ToggleButton privateToggle;
    @FXML private ToggleButton sharedToggle;
    @FXML private Spinner<Integer> residentsSpinner;
    @FXML private Spinner<Integer> openSpotsSpinner;
    @FXML private StackPane privacyIcon;
    @FXML private Label availabilityHint;
    @FXML private DatePicker availableFromPicker;
    @FXML private VBox untilBox;
    @FXML private DatePicker availableUntilPicker;
    @FXML private VBox leaseBox;
    @FXML private Spinner<Integer> leaseSpinner;
    @FXML private Label rentLabel;
    @FXML private Label rentUnit;
    @FXML private TextField rentField;
    @FXML private TextField wifiField;
    @FXML private TextField electricityField;
    @FXML private TextField laundryField;
    @FXML private VBox dropZone;
    @FXML private StackPane dropIcon;
    @FXML private Label photoHint;
    @FXML private Button addPhotosButton;
    @FXML private FlowPane photosPane;
    @FXML private TextArea descriptionArea;
    @FXML private Label descriptionCounter;
    @FXML private CheckBox petFriendlyBox;
    @FXML private CheckBox smokingAllowedBox;
    @FXML private ComboBox<GenderPreference> genderPreferenceBox;
    @FXML private TextField minAgeField;
    @FXML private TextField maxAgeField;

    @FXML private StackPane previewHolder;
    @FXML private Label costSummaryLabel;
    @FXML private VBox checklistBox;
    @FXML private Label errorLabel;
    @FXML private Button publishButton;

    private ShellController shell;
    private Listing editing;
    /** Photos already saved in the app's photo folder, plus new ones picked from disk (imported on publish). */
    private final List<String> photoPaths = new ArrayList<>();
    /** Coalesces rapid edits (typing) into one preview refresh. */
    private final PauseTransition previewDelay = new PauseTransition(Duration.millis(120));

    @FXML
    private void initialize() {
        locationBox.setItems(FXCollections.observableArrayList(Neighborhoods.ALL));
        locationBox.setVisibleRowCount(12);
        privateToggle.setUserData(RoomType.PRIVATE);
        sharedToggle.setUserData(RoomType.SHARED);
        genderPreferenceBox.setItems(FXCollections.observableArrayList(GenderPreference.values()));
        genderPreferenceBox.setValue(GenderPreference.ANY);
        keepOneSelected(typeGroup);
        keepOneSelected(roomTypeGroup);

        residentsSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 20, 1));
        openSpotsSpinner.setValueFactory(new IntegerSpinnerValueFactory(1, 20, 1));
        leaseSpinner.setValueFactory(new IntegerSpinnerValueFactory(1, 36, 12));

        backButton.setGraphic(Icons.of("chevron-left", 16, "icon-brand"));
        privacyIcon.getChildren().add(Icons.of("lock", 16, "icon-brand"));
        dropIcon.getChildren().add(Icons.of("upload", 26, "icon-brand"));

        availableFromPicker.setValue(LocalDate.now());
        availableUntilPicker.setValue(LocalDate.now().plusMonths(3));
        disableDatesBefore(availableFromPicker, LocalDate.now());
        availableFromPicker.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                disableDatesBefore(availableUntilPicker, newValue.plusDays(1));
                if (availableUntilPicker.getValue() != null && !availableUntilPicker.getValue().isAfter(newValue)) {
                    availableUntilPicker.setValue(newValue.plusMonths(1));
                }
            }
        });
        disableDatesBefore(availableUntilPicker, LocalDate.now().plusDays(1));

        for (TextField field : new TextField[] {rentField, wifiField, electricityField, laundryField}) {
            Inputs.allowMoney(field);
        }
        Inputs.allowWholeNumbers(minAgeField, 2);
        Inputs.allowWholeNumbers(maxAgeField, 2);
        titleField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.length() > MAX_TITLE_LENGTH) {
                titleField.setText(oldValue);
            }
        });

        typeGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> updateTypeSpecificFields());
        setUpPhotoDrop();

        // Any change refreshes the preview, cost total and checklist.
        previewDelay.setOnFinished(event -> refreshPreview());
        watch(titleField.textProperty(), typeGroup.selectedToggleProperty(), locationBox.valueProperty(),
                roomTypeGroup.selectedToggleProperty(), residentsSpinner.valueProperty(), openSpotsSpinner.valueProperty(),
                availableFromPicker.valueProperty(), availableUntilPicker.valueProperty(), leaseSpinner.valueProperty(),
                rentField.textProperty(), wifiField.textProperty(), electricityField.textProperty(),
                laundryField.textProperty(), descriptionArea.textProperty(), petFriendlyBox.selectedProperty(),
                smokingAllowedBox.selectedProperty(), genderPreferenceBox.valueProperty(), minAgeField.textProperty(),
                maxAgeField.textProperty());

        updateTypeSpecificFields();
        renderPhotos();
        refreshPreview();
    }

    /** Called right after loading. {@code toEdit} is null for a brand-new post. */
    public void init(ShellController shell, Listing toEdit) {
        this.shell = shell;
        this.editing = toEdit;
        if (toEdit != null) {
            pageTitle.setText("Edit your listing");
            pageSubtitle.setText("Update anything that has changed. People who already messaged you keep their conversation.");
            publishButton.setText("Save changes");
            fillFrom(toEdit);
        }
    }

    private void watch(Observable... values) {
        for (Observable value : values) {
            value.addListener(obs -> previewDelay.playFromStart());
        }
    }

    private static void keepOneSelected(ToggleGroup group) {
        group.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null && oldValue != null) {
                oldValue.setSelected(true);
            }
        });
    }

    private void fillFrom(Listing listing) {
        titleField.setText(listing.getTitle());
        locationBox.setValue(listing.getLocation());
        (listing.getRoomType() == RoomType.SHARED ? sharedToggle : privateToggle).setSelected(true);
        residentsSpinner.getValueFactory().setValue(listing.getCurrentResidents());
        openSpotsSpinner.getValueFactory().setValue(listing.getOpenSpots());
        // Existing posts may have a start date in the past; let the owner keep it.
        disableDatesBefore(availableFromPicker, null);
        availableFromPicker.setValue(listing.getAvailableFrom());
        wifiField.setText(amount(listing.getWifiCost()));
        electricityField.setText(amount(listing.getElectricityCost()));
        laundryField.setText(amount(listing.getLaundryCost()));
        descriptionArea.setText(listing.getDescription());

        if (listing instanceof PermanentListing permanent) {
            permanentRadio.setSelected(true);
            rentField.setText(amount(permanent.getMonthlyRent()));
            leaseSpinner.getValueFactory().setValue(permanent.getMinLeaseMonths());
        } else if (listing instanceof TemporaryListing temporary) {
            temporaryRadio.setSelected(true);
            rentField.setText(amount(temporary.getDailyRate()));
            availableUntilPicker.setValue(temporary.getAvailableUntil());
        }

        HouseholdPreferences prefs = listing.getPreferences();
        petFriendlyBox.setSelected(prefs.isPetFriendly());
        smokingAllowedBox.setSelected(prefs.isSmokingAllowed());
        genderPreferenceBox.setValue(prefs.getGenderPreference());
        minAgeField.setText(prefs.getMinAge() > 0 ? String.valueOf(prefs.getMinAge()) : "");
        maxAgeField.setText(prefs.getMaxAge() > 0 ? String.valueOf(prefs.getMaxAge()) : "");

        photoPaths.clear();
        photoPaths.addAll(listing.getPhotoPaths());
        renderPhotos();
        refreshPreview();
    }

    private static String amount(double value) {
        if (value == 0) {
            return "";
        }
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.US, "%.2f", value);
    }

    private boolean isTemporary() {
        return temporaryRadio.isSelected();
    }

    private RoomType roomType() {
        return sharedToggle.isSelected() ? RoomType.SHARED : RoomType.PRIVATE;
    }

    private void updateTypeSpecificFields() {
        boolean temporary = isTemporary();
        untilBox.setVisible(temporary);
        untilBox.setManaged(temporary);
        leaseBox.setVisible(!temporary);
        leaseBox.setManaged(!temporary);
        rentLabel.setText(temporary ? "Daily rate" : "Monthly rent");
        rentUnit.setText(temporary ? "/ day" : "/ month");
        availabilityHint.setText(temporary ? "The dates the room is free." : "When someone can move in.");
    }

    // ---------------------------------------------------------------- live preview

    private void refreshPreview() {
        previewHolder.getChildren().setAll(ListingCard.create(buildListing(true), Session.getCurrentUser(),
                listing -> { }, PREVIEW_WIDTH));
        updateCostSummary();
        updateChecklist();
    }

    private void updateCostSummary() {
        Double rent = Inputs.parseMoney(rentField);
        double utilities = valueOrZero(wifiField) + valueOrZero(electricityField) + valueOrZero(laundryField);
        if (rent == null) {
            costSummaryLabel.setText("Enter the rent to see what each person pays in total.");
            return;
        }
        if (isTemporary()) {
            double perDay = rent + utilities / 30.0;
            String text = "Each person pays " + Format.money(round(perDay)) + " a day: daily rate " + Format.money(rent)
                    + " + utilities " + Format.money(round(utilities / 30.0)) + ".";
            LocalDate from = availableFromPicker.getValue();
            LocalDate until = availableUntilPicker.getValue();
            if (from != null && until != null && until.isAfter(from)) {
                long days = ChronoUnit.DAYS.between(from, until);
                text += "\nFull stay of " + Format.plural((int) days, "day") + ": " + Format.money(round(perDay * days));
            }
            costSummaryLabel.setText(text);
        } else {
            costSummaryLabel.setText("Each person pays " + Format.money(rent + utilities) + " a month: rent "
                    + Format.money(rent) + " + utilities " + Format.money(utilities) + ".");
        }
    }

    private void updateChecklist() {
        Double rent = Inputs.parseMoney(rentField);
        checklistBox.getChildren().setAll(
                checkItem("A title", !titleField.getText().isBlank(), true),
                checkItem("A neighborhood", locationBox.getValue() != null, true),
                checkItem(isTemporary() ? "A daily rate" : "The monthly rent", rent != null && rent > 0, true),
                checkItem("A description (" + MIN_DESCRIPTION_LENGTH + "+ characters)",
                        descriptionArea.getText().trim().length() >= MIN_DESCRIPTION_LENGTH, true),
                checkItem("Photos (recommended)", !photoPaths.isEmpty(), false));
        int length = titleField.getText().length();
        titleCounter.setText(length + " / " + MAX_TITLE_LENGTH);
        int description = descriptionArea.getText().trim().length();
        descriptionCounter.setText(description < MIN_DESCRIPTION_LENGTH
                ? (MIN_DESCRIPTION_LENGTH - description) + " more characters needed"
                : Format.plural(description, "character"));
    }

    private static HBox checkItem(String text, boolean done, boolean required) {
        StackPane mark = new StackPane();
        mark.getStyleClass().addAll("check-mark", done ? "check-done" : required ? "check-todo" : "check-optional");
        if (done) {
            mark.getChildren().add(Icons.of("check", 11, "icon-on-brand"));
        }
        Label label = new Label(text);
        label.getStyleClass().add(done ? "check-text-done" : "check-text");
        HBox row = new HBox(10, mark, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private static double valueOrZero(TextField field) {
        Double value = Inputs.parseMoney(field);
        return value == null ? 0 : value;
    }

    // ---------------------------------------------------------------- photos

    private void setUpPhotoDrop() {
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles() && photoPaths.size() < MAX_PHOTOS) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        dropZone.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                dropZone.getStyleClass().add("drag-over");
            }
        });
        dropZone.setOnDragExited(event -> dropZone.getStyleClass().remove("drag-over"));
        dropZone.setOnDragDropped(event -> {
            Dragboard board = event.getDragboard();
            boolean added = board.hasFiles() && addFiles(board.getFiles());
            event.setDropCompleted(added);
            event.consume();
        });
    }

    @FXML
    private void addPhotos() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose photos of the room and shared spaces");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        List<File> files = chooser.showOpenMultipleDialog(Navigator.getStage());
        if (files != null) {
            addFiles(files);
        }
    }

    /** Adds the image files among {@code files}; returns whether any were added. */
    private boolean addFiles(List<File> files) {
        boolean added = false;
        for (File file : files) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (!name.matches(".*\\.(png|jpe?g|gif|bmp)$")) {
                continue;
            }
            if (photoPaths.size() >= MAX_PHOTOS) {
                Inputs.showMessage(errorLabel, "You can add up to " + MAX_PHOTOS + " photos.");
                break;
            }
            if (!photoPaths.contains(file.getAbsolutePath())) {
                photoPaths.add(file.getAbsolutePath());
                added = true;
            }
        }
        renderPhotos();
        previewDelay.playFromStart();
        return added;
    }

    private void renderPhotos() {
        photosPane.getChildren().clear();
        for (int i = 0; i < photoPaths.size(); i++) {
            String path = photoPaths.get(i);
            StackPane thumb = Photos.box(path, "Photo", 160, 108, 10);
            thumb.getStyleClass().add("photo-thumb");

            Button remove = new Button();
            remove.setGraphic(Icons.of("x", 12, "icon-on-brand"));
            remove.getStyleClass().add("photo-remove");
            remove.setOnAction(event -> {
                photoPaths.remove(path);
                renderPhotos();
                previewDelay.playFromStart();
            });
            StackPane.setAlignment(remove, Pos.TOP_RIGHT);
            StackPane.setMargin(remove, new Insets(6));
            thumb.getChildren().add(remove);

            Label corner;
            if (i == 0) {
                corner = new Label("Cover");
                corner.getStyleClass().addAll("photo-pill", "pill-cover");
            } else {
                Button makeCover = new Button("Make cover");
                makeCover.getStyleClass().add("make-cover");
                makeCover.setOnAction(event -> {
                    photoPaths.remove(path);
                    photoPaths.add(0, path);
                    renderPhotos();
                    previewDelay.playFromStart();
                });
                StackPane.setAlignment(makeCover, Pos.BOTTOM_LEFT);
                StackPane.setMargin(makeCover, new Insets(6));
                thumb.getChildren().add(makeCover);
                corner = null;
            }
            if (corner != null) {
                StackPane.setAlignment(corner, Pos.BOTTOM_LEFT);
                StackPane.setMargin(corner, new Insets(8));
                thumb.getChildren().add(corner);
            }
            photosPane.getChildren().add(thumb);
        }
        boolean full = photoPaths.size() >= MAX_PHOTOS;
        addPhotosButton.setDisable(full);
        photoHint.setText(full ? "That's the maximum of " + MAX_PHOTOS + " photos."
                : photoPaths.isEmpty() ? "JPG or PNG · up to " + MAX_PHOTOS + " photos"
                : photoPaths.size() + " of " + MAX_PHOTOS + " added · drag more in any time");
    }

    // ---------------------------------------------------------------- saving

    @FXML
    private void publish() {
        List<String> errors = validate();
        if (!errors.isEmpty()) {
            Inputs.showErrors(errorLabel, errors);
            return;
        }
        Inputs.hide(errorLabel);

        List<String> storedPhotos = new ArrayList<>();
        DataStore store = DataStore.getInstance();
        for (String path : photoPaths) {
            if (store.isStoredPhoto(path)) {
                storedPhotos.add(path);
            } else {
                try {
                    storedPhotos.add(store.importPhoto(new File(path)));
                } catch (IOException e) {
                    Inputs.showMessage(errorLabel, "Could not copy the photo " + new File(path).getName() + ": " + e.getMessage());
                    return;
                }
            }
        }

        Listing listing = buildListing(false);
        listing.setPhotoPaths(storedPhotos);
        store.saveListing(listing);
        shell.showDetails(listing, editing == null
                ? "Your listing is live! People can now find it in Explore."
                : "Your changes have been saved.");
    }

    @FXML
    private void cancel() {
        if (editing != null) {
            shell.showDetails(editing);
        } else {
            shell.goBack();
        }
    }

    private List<String> validate() {
        List<String> errors = new ArrayList<>();
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            errors.add("Give your listing a title.");
        } else if (title.length() > MAX_TITLE_LENGTH) {
            errors.add("Keep the title under " + MAX_TITLE_LENGTH + " characters.");
        }
        if (locationBox.getValue() == null) {
            errors.add("Choose the neighborhood.");
        }
        Double rent = Inputs.parseMoney(rentField);
        if (rent == null || rent <= 0) {
            errors.add(isTemporary() ? "Enter the daily rate." : "Enter the monthly rent.");
        }
        LocalDate from = availableFromPicker.getValue();
        if (from == null) {
            errors.add("Choose the date the room is available from.");
        } else if (editing == null && from.isBefore(LocalDate.now())) {
            errors.add("The available-from date can't be in the past.");
        }
        if (isTemporary()) {
            LocalDate until = availableUntilPicker.getValue();
            if (until == null) {
                errors.add("Choose the date the temporary stay ends.");
            } else if (from != null && !until.isAfter(from)) {
                errors.add("The end date must be after the start date.");
            }
        }
        if (descriptionArea.getText().trim().length() < MIN_DESCRIPTION_LENGTH) {
            errors.add("Describe the household in a sentence or two (at least " + MIN_DESCRIPTION_LENGTH + " characters).");
        }
        Integer minAge = Inputs.parseWholeNumber(minAgeField);
        Integer maxAge = Inputs.parseWholeNumber(maxAgeField);
        if ((minAge != null && (minAge < 18 || minAge > 99)) || (maxAge != null && (maxAge < 18 || maxAge > 99))) {
            errors.add("Age preferences must be between 18 and 99.");
        } else if (minAge != null && maxAge != null && minAge > maxAge) {
            errors.add("Min age can't be greater than max age.");
        }
        return errors;
    }

    /**
     * Builds a listing from the form. For the preview, blanks get placeholder text instead of
     * failing, since the user is still filling the form in.
     */
    private Listing buildListing(boolean preview) {
        User owner = Session.getCurrentUser();
        boolean keepIdentity = editing != null && !preview;
        Listing listing;
        if (isTemporary()) {
            TemporaryListing temporary = keepIdentity
                    ? new TemporaryListing(editing.getId(), editing.getOwnerEmail(), editing.getCreatedAt())
                    : new TemporaryListing(owner.getEmail());
            temporary.setDailyRate(valueOrZero(rentField));
            LocalDate until = availableUntilPicker.getValue();
            temporary.setAvailableUntil(until != null ? until : LocalDate.now().plusMonths(1));
            listing = temporary;
        } else {
            PermanentListing permanent = keepIdentity
                    ? new PermanentListing(editing.getId(), editing.getOwnerEmail(), editing.getCreatedAt())
                    : new PermanentListing(owner.getEmail());
            permanent.setMonthlyRent(valueOrZero(rentField));
            permanent.setMinLeaseMonths(leaseSpinner.getValue());
            listing = permanent;
        }
        String title = titleField.getText().trim();
        listing.setTitle(preview && title.isEmpty() ? "Your listing title" : title);
        String location = locationBox.getValue();
        listing.setLocation(preview && location == null ? "Neighborhood" : location);
        listing.setRoomType(roomType());
        listing.setCurrentResidents(residentsSpinner.getValue());
        listing.setOpenSpots(openSpotsSpinner.getValue());
        LocalDate from = availableFromPicker.getValue();
        listing.setAvailableFrom(preview && from == null ? LocalDate.now() : from);
        listing.setWifiCost(valueOrZero(wifiField));
        listing.setElectricityCost(valueOrZero(electricityField));
        listing.setLaundryCost(valueOrZero(laundryField));
        listing.setDescription(descriptionArea.getText().trim());
        if (preview) {
            listing.setPhotoPaths(new ArrayList<>(photoPaths));
        }

        HouseholdPreferences prefs = new HouseholdPreferences();
        prefs.setPetFriendly(petFriendlyBox.isSelected());
        prefs.setSmokingAllowed(smokingAllowedBox.isSelected());
        prefs.setGenderPreference(genderPreferenceBox.getValue());
        Integer minAge = Inputs.parseWholeNumber(minAgeField);
        Integer maxAge = Inputs.parseWholeNumber(maxAgeField);
        prefs.setMinAge(minAge == null ? 0 : minAge);
        prefs.setMaxAge(maxAge == null ? 0 : maxAge);
        listing.setPreferences(prefs);
        return listing;
    }

    /** Greys out calendar days before {@code earliest} (or none, if null). */
    private static void disableDatesBefore(DatePicker picker, LocalDate earliest) {
        picker.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || (earliest != null && date.isBefore(earliest)));
            }
        });
    }
}
