package application;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

import controller.ShellController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Loads FXML screens and switches between the logged-out screens and the main app. */
public final class Navigator {
    private static Stage stage;
    private static Scene scene;

    /** A loaded FXML file together with its controller. */
    public record View<C>(Parent root, C controller) {
    }

    private Navigator() {
    }

    static void init(Stage primaryStage) {
        stage = primaryStage;
        scene = new Scene(new StackPane(), 1280, 820);
        scene.getStylesheets().add(stylesheet());
        stage.setScene(scene);
    }

    public static String stylesheet() {
        return Navigator.class.getResource("styles.css").toExternalForm();
    }

    public static Stage getStage() {
        return stage;
    }

    public static <C> View<C> load(String fxmlFile) {
        URL url = Navigator.class.getResource(fxmlFile);
        if (url == null) {
            throw new IllegalStateException("Missing screen file: " + fxmlFile);
        }
        FXMLLoader loader = new FXMLLoader(url);
        try {
            Parent root = loader.load();
            return new View<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + fxmlFile, e);
        }
    }

    public static void showWelcome() {
        scene.setRoot(load("Welcome.fxml").root());
    }

    public static void showLogin() {
        scene.setRoot(load("Login.fxml").root());
    }

    public static void showRegister() {
        scene.setRoot(load("Register.fxml").root());
    }

    public static void showMainApp() {
        View<ShellController> view = load("Shell.fxml");
        scene.setRoot(view.root());
        view.controller().showListings();
    }
}
