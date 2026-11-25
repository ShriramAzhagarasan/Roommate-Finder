package controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import filter.CompositeFilter;
import filter.Filter;
import filter.ListingFilters;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.AccommodationType;
import model.Listing;
import model.Neighborhoods;
import model.RoomType;
import model.User;
import service.DataStore;
import service.Session;
import ui.Icons;
import ui.Inputs;
import ui.ListingCard;
import util.Format;

/**
 * The Explore page: every listing as a photo card. Search, the neighborhood picker, sorting and the
 * quick filter chips all update the results instantly; rarer filters live in the "More filters" panel.
 */
public class MainListingPageController {
    private static final String ANY_LOCATION = "All neighborhoods";

    private enum SortOption {
        NEWEST("Newest first", Comparator.comparing(Listing::getCreatedAt).reversed()),
        PRICE_LOW("Price: low to high", Comparator.comparingDouble(Listing::getMonthlyEquivalentCost)),
        PRICE_HIGH("Price: high to low", Comparator.comparingDouble(Listing::getMonthlyEquivalentCost).reversed()),
        SOONEST("Available soonest", Comparator.comparing(Listing::getAvailableFrom)),
        MOST_SPOTS("Most open spots", Comparator.comparingInt(Listing::getOpenSpots).reversed());

        private final String label;
        private final Comparator<Listing> comparator;

        SortOption(String label, Comparator<Listing> comparator) {
            this.label = label;
            this.comparator = comparator;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Cards are built a page at a time so hundreds of listings with photos stay fast. */
    private static final int PAGE_SIZE = 24;
    /** Cards never get narrower than this; the grid adds or removes columns instead. */
    private static final double MIN_CARD_WIDTH = 260;
    private static final double GRID_GAP = 20;
    private static final double GRID_PADDING = 32;

    @FXML private BorderPane rootPane;
    @FXML private HBox searchBox;
    @FXML private StackPane searchIconHolder;
    @FXML private TextField searchBar;
    @FXML private Button clearSearchButton;
    @FXML private ComboBox<String> locationFilter;
    @FXML private ComboBox<SortOption> sortBox;

    @FXML private ToggleGroup typeChips;
    @FXML private ToggleGroup roomChips;
    @FXML private ToggleButton permanentChip;
    @FXML private ToggleButton temporaryChip;
    @FXML private ToggleButton privateChip;
    @FXML private ToggleButton sharedChip;
    @FXML private ToggleButton petChip;
    @FXML private ToggleButton nonSmokingChip;
    @FXML private ToggleButton matchChip;
    @FXML private Button toggleFiltersButton;

    @FXML private ScrollPane listingsScroll;
    @FXML private Label resultsLabel;
    @FXML private Label resultsSubtitle;
    @FXML private Hyperlink clearAllLink;
    @FXML private FlowPane listingsPane;
    @FXML private Button showMoreButton;
    @FXML private VBox emptyState;
    @FXML private StackPane emptyIconHolder;

    @FXML private VBox filtersSidebar;
    @FXML private Button closeFiltersButton;
    @FXML private Label activeFiltersLabel;
    @FXML private TextField minBudgetFilter;
    @FXML private TextField maxBudgetFilter;
    @FXML private DatePicker moveInDateFilter;
    @FXML private DatePicker moveOutDateFilter;
    @FXML private VBox permanentDetails;
    @FXML private VBox temporaryDetails;
    @FXML private TextField maxUtilitiesFilter;
    @FXML private TextField maxDailyCostFilter;
    @FXML private Label filterErrorLabel;

    private ShellController shell;
    /** What the "More filters" panel last applied. The chips and search are read live on every render. */
    private CompositeFilter<Listing> panelFilters = new CompositeFilter<>(CompositeFilter.Mode.ALL);
    private List<Listing> currentResults = List.of();
    private double cardWidth = ListingCard.DEFAULT_WIDTH;

    @FXML
    public void initialize() {
        List<String> locations = new ArrayList<>();
        locations.add(ANY_LOCATION);
        locations.addAll(Neighborhoods.ALL);
        locationFilter.setItems(FXCollections.observableArrayList(locations));
        locationFilter.setValue(ANY_LOCATION);
        locationFilter.setVisibleRowCount(12);
        sortBox.setItems(FXCollections.observableArrayList(SortOption.values()));
        sortBox.setValue(SortOption.NEWEST);

        searchIconHolder.getChildren().add(Icons.of("search", 18, "icon-muted"));
        clearSearchButton.setGraphic(Icons.of("x", 14, "icon-muted"));
        closeFiltersButton.setGraphic(Icons.of("x", 18, "icon-muted"));
        toggleFiltersButton.setGraphic(Icons.of("sliders", 15, "icon-chip"));
        emptyIconHolder.getChildren().add(Icons.of("search", 30, "icon-brand"));
        searchBar.focusedProperty().addListener((obs, oldValue, focused) -> {
            searchBox.getStyleClass().remove("focused");
            if (focused) {
                searchBox.getStyleClass().add("focused");
            }
        });

        for (TextField field : new TextField[] {minBudgetFilter, maxBudgetFilter, maxUtilitiesFilter, maxDailyCostFilter}) {
            Inputs.allowMoney(field);
            field.setOnAction(event -> applyFilters());
        }
        moveInDateFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && moveOutDateFilter.getValue() != null && moveOutDateFilter.getValue().isBefore(newValue)) {
                moveOutDateFilter.setValue(null);
            }
        });

