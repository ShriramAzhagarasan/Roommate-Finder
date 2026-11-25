package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import service.DataStore;
import ui.Dialogs;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> showUnexpectedError(error));
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> showUnexpectedError(error));

        loadFonts();
        DataStore.getInstance(); // load saved data (or create the demo data) before the first screen

        primaryStage.setTitle("Roommate Finder");
        primaryStage.setMinWidth(1080);
        primaryStage.setMinHeight(700);
        Navigator.init(primaryStage);
        Navigator.showWelcome();
        primaryStage.show();
    }

    /** Registers the bundled Inter font so the stylesheet can use it. */
    private static void loadFonts() {
        for (String weight : new String[] {"Regular", "SemiBold", "Bold"}) {
            String resource = "/fonts/Inter-" + weight + ".ttf";
            try (var in = Main.class.getResourceAsStream(resource)) {
                if (in == null || Font.loadFont(in, 13) == null) {
                    System.err.println("Could not load " + resource + "; falling back to the system font.");
                }
            } catch (java.io.IOException e) {
                System.err.println("Could not load " + resource + ": " + e.getMessage());
            }
        }
    }

    private static void showUnexpectedError(Throwable error) {
        error.printStackTrace();
        Throwable root = error;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage() != null ? root.getMessage() : root.toString();
        // Always defer: showAndWait() throws if called during a layout or animation pulse,
        // which is exactly when some UI errors happen.
        Platform.runLater(() -> Dialogs.showError("Something went wrong", message));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
