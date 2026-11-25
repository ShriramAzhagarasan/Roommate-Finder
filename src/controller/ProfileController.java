package controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.Gender;
import model.Listing;
import model.Message;
import model.SleepSchedule;
import model.User;
import service.DataStore;
import service.Session;
import ui.Avatar;
import ui.CoverImage;
import ui.Dialogs;
import ui.Icons;
import ui.Inputs;
import ui.ListingCard;
import util.Format;

/** The account area: profile and lifestyle, the user's own listings, their conversations, and password. */
public class ProfileController {
    public static final int PROFILE_TAB = 0;
    public static final int LISTINGS_TAB = 1;
    public static final int MESSAGES_TAB = 2;
    public static final int SECURITY_TAB = 3;
    private static final int MAX_BIO_LENGTH = 500;

    @FXML private ToggleGroup sectionGroup;
    @FXML private ToggleButton navProfile;
    @FXML private ToggleButton navListings;
    @FXML private ToggleButton navMessages;
    @FXML private ToggleButton navSecurity;
    @FXML private Button logOutButton;
    @FXML private Node profileSection;
    @FXML private Node listingsSection;
    @FXML private Node messagesSection;
    @FXML private Node securitySection;

    @FXML private StackPane avatarHolder;
    @FXML private Label sidebarName;
    @FXML private Label sidebarEmail;
    @FXML private Label memberSinceLabel;
    @FXML private Label completenessValue;
    @FXML private ProgressBar completenessBar;
    @FXML private Label completenessHint;

    @FXML private TextField nameField;
    @FXML private Label emailLabel;
    @FXML private TextField ageField;
    @FXML private ComboBox<Gender> genderBox;
    @FXML private TextField phoneField;
    @FXML private ToggleGroup sleepGroup;
    @FXML private ToggleButton earlyBirdToggle;
    @FXML private ToggleButton nightOwlToggle;
    @FXML private ToggleButton flexibleToggle;
    @FXML private CheckBox smokerBox;
    @FXML private CheckBox petsBox;
    @FXML private TextArea bioArea;
    @FXML private Label bioCounter;
    @FXML private Label profileStatusLabel;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordStatusLabel;

    @FXML private Button newPostButton;
    @FXML private HBox statsRow;
    @FXML private VBox myListingsBox;

    @FXML private Label conversationCount;
    @FXML private VBox conversationList;
    @FXML private VBox threadPane;
    @FXML private HBox threadHeader;
    @FXML private ScrollPane threadScroll;
    @FXML private VBox threadBox;
    @FXML private TextArea replyArea;
    @FXML private Button sendButton;
    @FXML private VBox threadEmpty;
    @FXML private StackPane threadEmptyIcon;
    @FXML private Label threadEmptyTitle;
    @FXML private Label threadEmptyText;

    private ShellController shell;
    private List<Conversation> conversations = List.of();
    private Conversation openConversation;

    /** All messages between the user and one other person about one listing. */
    private record Conversation(String otherEmail, String otherName, String listingId, String listingTitle,
            List<Message> messages) {
        Message last() {
            return messages.get(messages.size() - 1);
        }
    }