        // Everything outside the panel applies as soon as it changes.
        searchBar.textProperty().addListener((obs, oldValue, newValue) -> {
            boolean hasText = !newValue.isEmpty();
            clearSearchButton.setVisible(hasText);
            clearSearchButton.setManaged(hasText);
            render();
        });
        sortBox.valueProperty().addListener((obs, oldValue, newValue) -> render());
        locationFilter.valueProperty().addListener((obs, oldValue, newValue) -> render());
        for (ToggleButton chip : chips()) {
            chip.selectedProperty().addListener((obs, oldValue, newValue) -> {
                updatePanelSections();
                render();
            });
        }

        // The panel starts closed so the listings get the full width.
        rootPane.setRight(null);
        listingsScroll.viewportBoundsProperty().addListener((obs, oldValue, bounds) -> resizeCards(bounds.getWidth()));
        updatePanelSections();
    }

    public void setShell(ShellController shell) {
        this.shell = shell;
    }

    /** Re-reads listings from the data store, e.g. after a post was created or edited. */
    public void refresh() {
        render();
    }

    private ToggleButton[] chips() {
        return new ToggleButton[] {permanentChip, temporaryChip, privateChip, sharedChip, petChip, nonSmokingChip, matchChip};
    }

    // ---------------------------------------------------------------- actions

    @FXML
    private void toggleFilters() {
        rootPane.setRight(rootPane.getRight() == null ? filtersSidebar : null);
        updateFilterButton();
    }

    @FXML
    private void applyFilters() {
        CompositeFilter<Listing> built = buildPanelFilters();
        if (built != null) {
            panelFilters = built;
            render();
        }
    }

    @FXML
    private void clearFilters() {
        minBudgetFilter.clear();
        maxBudgetFilter.clear();
        moveInDateFilter.setValue(null);
        moveOutDateFilter.setValue(null);
        maxUtilitiesFilter.clear();
        maxDailyCostFilter.clear();
        Inputs.hide(filterErrorLabel);
        panelFilters = new CompositeFilter<>(CompositeFilter.Mode.ALL);
        render();
    }

    @FXML
    private void clearSearch() {
        searchBar.clear();
        searchBar.requestFocus();
    }

    @FXML
    private void clearAll() {
        searchBar.clear();
        locationFilter.setValue(ANY_LOCATION);
        for (ToggleButton chip : chips()) {
            chip.setSelected(false);
        }
        clearFilters();
    }

    /** The temporary-only and permanent-only filters only show when they can apply. */
    private void updatePanelSections() {
        boolean showPermanent = !temporaryChip.isSelected();
        boolean showTemporary = !permanentChip.isSelected();
        permanentDetails.setVisible(showPermanent);
        permanentDetails.setManaged(showPermanent);
        temporaryDetails.setVisible(showTemporary);
        temporaryDetails.setManaged(showTemporary);
    }

    // ---------------------------------------------------------------- filtering

    /** Turns the panel's inputs into one combined filter, or returns null if an input is invalid. */
    private CompositeFilter<Listing> buildPanelFilters() {
        List<String> errors = new ArrayList<>();
        Double minBudget = Inputs.parseMoney(minBudgetFilter);
        Double maxBudget = Inputs.parseMoney(maxBudgetFilter);
        if (minBudget != null && maxBudget != null && maxBudget < minBudget) {
            errors.add("Max budget must be greater than or equal to min budget.");
        }
        LocalDate moveIn = moveInDateFilter.getValue();
        LocalDate moveOut = moveOutDateFilter.getValue();
        if (moveIn != null && moveOut != null && !moveOut.isAfter(moveIn)) {
            errors.add("Move-out date must be after the move-in date.");
        }
        if (!errors.isEmpty()) {
            Inputs.showErrors(filterErrorLabel, errors);
            return null;
        }
        Inputs.hide(filterErrorLabel);

        CompositeFilter<Listing> filters = new CompositeFilter<>(CompositeFilter.Mode.ALL);
        if (minBudget != null || maxBudget != null) {
            filters.add(ListingFilters.budget(minBudget, maxBudget));
        }
        if (moveIn != null || moveOut != null) {
            filters.add(ListingFilters.availability(moveIn, moveOut));
        }
        Double maxUtilities = Inputs.parseMoney(maxUtilitiesFilter);
        if (maxUtilities != null) {
            filters.add(ListingFilters.maxMonthlyUtilities(maxUtilities));
        }
        Double maxDaily = Inputs.parseMoney(maxDailyCostFilter);
        if (maxDaily != null) {
            filters.add(ListingFilters.maxDailyCost(maxDaily));
        }
        return filters;
    }

    /** The chips and neighborhood picker as filters. */
    private CompositeFilter<Listing> quickFilters() {
        CompositeFilter<Listing> filters = new CompositeFilter<>(CompositeFilter.Mode.ALL);
        if (permanentChip.isSelected()) {
            filters.add(ListingFilters.type(AccommodationType.PERMANENT));
        } else if (temporaryChip.isSelected()) {
            filters.add(ListingFilters.type(AccommodationType.TEMPORARY));
        }
        if (privateChip.isSelected()) {
            filters.add(ListingFilters.roomType(RoomType.PRIVATE));
        } else if (sharedChip.isSelected()) {
            filters.add(ListingFilters.roomType(RoomType.SHARED));
        }
        if (petChip.isSelected()) {
            filters.add(ListingFilters.petFriendly());
        }
        if (nonSmokingChip.isSelected()) {
            filters.add(ListingFilters.nonSmoking());
        }
        if (matchChip.isSelected()) {
            filters.add(ListingFilters.compatibleWith(Session.getCurrentUser()));
        }
        if (!ANY_LOCATION.equals(locationFilter.getValue())) {
            filters.add(ListingFilters.location(locationFilter.getValue()));
        }
        return filters;
    }

    private void render() {
        CompositeFilter<Listing> quick = quickFilters();
        CompositeFilter<Listing> everything = new CompositeFilter<Listing>(CompositeFilter.Mode.ALL)
                .add(quick)
                .add(panelFilters)
                .add(ListingFilters.keyword(searchBar.getText()));

        List<Listing> results = Filter.applyTo(DataStore.getInstance().getAllListings(), everything);
        results.sort(sortBox.getValue().comparator);
        currentResults = results;

        List<Node> cards = new ArrayList<>();
        for (Listing listing : results.subList(0, Math.min(PAGE_SIZE, results.size()))) {
            cards.add(card(listing));
        }
        listingsPane.getChildren().setAll(cards);
        listingsScroll.setVvalue(0);
        updateShowMore();

        resultsLabel.setText(Format.plural(results.size(), "room"));
        resultsSubtitle.setText(subtitle(results));
        emptyState.setVisible(results.isEmpty());
        listingsScroll.setVisible(!results.isEmpty());

        int active = quick.criteriaCount() + panelFilters.criteriaCount()
                + (searchBar.getText().isBlank() ? 0 : 1);
        clearAllLink.setVisible(active > 0);
        clearAllLink.setManaged(active > 0);
        int inPanel = panelFilters.criteriaCount();
        activeFiltersLabel.setText(inPanel == 0 ? "None applied yet" : Format.plural(inPanel, "filter") + " applied");
        updateFilterButton();
    }

    private String subtitle(List<Listing> results) {
        if (results.isEmpty()) {
            return "";
        }
        if (!ANY_LOCATION.equals(locationFilter.getValue())) {
            return "in " + locationFilter.getValue();
        }
        long areas = results.stream().map(Listing::getLocation).distinct().count();
        return "across " + Format.plural((int) areas, "Boston-area neighborhood");
    }

    private void updateFilterButton() {
        int inPanel = panelFilters.criteriaCount();
        toggleFiltersButton.setText(inPanel == 0 ? "More filters" : "More filters  ·  " + inPanel);
        toggleFiltersButton.getStyleClass().remove("chip-on");
        if (inPanel > 0 || rootPane.getRight() != null) {
            toggleFiltersButton.getStyleClass().add("chip-on");
        }
    }

    // ---------------------------------------------------------------- grid

    private Node card(Listing listing) {
        User viewer = Session.getCurrentUser();
        return ListingCard.create(listing, viewer, shell::showDetails, cardWidth);
    }

    @FXML
    private void showMore() {
        int shown = listingsPane.getChildren().size();
        int end = Math.min(shown + PAGE_SIZE, currentResults.size());
        for (Listing listing : currentResults.subList(shown, end)) {
            listingsPane.getChildren().add(card(listing));
        }
        updateShowMore();
    }

    private void updateShowMore() {
        int shown = listingsPane.getChildren().size();
        int remaining = currentResults.size() - shown;
        showMoreButton.setVisible(remaining > 0);
        showMoreButton.setManaged(remaining > 0);
        showMoreButton.setText("Show " + Math.min(remaining, PAGE_SIZE) + " more rooms   ·   "
                + shown + " of " + currentResults.size());
    }

    /** Fits as many columns as the width allows, then stretches the cards to fill the row exactly. */
    private void resizeCards(double viewportWidth) {
        double available = viewportWidth - GRID_PADDING * 2;
        if (available <= 0) {
            return;
        }
        int columns = Math.max(1, (int) ((available + GRID_GAP) / (MIN_CARD_WIDTH + GRID_GAP)));
        double width = Math.floor((available - GRID_GAP * (columns - 1)) / columns);
        if (Math.abs(width - cardWidth) < 0.5) {
            return;
        }
        cardWidth = width;
        for (Node card : listingsPane.getChildren()) {
            ListingCard.setWidth((Region) card, width);
        }
    }
}
