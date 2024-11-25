package controller;

import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.time.LocalDate;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class MainListingPageController {

    @FXML
    private BorderPane rootPane; // The main BorderPane

    @FXML
    private TextField searchBar;

    @FXML
    private VBox filtersSidebar; // The VBox for filters

    @FXML
    private Button toggleFiltersButton; // The toggle button

    @FXML
    private ImageView filterIcon; // The icon image for the button

    private boolean filtersVisible = false; // Tracks whether the filters are visible

    @FXML
    private DatePicker moveInDateFilter;

    @FXML
    private DatePicker moveOutDateFilter;

    @FXML
    private ComboBox<String> locationFilter; // Dropdown for location

    @FXML
    private ComboBox<String> roomTypeFilter;

    @FXML
    private TextField minBudgetFilter;

    @FXML
    private TextField maxBudgetFilter;

    @FXML
    private ComboBox<String> typeFilter;

    @FXML
    private GridPane listingsGrid;

    @FXML
    private Button homeButton;

    @FXML
    private Button NewPostButton;

    @FXML
    private Button profileButton;
    
    @FXML
    private Button logOutButton;

    @FXML
    public void initialize() {
        System.out.println("Controller initialized!");

        // Populate Location Filter
        locationFilter.setItems(FXCollections.observableArrayList(
            "Back Bay",
            "Huntington Avenue",
            "Newbury Street",
            "Roxbury",
            "Dorchester",
            "Allston",
            "Brookline",
            "Mission Main",
            "Jamaica Plain",
            "Fenway-Kenmore"
        ));

        // Set default button highlighting
        highlightButton(homeButton); // Highlight "Home" as default

        // Add click listeners for top bar buttons
        homeButton.setOnAction(event -> highlightButton(homeButton));
        NewPostButton.setOnAction(event -> highlightButton(NewPostButton));
        profileButton.setOnAction(event -> highlightButton(profileButton));
        logOutButton.setOnAction(event -> {
            highlightButton(logOutButton);
            logOut();
        });

        // Load the filter icon for the toggle button
        filterIcon.setImage(new Image(getClass().getResourceAsStream("/images/filter-icon.png")));

        // Initially, hide the filters
        rootPane.setRight(null);
        

        // Validate numeric input for budget fields
        addNumericValidation(minBudgetFilter);
        addNumericValidation(maxBudgetFilter);

        // Set toggle functionality
        toggleFiltersButton.setOnAction(event -> toggleFilters());

        // Set default behavior on date selection
        moveInDateFilter.setOnAction(event -> {
            System.out.println("Move-in Date Selected: " + moveInDateFilter.getValue());
        });

        moveOutDateFilter.setOnAction(event -> {
            System.out.println("Move-out Date Selected: " + moveOutDateFilter.getValue());
        });

        // Populate filters
        typeFilter.setItems(FXCollections.observableArrayList("Temporary", "Permanent"));
        roomTypeFilter.setItems(FXCollections.observableArrayList("Shared", "Private"));

        // Optional: Add event listener for room type selection
        roomTypeFilter.setOnAction(event -> {
            String selectedRoomType = roomTypeFilter.getValue();
            System.out.println("Selected Room Type: " + selectedRoomType);
        });

        // Load sample listings
        loadListings();
        
     // Log Out button functionality
        logOutButton.setOnAction(event -> logOut());
    }

    private void toggleFilters() {
        if (filtersVisible) {
            // Hide filters
            rootPane.setRight(null); // Remove the filters sidebar
            filtersSidebar.setVisible(false);
            filtersSidebar.setManaged(false);
        } else {
            // Show filters
            rootPane.setRight(filtersSidebar); // Add the filters sidebar
            filtersSidebar.setVisible(true);
            filtersSidebar.setManaged(true);
        }
        filtersVisible = !filtersVisible; // Toggle state
    }

    private void highlightButton(Button selectedButton) {
        // Reset styles for all buttons
        homeButton.getStyleClass().remove("active-tab");
        NewPostButton.getStyleClass().remove("active-tab");
        profileButton.getStyleClass().remove("active-tab");
        logOutButton.getStyleClass().remove("active-tab");

        // Add style to the selected button
        selectedButton.getStyleClass().add("active-tab");
    }
    
    private void addNumericValidation(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("[^\\d]", "")); // Allow only digits
            }
        });
    }

    private void loadListings() {
        for (int i = 0; i < 10; i++) {
            javafx.scene.control.Label listing = new javafx.scene.control.Label("Listing " + (i + 1));
            listingsGrid.add(listing, i % 3, i / 3); // Add in a 3-column grid
        }
    }

    @FXML
    private void applyFilters() {
        try {
            // Validate Budget Fields
            String minBudgetStr = minBudgetFilter.getText();
            String maxBudgetStr = maxBudgetFilter.getText();

            if (!minBudgetStr.isEmpty() && !maxBudgetStr.isEmpty()) {
                int minBudget = Integer.parseInt(minBudgetStr);
                int maxBudget = Integer.parseInt(maxBudgetStr);

                if (maxBudget < minBudget) {
                	showErrorPopup("Budget Error", "Maximum budget must be greater than or equal to minimum budget.");
                    return;
                }
            }

            // Log all filter values for debugging
            String searchQuery = searchBar.getText(); // Search query text
            LocalDate moveInDate = moveInDateFilter.getValue(); // Move-in date
            LocalDate moveOutDate = moveOutDateFilter.getValue(); // Move-out date
            String location = locationFilter.getValue(); // Location filter (from ComboBox)
            String roomType = roomTypeFilter.getValue(); // Room type filter (Shared/Private)
            String accommodationType = typeFilter.getValue(); // Accommodation type (Temporary/Permanent)

            System.out.println("Applying filters...");
            System.out.println("Search Query: " + searchQuery);
            System.out.println("Move-in Date: " + moveInDate);
            System.out.println("Move-out Date: " + moveOutDate);
            System.out.println("Location: " + location);
            System.out.println("Room Type: " + roomType);
            System.out.println("Min Budget: " + minBudgetStr);
            System.out.println("Max Budget: " + maxBudgetStr);
            System.out.println("Accommodation Type: " + accommodationType);

            // TODO: Add filtering logic to update the listingsGrid based on these values
        } catch (NumberFormatException e) {
        	showErrorPopup("Invalid Input", "Please enter valid numeric values for the budget.");
        }
    }
    
    private void logOut() {
        System.out.println("Log Out button clicked.");
        // TODO: Add logic to handle log-out functionality, e.g., navigating to the login screen
    }
    
    private void showErrorPopup(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}

