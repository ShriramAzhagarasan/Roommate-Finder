package ui;

import javafx.geometry.HPos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

/**
 * A photo that fills whatever size it is given, cropping the edges instead of stretching, with
 * rounded corners. Unlike {@link Photos#box}, it can grow and shrink with the layout, which is what
 * lets the listing grid resize its cards to fit the window.
 */
public class CoverImage extends Region {
    private final ImageView view = new ImageView();
    private final Rectangle clip = new Rectangle();
    private final Image image;
    private final Label placeholder;
    private final double radius;
    private final boolean roundTopOnly;

    /**
     * @param roundTopOnly round only the top corners, for a photo that sits flush at the top of a card
     */
    public CoverImage(String path, String placeholderText, double radius, boolean roundTopOnly) {
        this.radius = radius;
        this.roundTopOnly = roundTopOnly;
        getStyleClass().add("cover-image");
        image = path == null ? null : Photos.load(path, 720);
        if (image != null) {
            view.setImage(image);
            view.setSmooth(true);
            getChildren().add(view);
            placeholder = null;
        } else {
            String[] colors = Photos.placeholderColors(placeholderText);
            setStyle("-fx-background-color: linear-gradient(to bottom right, " + colors[0] + ", " + colors[1] + ");");
            placeholder = new Label(placeholderText);
            placeholder.getStyleClass().add("placeholder-title");
            placeholder.setWrapText(true);
            getChildren().add(placeholder);
        }
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        setClip(clip);
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        clip.setWidth(width);
        // Extending the clip below the box leaves the bottom corners square.
        clip.setHeight(roundTopOnly ? height + radius : height);
        if (width <= 0 || height <= 0) {
            return;
        }
        if (image != null) {
            double scale = Math.max(width / image.getWidth(), height / image.getHeight());
            double viewportWidth = width / scale;
            double viewportHeight = height / scale;
            view.setViewport(new Rectangle2D((image.getWidth() - viewportWidth) / 2,
                    (image.getHeight() - viewportHeight) / 2, viewportWidth, viewportHeight));
            view.setFitWidth(width);
            view.setFitHeight(height);
            view.relocate(0, 0);
        } else {
            layoutInArea(placeholder, 12, 12, width - 24, height - 24, 0, HPos.CENTER, VPos.CENTER);
        }
    }

    // The box takes its size from the layout, never from the photo.
    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 0;
    }

    @Override
    protected double computePrefWidth(double height) {
        return 0;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 0;
    }
}
