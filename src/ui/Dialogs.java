package ui;

import java.util.Optional;

import application.Navigator;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;

/** Pop-up windows used across the app. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void showError(String title, String message) {
        alert(Alert.AlertType.ERROR, title, message).showAndWait();
    }

    public static void showInfo(String title, String message) {
        alert(Alert.AlertType.INFORMATION, title, message).showAndWait();
    }

    public static boolean confirm(String title, String message, String confirmText) {
        Alert alert = alert(Alert.AlertType.CONFIRMATION, title, message);
        ButtonType confirm = new ButtonType(confirmText, ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(confirm, ButtonType.CANCEL);
        return alert.showAndWait().filter(button -> button == confirm).isPresent();
    }

    /** Asks the user to write a message. Returns the trimmed text, or empty if they cancelled. */
    public static Optional<String> composeMessage(String title, String header, String initialText) {
        Dialog<String> dialog = new Dialog<>();
        prepare(dialog);
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        ButtonType send = new ButtonType("Send", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(send, ButtonType.CANCEL);

        TextArea text = new TextArea(initialText);
        text.setWrapText(true);
        text.setPrefSize(480, 180);
        dialog.getDialogPane().setContent(text);

        Node sendButton = dialog.getDialogPane().lookupButton(send);
        sendButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> text.getText().trim().isEmpty(), text.textProperty()));

        dialog.setResultConverter(button -> button == send ? text.getText().trim() : null);
        Platform.runLater(() -> {
            text.requestFocus();
            text.end();
        });
        return dialog.showAndWait();
    }

    private static Alert alert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        prepare(alert);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        return alert;
    }

    private static void prepare(Dialog<?> dialog) {
        if (Navigator.getStage() != null && Navigator.getStage().getScene() != null) {
            dialog.initOwner(Navigator.getStage());
        }
        dialog.getDialogPane().getStylesheets().add(Navigator.stylesheet());
    }
}
