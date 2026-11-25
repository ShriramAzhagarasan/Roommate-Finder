package ui;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/** Shows listing photos cropped to fill a box, or a coloured placeholder when there is no photo. */
public final class Photos {
    private static final String[][] PLACEHOLDER_GRADIENTS = {
            {"#2e86de", "#54a0ff"}, {"#10ac84", "#1dd1a1"}, {"#ee5253", "#ff6b6b"},
            {"#5f27cd", "#8e7cc3"}, {"#ff9f43", "#feca57"}, {"#0abde3", "#48dbfb"}};
    private static final Map<String, Image> CACHE = new HashMap<>();

    private Photos() {
    }

    /** A photo box of exactly {@code width} x {@code height}. */
    public static StackPane box(String path, String placeholderText, double width, double height, double cornerRadius) {
        StackPane pane = new StackPane();
        pane.setMinSize(width, height);
        pane.setPrefSize(width, height);
        pane.setMaxSize(width, height);
        pane.getStyleClass().add("photo-box");

        Image image = path == null ? null : load(path, Math.max(width, height) * 2);
        if (image != null) {
            ImageView view = new ImageView(image);
            double scale = Math.max(width / image.getWidth(), height / image.getHeight());
            double viewportWidth = width / scale;
            double viewportHeight = height / scale;
            view.setViewport(new Rectangle2D((image.getWidth() - viewportWidth) / 2,
                    (image.getHeight() - viewportHeight) / 2, viewportWidth, viewportHeight));
            view.setFitWidth(width);
            view.setFitHeight(height);
            view.setSmooth(true);
            pane.getChildren().add(view);
        } else {
            String[] colors = placeholderColors(placeholderText);
            pane.setStyle("-fx-background-color: linear-gradient(to bottom right, " + colors[0] + ", " + colors[1] + ");");
            VBox text = new VBox(2);
            text.setAlignment(Pos.CENTER);
            Label main = new Label(placeholderText);
            main.getStyleClass().add("placeholder-title");
            main.setWrapText(true);
            text.getChildren().add(main);
            if (height >= 120) {
                Label sub = new Label(path == null ? "No photos yet" : "Photo unavailable");
                sub.getStyleClass().add("placeholder-subtitle");
                text.getChildren().add(sub);
            }
            pane.getChildren().add(text);
        }

        if (cornerRadius > 0) {
            Rectangle clip = new Rectangle(width, height);
            clip.setArcWidth(cornerRadius * 2);
            clip.setArcHeight(cornerRadius * 2);
            pane.setClip(clip);
        }
        return pane;
    }

    public static StackPane cover(List<String> photoPaths, String placeholderText, double width, double height,
            double cornerRadius) {
        return box(photoPaths.isEmpty() ? null : photoPaths.get(0), placeholderText, width, height, cornerRadius);
    }

    /** The two gradient colours used behind a missing photo, picked consistently from its label. */
    static String[] placeholderColors(String text) {
        return PLACEHOLDER_GRADIENTS[Math.floorMod(text == null ? 0 : text.hashCode(), PLACEHOLDER_GRADIENTS.length)];
    }

    static Image load(String path, double maxSize) {
        String key = path + "@" + (int) maxSize;
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        File file = new File(path);
        Image image = null;
        if (file.isFile()) {
            Image loaded = new Image(file.toURI().toString(), maxSize, maxSize, true, true, false);
            if (!loaded.isError() && loaded.getWidth() > 0) {
                image = loaded;
            }
        }
        CACHE.put(key, image);
        return image;
    }
}
