package ui;

import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Input restrictions and parsing for form fields. */
public final class Inputs {

    private Inputs() {
    }

    /** Only digits, at most {@code maxDigits} of them. */
    public static void allowWholeNumbers(TextField field, int maxDigits) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d{0," + maxDigits + "}")) {
                field.setText(oldValue);
            }
        });
    }

    /** A dollar amount: digits with an optional decimal point and up to two decimals. */
    public static void allowMoney(TextField field) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d{0,6}(\\.\\d{0,2})?")) {
                field.setText(oldValue);
            }
        });
    }

    /** The number in the field, or null if it is blank. */
    public static Integer parseWholeNumber(TextField field) {
        String text = field.getText().trim();
        return text.isEmpty() ? null : Integer.valueOf(text);
    }

    /** The amount in the field, or null if it is blank. */
    public static Double parseMoney(TextField field) {
        String text = field.getText().trim();
        if (text.isEmpty() || text.equals(".")) {
            return null;
        }
        return Double.valueOf(text);
    }

    public static void showErrors(Label label, List<String> errors) {
        if (errors.isEmpty()) {
            hide(label);
            return;
        }
        label.setText(errors.size() == 1 ? errors.get(0) : "• " + String.join("\n• ", errors));
        label.setVisible(true);
        label.setManaged(true);
    }

    public static void showMessage(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    public static void hide(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }
}
