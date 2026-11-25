package ui;

import java.util.Map;

import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Small line icons drawn as vector paths, so they look sharp at any size and don't depend on
 * which symbols a font happens to include. Colour them in CSS with {@code -fx-stroke} on {@code .icon}.
 */
public final class Icons {
    /** Outline paths on a 24 x 24 grid. */
    private static final Map<String, String> PATHS = Map.ofEntries(
            Map.entry("search", "M11 3a8 8 0 1 0 0 16a8 8 0 1 0 0-16z M21 21l-4.35-4.35"),
            Map.entry("sliders", "M4 21v-7 M4 10V3 M12 21v-9 M12 8V3 M20 21v-5 M20 12V3 M1 14h6 M9 8h6 M17 16h6"),
            Map.entry("plus", "M12 5v14 M5 12h14"),
            Map.entry("chevron-down", "M6 9l6 6 6-6"),
            Map.entry("chevron-left", "M15 18l-6-6 6-6"),
            Map.entry("logout", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9"),
            Map.entry("user", "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M12 3a4 4 0 1 0 0 8a4 4 0 1 0 0-8z"),
            Map.entry("grid", "M3 3h7v7H3z M14 3h7v7h-7z M14 14h7v7h-7z M3 14h7v7H3z"),
            Map.entry("message", "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"),
            Map.entry("lock", "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z M7 11V7a5 5 0 0 1 10 0v4"),
            Map.entry("image", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z "
                    + "M8.5 7a1.5 1.5 0 1 0 0 3a1.5 1.5 0 1 0 0-3z M21 15l-5-5L5 21"),
            Map.entry("upload", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 M17 8l-5-5-5 5 M12 3v12"),
            Map.entry("check", "M20 6L9 17l-5-5"),
            Map.entry("x", "M18 6L6 18 M6 6l12 12"),
            Map.entry("pin", "M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z M12 7a3 3 0 1 0 0 6a3 3 0 1 0 0-6z"),
            Map.entry("calendar", "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M16 2v4 M8 2v4 M3 10h18"),
            Map.entry("send", "M22 2L11 13 M22 2l-7 20-4-9-9-4 20-7z"),
            Map.entry("edit", "M17 3a2.83 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"),
            Map.entry("trash", "M3 6h18 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6 M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"),
            Map.entry("eye", "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z"),
            Map.entry("users", "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 3a4 4 0 1 0 0 8a4 4 0 1 0 0-8z "
                    + "M23 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75"),
            Map.entry("home", "M3 10l9-7 9 7v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M9 22V12h6v10"),
            Map.entry("star", "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01z"),
            Map.entry("inbox", "M22 12h-6l-2 3h-4l-2-3H2 M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"));

    private Icons() {
    }

    /** An icon in a square box of {@code size} pixels. Extra style classes go on the icon's path. */
    public static StackPane of(String name, double size, String... styleClasses) {
        String path = PATHS.get(name);
        if (path == null) {
            throw new IllegalArgumentException("Unknown icon: " + name);
        }
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.getStyleClass().add("icon");
        svg.getStyleClass().addAll(styleClasses);
        svg.setStrokeLineCap(StrokeLineCap.ROUND);
        svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
        svg.setScaleX(size / 24);
        svg.setScaleY(size / 24);

        // The Group takes the scale into account when it is laid out; the box keeps every icon the same size.
        StackPane box = new StackPane(new Group(svg));
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        box.getStyleClass().add("icon-box");
        return box;
    }
}
