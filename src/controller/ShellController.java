package controller;

import application.Navigator;
import application.Navigator.View;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.Listing;
import model.User;
import service.Session;
import ui.Avatar;
import ui.Icons;

/** The logged-in window: the top bar plus whichever page is showing below it. */
public class ShellController {

    @FXML private StackPane contentArea;
    @FXML private Button homeButton;
    @FXML private Button messagesButton;
    @FXML private Button myListingsButton;
    @FXML private Button newPostButton;
    @FXML private Button profileButton;

    // The listings page is kept so search text and filters survive navigating away and back.
    private Parent listingsView;
    private MainListingPageController listingsController;
    private Runnable returnToPreviousPage = this::showListings;
    private ContextMenu accountMenu;

    @FXML
    private void initialize() {
        newPostButton.setGraphic(Icons.of("plus", 16, "icon-on-brand"));
        refreshUserLabel();
    }

    /** Redraws the avatar and account menu, e.g. after the user changes their name. */
    public void refreshUserLabel() {
        User user = Session.getCurrentUser();
        HBox graphic = new HBox(6, Avatar.of(user.getName(), 34), Icons.of("chevron-down", 14, "icon-muted"));
        graphic.setAlignment(Pos.CENTER);
        profileButton.setGraphic(graphic);
        accountMenu = buildAccountMenu(user);
    }

    private ContextMenu buildAccountMenu(User user) {
        Label name = new Label(user.getName());
        name.getStyleClass().add("menu-name");
        Label email = new Label(user.getEmail());
        email.getStyleClass().add("menu-email");
        HBox header = new HBox(12, Avatar.of(user.getName(), 40), new VBox(2, name, email));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("menu-header");
        CustomMenuItem headerItem = new CustomMenuItem(header, false);
        headerItem.getStyleClass().add("menu-header-item");

        MenuItem profile = item("Your profile", "user", () -> showProfile(ProfileController.PROFILE_TAB));
        MenuItem listings = item("My listings", "grid", () -> showProfile(ProfileController.LISTINGS_TAB));
        MenuItem messages = item("Messages", "message", () -> showProfile(ProfileController.MESSAGES_TAB));
        MenuItem security = item("Password & security", "lock", () -> showProfile(ProfileController.SECURITY_TAB));
        MenuItem logOut = item("Log out", "logout", this::logOut);
        logOut.getStyleClass().add("menu-danger");

        ContextMenu menu = new ContextMenu(headerItem, new SeparatorMenuItem(), profile, listings, messages, security,
                new SeparatorMenuItem(), logOut);
        menu.getStyleClass().add("account-menu");
        // Line the menu up with the right edge of the avatar.
        menu.setOnShown(event -> {
            var bounds = profileButton.localToScreen(profileButton.getBoundsInLocal());
            menu.setX(bounds.getMaxX() - menu.getWidth() + 8);
        });
        return menu;
    }

    private static MenuItem item(String text, String icon, Runnable action) {
        MenuItem item = new MenuItem(text, Icons.of(icon, 16, "icon-menu"));
        item.setOnAction(event -> action.run());
        return item;
    }

    @FXML
    private void openAccountMenu() {
        if (accountMenu.isShowing()) {
            accountMenu.hide();
        } else {
            accountMenu.show(profileButton, Side.BOTTOM, 0, 8);
        }
    }

    @FXML
    public void showListings() {
        if (listingsView == null) {
            View<MainListingPageController> view = Navigator.load("MainListingPage.fxml");
            listingsView = view.root();
            listingsController = view.controller();
            listingsController.setShell(this);
        }
        listingsController.refresh();
        returnToPreviousPage = this::showListings;
        setContent(listingsView, homeButton);
    }

    @FXML
    private void showNewPost() {
        showPostEditor(null);
    }

    @FXML
    private void showMessages() {
        showProfile(ProfileController.MESSAGES_TAB);
    }

    @FXML
    private void showMyListings() {
        showProfile(ProfileController.LISTINGS_TAB);
    }

    /** Opens the post form, empty for a new post or filled in to edit an existing one. */
    public void showPostEditor(Listing toEdit) {
        View<NewPostController> view = Navigator.load("NewPost.fxml");
        view.controller().init(this, toEdit);
        setContent(view.root(), toEdit == null ? newPostButton : myListingsButton);
    }

    public void showProfile() {
        showProfile(ProfileController.PROFILE_TAB);
    }

    public void showProfile(int tab) {
        View<ProfileController> view = Navigator.load("Profile.fxml");
        setContent(view.root(), null);
        view.controller().init(this, tab);
    }

    /** Keeps "Back" pointing at the profile section the user was last looking at, and the top bar in step. */
    void rememberProfileTab(int tab) {
        returnToPreviousPage = () -> showProfile(tab);
        highlight(switch (tab) {
            case ProfileController.MESSAGES_TAB -> messagesButton;
            case ProfileController.LISTINGS_TAB -> myListingsButton;
            default -> profileButton;
        });
    }

    public void showDetails(Listing listing) {
        showDetails(listing, null);
    }

    public void showDetails(Listing listing, String bannerMessage) {
        View<ListingDetailsController> view = Navigator.load("ListingDetails.fxml");
        view.controller().init(this, listing, bannerMessage);
        setContent(view.root(), null);
    }

    /** Returns to the listings page or profile page the user came from. */
    public void goBack() {
        returnToPreviousPage.run();
    }

    @FXML
    void logOut() {
        Session.logout();
        Navigator.showWelcome();
    }

    private void setContent(Parent page, Button activeButton) {
        contentArea.getChildren().setAll(page);
        highlight(activeButton);
    }

    private void highlight(Button activeButton) {
        for (Button button : new Button[] {homeButton, messagesButton, myListingsButton, newPostButton, profileButton}) {
            button.getStyleClass().remove("active-tab");
        }
        if (activeButton != null) {
            activeButton.getStyleClass().add("active-tab");
        }
    }
}