    @FXML
    private void initialize() {
        genderBox.setItems(FXCollections.observableArrayList(Gender.values()));
        Inputs.allowWholeNumbers(ageField, 3);
        earlyBirdToggle.setUserData(SleepSchedule.EARLY_BIRD);
        nightOwlToggle.setUserData(SleepSchedule.NIGHT_OWL);
        flexibleToggle.setUserData(SleepSchedule.FLEXIBLE);

        navProfile.setGraphic(Icons.of("user", 18, "icon-nav"));
        navListings.setGraphic(Icons.of("grid", 18, "icon-nav"));
        navMessages.setGraphic(Icons.of("message", 18, "icon-nav"));
        navSecurity.setGraphic(Icons.of("lock", 18, "icon-nav"));
        logOutButton.setGraphic(Icons.of("logout", 18, "icon-danger"));
        newPostButton.setGraphic(Icons.of("plus", 16, "icon-on-brand"));
        threadEmptyIcon.getChildren().add(Icons.of("inbox", 30, "icon-brand"));
        sendButton.setGraphic(Icons.of("send", 15, "icon-on-brand"));

        // A section button can't be switched off by clicking it again.
        sectionGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                oldValue.setSelected(true);
            } else {
                showSection(sectionGroup.getToggles().indexOf(newValue));
            }
        });

        bioArea.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.length() > MAX_BIO_LENGTH) {
                bioArea.setText(oldValue);
            }
            bioCounter.setText(bioArea.getText().length() + " / " + MAX_BIO_LENGTH);
        });

        sendButton.disableProperty().bind(replyArea.textProperty().map(text -> text.trim().isEmpty()));
        replyArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && event.isShortcutDown()) {
                sendReply();
                event.consume();
            }
        });
    }

    public void init(ShellController shell, int tab) {
        this.shell = shell;
        fillProfile();
        renderMyListings();
        renderConversations();
        sectionGroup.selectToggle(sectionGroup.getToggles().get(tab));
        showSection(tab);
    }

    private void showSection(int index) {
        Node[] sections = {profileSection, listingsSection, messagesSection, securitySection};
        for (int i = 0; i < sections.length; i++) {
            sections[i].setVisible(i == index);
        }
        if (shell != null) {
            shell.rememberProfileTab(index);
        }
    }

    @FXML
    private void logOut() {
        shell.logOut();
    }

    // ---------------------------------------------------------------- sidebar

    private void renderSidebar() {
        User user = Session.getCurrentUser();
        avatarHolder.getChildren().setAll(Avatar.of(user.getName(), 84));
        sidebarName.setText(user.getName());
        sidebarEmail.setText(user.getEmail());
        memberSinceLabel.setText(user.getMemberSince() == null ? ""
                : "Member since " + Format.monthYear(user.getMemberSince()));

        // Each of these makes a profile more useful to the households you contact.
        List<String> missing = new ArrayList<>();
        if (user.getAge() <= 0) {
            missing.add("your age");
        }
        if (user.getGender() == null) {
            missing.add("your gender");
        }
        if (user.getSleepSchedule() == null) {
            missing.add("your sleep schedule");
        }
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            missing.add("a phone number");
        }
        if (user.getBio() == null || user.getBio().length() < 40) {
            missing.add("a short bio");
        }
        int total = 6;
        int done = total - missing.size();
        completenessBar.setProgress(done / (double) total);
        completenessValue.setText(Math.round(100.0 * done / total) + "%");
        completenessHint.setText(missing.isEmpty()
                ? "Your profile is complete. Households can see exactly who you are."
                : "Add " + missing.get(0) + " to stand out to households.");
    }

    // ---------------------------------------------------------------- profile

    private void fillProfile() {
        User user = Session.getCurrentUser();
        nameField.setText(user.getName());
        emailLabel.setText(user.getEmail());
        ageField.setText(user.getAge() > 0 ? String.valueOf(user.getAge()) : "");
        genderBox.setValue(user.getGender());
        phoneField.setText(user.getPhone());
        sleepGroup.getToggles().forEach(toggle -> toggle.setSelected(toggle.getUserData() == user.getSleepSchedule()));
        smokerBox.setSelected(user.isSmoker());
        petsBox.setSelected(user.hasPets());
        bioArea.setText(user.getBio() == null ? "" : user.getBio());
        renderSidebar();
    }

    @FXML
    private void discardProfile() {
        fillProfile();
        Inputs.hide(profileStatusLabel);
    }

    @FXML
    private void saveProfile() {
        List<String> errors = new ArrayList<>();
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            errors.add("Enter your full name.");
        }
        Integer age = Inputs.parseWholeNumber(ageField);
        if (age == null || age < 18 || age > 99) {
            errors.add("Enter an age between 18 and 99.");
        }
        if (!errors.isEmpty()) {
            setStatus(profileStatusLabel, String.join("\n", errors), false);
            return;
        }
        User user = Session.getCurrentUser();
        user.setName(name);
        user.setAge(age);
        user.setGender(genderBox.getValue());
        user.setPhone(phoneField.getText().trim());
        user.setSleepSchedule(sleepGroup.getSelectedToggle() == null ? null
                : (SleepSchedule) sleepGroup.getSelectedToggle().getUserData());
        user.setSmoker(smokerBox.isSelected());
        user.setHasPets(petsBox.isSelected());
        user.setBio(bioArea.getText().trim());
        DataStore.getInstance().save();
        shell.refreshUserLabel();
        renderSidebar();
        setStatus(profileStatusLabel, "Saved. Room matches now use your updated profile.", true);
    }

    @FXML
    private void changePassword() {
        String newPassword = newPasswordField.getText();
        if (newPassword.length() < RegisterController.MIN_PASSWORD_LENGTH) {
            setStatus(passwordStatusLabel, "New password must be at least " + RegisterController.MIN_PASSWORD_LENGTH
                    + " characters.", false);
            return;
        }
        if (!newPassword.equals(confirmPasswordField.getText())) {
            setStatus(passwordStatusLabel, "New passwords do not match.", false);
            return;
        }
        if (!DataStore.getInstance().changePassword(Session.getCurrentUser(), currentPasswordField.getText(), newPassword)) {
            setStatus(passwordStatusLabel, "Your current password is incorrect.", false);
            return;
        }
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        setStatus(passwordStatusLabel, "Password updated.", true);
    }

    private static void setStatus(Label label, String text, boolean success) {
        label.getStyleClass().removeAll("error-label", "success-label");
        label.getStyleClass().add(success ? "success-label" : "error-label");
        Inputs.showMessage(label, text);
    }

    // ---------------------------------------------------------------- my listings

    private void renderMyListings() {
        List<Listing> mine = DataStore.getInstance().getListingsByOwner(Session.getCurrentUser().getEmail());
        navListings.setText(mine.isEmpty() ? "My listings" : "My listings  (" + mine.size() + ")");

        long inquiries = mine.stream().mapToLong(DataStore.getInstance()::countInquiries).sum();
        int openSpots = mine.stream().mapToInt(Listing::getOpenSpots).sum();
        statsRow.getChildren().setAll(
                stat(String.valueOf(mine.size()), mine.size() == 1 ? "Active listing" : "Active listings", "grid"),
                stat(String.valueOf(inquiries), inquiries == 1 ? "Message received" : "Messages received", "message"),
                stat(String.valueOf(openSpots), openSpots == 1 ? "Open spot" : "Open spots", "users"));

        myListingsBox.getChildren().clear();
        if (mine.isEmpty()) {
            myListingsBox.getChildren().add(emptyCard("home", "You haven't posted a room yet",
                    "Have a spare room? A post takes about two minutes, and people can message you right away."));
            return;
        }
        for (Listing listing : mine) {
            myListingsBox.getChildren().add(listingRow(listing));
        }
    }

    private static VBox stat(String value, String caption, String icon) {
        Label number = new Label(value);
        number.getStyleClass().add("stat-number");
        Label text = new Label(caption);
        text.getStyleClass().add("stat-label");
        HBox top = new HBox(number);
        top.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(spacer, Icons.of(icon, 18, "icon-brand"));
        VBox tile = new VBox(2, top, text);
        tile.getStyleClass().add("stat-tile");
        HBox.setHgrow(tile, Priority.ALWAYS);
        tile.setMaxWidth(Double.MAX_VALUE);
        return tile;
    }

    private HBox listingRow(Listing listing) {
        String cover = listing.getPhotoPaths().isEmpty() ? null : listing.getPhotoPaths().get(0);
        CoverImage photo = new CoverImage(cover, listing.getLocation(), 10, false);
        photo.setMinSize(168, 112);
        photo.setPrefSize(168, 112);
        photo.setMaxSize(168, 112);

        Label title = new Label(listing.getTitle());
        title.getStyleClass().add("row-title");
        title.setWrapText(true);
        Label where = new Label(listing.getLocation() + "  ·  " + listing.getRoomType());
        where.getStyleClass().add("card-meta");
        HBox badges = new HBox(8, ListingCard.typeBadge(listing.getType()), where);
        badges.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(listing.getPriceLabel() + "   ·   " + listing.getAvailabilityLabel());
        price.getStyleClass().add("row-meta");
        long inquiries = DataStore.getInstance().countInquiries(listing);
        Label messages = new Label(Format.plural((int) inquiries, "message") + "  ·  "
                + Format.plural(listing.getOpenSpots(), "open spot"));
        messages.getStyleClass().add("row-hint");
        VBox info = new VBox(6, badges, title, price, messages);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button view = iconButton("View", "eye", () -> shell.showDetails(listing));
        Button edit = iconButton("Edit", "edit", () -> shell.showPostEditor(listing));
        Button delete = iconButton("Delete", "trash", () -> {
            if (Dialogs.confirm("Delete listing", "Delete \"" + listing.getTitle() + "\"? This can't be undone.", "Delete")) {
                DataStore.getInstance().deleteListing(listing);
                renderMyListings();
            }
        });
        delete.getStyleClass().add("row-action-danger");
        VBox actions = new VBox(6, view, edit, delete);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(18, photo, info, actions);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("card", "listing-row");
        return row;
    }

    private static Button iconButton(String text, String icon, Runnable action) {
        Button button = new Button(text, Icons.of(icon, 15, "icon-row"));
        button.getStyleClass().add("row-action");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> action.run());
        return button;
    }

    private static VBox emptyCard(String icon, String title, String text) {
        StackPane iconHolder = new StackPane(Icons.of(icon, 28, "icon-brand"));
        iconHolder.getStyleClass().add("empty-icon");
        Label heading = new Label(title);
        heading.getStyleClass().add("empty-title");
        Label body = new Label(text);
        body.getStyleClass().add("empty-text");
        body.setWrapText(true);
        body.setMaxWidth(380);
        VBox box = new VBox(10, iconHolder, heading, body);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().addAll("card", "empty-card");
        return box;
    }

    @FXML
    private void newPost() {
        shell.showPostEditor(null);
    }

    // ---------------------------------------------------------------- messages

    private void renderConversations() {
        User me = Session.getCurrentUser();
        Map<String, List<Message>> grouped = new LinkedHashMap<>();
        for (Message message : DataStore.getInstance().getMessagesInvolving(me.getEmail())) {
            boolean mine = message.fromEmail().equalsIgnoreCase(me.getEmail());
            String other = (mine ? message.toEmail() : message.fromEmail()).toLowerCase();
            grouped.computeIfAbsent(other + "|" + message.listingId(), key -> new ArrayList<>()).add(message);
        }
        List<Conversation> list = new ArrayList<>();
        for (List<Message> messages : grouped.values()) {
            Message first = messages.get(0);
            boolean mine = first.fromEmail().equalsIgnoreCase(me.getEmail());
            String otherEmail = mine ? first.toEmail() : first.fromEmail();
            String otherName = messages.stream().filter(m -> m.fromEmail().equalsIgnoreCase(otherEmail))
                    .map(Message::fromName).findFirst()
                    .orElseGet(() -> DataStore.getInstance().findUser(otherEmail).map(User::getName).orElse(otherEmail));
            list.add(new Conversation(otherEmail, otherName, first.listingId(), first.listingTitle(), messages));
        }
        list.sort(Comparator.comparing((Conversation c) -> c.last().sentAt()).reversed());
        conversations = list;

        int received = DataStore.getInstance().getInbox(me.getEmail()).size();
        navMessages.setText(received == 0 ? "Messages" : "Messages  (" + received + ")");
        conversationCount.setText(list.isEmpty() ? "No conversations yet" : Format.plural(list.size(), "conversation"));

        conversationList.getChildren().clear();
        for (Conversation conversation : list) {
            conversationList.getChildren().add(conversationItem(conversation));
        }
        if (list.isEmpty()) {
            threadEmptyTitle.setText("No messages yet");
            threadEmptyText.setText("When someone asks about one of your rooms, or you contact a household, "
                    + "the conversation shows up here.");
        } else {
            openConversation(openConversation == null ? list.get(0)
                    : list.stream().filter(c -> sameConversation(c, openConversation)).findFirst().orElse(list.get(0)));
        }
    }

    private static boolean sameConversation(Conversation a, Conversation b) {
        return a.otherEmail().equalsIgnoreCase(b.otherEmail()) && a.listingId().equals(b.listingId());
    }

    private Node conversationItem(Conversation conversation) {
        Label name = new Label(conversation.otherName());
        name.getStyleClass().add("conversation-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label time = new Label(Format.relative(conversation.last().sentAt()));
        time.getStyleClass().add("conversation-time");
        HBox top = new HBox(6, name, spacer, time);
        top.setAlignment(Pos.CENTER_LEFT);

        Label about = new Label(conversation.listingTitle());
        about.getStyleClass().add("conversation-about");
        boolean lastIsMine = conversation.last().fromEmail().equalsIgnoreCase(Session.getCurrentUser().getEmail());
        Label snippet = new Label((lastIsMine ? "You: " : "") + conversation.last().text().replace('\n', ' '));
        snippet.getStyleClass().add("conversation-snippet");

        VBox text = new VBox(2, top, about, snippet);
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox item = new HBox(12, Avatar.of(conversation.otherName(), 40), text);
        item.setAlignment(Pos.TOP_LEFT);
        item.getStyleClass().add("conversation-item");
        item.setUserData(conversation);
        item.setOnMouseClicked(event -> openConversation(conversation));
        return item;
    }

    private void openConversation(Conversation conversation) {
        openConversation = conversation;
        for (Node item : conversationList.getChildren()) {
            item.getStyleClass().remove("selected");
            if (item.getUserData() instanceof Conversation c && sameConversation(c, conversation)) {
                item.getStyleClass().add("selected");
            }
        }
        threadPane.setVisible(true);
        threadEmpty.setVisible(false);

        // Header: who, and which listing it's about
        Label name = new Label(conversation.otherName());
        name.getStyleClass().add("thread-name");
        Optional<Listing> listing = DataStore.getInstance().findListing(conversation.listingId());
        Hyperlink about = new Hyperlink(listing.isPresent() ? conversation.listingTitle()
                : conversation.listingTitle() + " (listing removed)");
        about.getStyleClass().add("thread-about");
        about.setPadding(javafx.geometry.Insets.EMPTY);
        listing.ifPresentOrElse(l -> about.setOnAction(event -> shell.showDetails(l)), () -> about.setDisable(true));
        Label email = new Label(conversation.otherEmail());
        email.getStyleClass().add("thread-email");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        threadHeader.getChildren().setAll(Avatar.of(conversation.otherName(), 44), new VBox(2, name, about), spacer, email);

        // Bubbles, grouped under a date whenever the day changes
        String me = Session.getCurrentUser().getEmail();
        threadBox.getChildren().clear();
        java.time.LocalDate lastDay = null;
        for (Message message : conversation.messages()) {
            java.time.LocalDate day = message.sentAt().toLocalDate();
            if (!day.equals(lastDay)) {
                Label date = new Label(Format.date(day));
                date.getStyleClass().add("thread-date");
                HBox dateRow = new HBox(date);
                dateRow.setAlignment(Pos.CENTER);
                threadBox.getChildren().add(dateRow);
                lastDay = day;
            }
            threadBox.getChildren().add(bubble(message, message.fromEmail().equalsIgnoreCase(me)));
        }
        replyArea.clear();
        Platform.runLater(() -> {
            threadScroll.layout();
            threadScroll.setVvalue(1.0);
        });
    }

    private static Node bubble(Message message, boolean mine) {
        Label text = new Label(message.text());
        text.setWrapText(true);
        text.getStyleClass().add("bubble-text");
        Label time = new Label(Format.time(message.sentAt()));
        time.getStyleClass().add("bubble-time");
        VBox bubble = new VBox(4, text, time);
        bubble.getStyleClass().addAll("bubble", mine ? "bubble-mine" : "bubble-theirs");
        bubble.setMaxWidth(460);
        HBox row = new HBox(bubble);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    @FXML
    private void sendReply() {
        String text = replyArea.getText().trim();
        if (text.isEmpty() || openConversation == null) {
            return;
        }
        User me = Session.getCurrentUser();
        DataStore.getInstance().sendMessage(new Message(me.getEmail(), me.getName(), openConversation.otherEmail(),
                openConversation.listingId(), openConversation.listingTitle(), text, LocalDateTime.now()));
        replyArea.clear();
        renderConversations();
    }
}
