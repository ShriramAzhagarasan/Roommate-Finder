package ui;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;

/** A round avatar with a person's initials, coloured consistently from their name. */
public final class Avatar {
    private static final String[][] GRADIENTS = {
            {"#2e86de", "#54a0ff"}, {"#10ac84", "#1dd1a1"}, {"#ee5253", "#ff8a80"},
            {"#5f27cd", "#8e7cc3"}, {"#f39c12", "#feca57"}, {"#0a9396", "#48c9b0"}, {"#c0392b", "#e17055"}};

    private Avatar() {
    }

    public static StackPane of(String name, double size) {
        String[] colors = GRADIENTS[Math.floorMod(name == null ? 0 : name.hashCode(), GRADIENTS.length)];
        Circle circle = new Circle(size / 2);
        circle.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(colors[0])), new Stop(1, Color.web(colors[1]))));

        Label initials = new Label(initials(name));
        initials.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + Math.round(size * 0.38) + "px;");

        StackPane avatar = new StackPane(circle, initials);
        avatar.setMinSize(size, size);
        avatar.setPrefSize(size, size);
        avatar.setMaxSize(size, size);
        avatar.getStyleClass().add("avatar");
        return avatar;
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
