package klikr.browser_core.diskview;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Window;
import klikr.Window_builder;
import klikr.Window_type;
import klikr.browser_core.virtual_landscape.Virtual_landscape;
import klikr.images.Image_window;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.util.execute.actor.Aborter;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.log.Logger;
import klikr.util.ui.Menu_items;

import java.io.File;
import java.nio.file.Path;

//*******************************************************
public class Draw_command
//*******************************************************
{
    private static final boolean dbg = false;
    final Logger logger;
    public final File_node node;
    public final Rectangle2D bounds;
    final int color_family;   // index into FAMILY_HUES
    final int depth;
    final long root_total_size;
    final boolean place_holder; // true = folder drawn as solid leaf (depth cutoff), not yet expanded
    public Runnable rescan_callback;  // set by the Diskview_application to trigger rescan after delete
    public java.util.function.Consumer<File_node> navigate_callback; // set by Diskview_application for click-to-navigate

    // Visually distinct hues (degrees) for top-level folder color families.
    // Spread around the color wheel to maximise perceptual distance.
    public static final double[] FAMILY_HUES = {
            210,  // blue
            30,  // orange
            120,  // green
            340,  // rose / red-pink
            55,  // gold / yellow
            170,  // teal / cyan
            275,  // purple
            10,  // red
            150,  // mint green
            310,  // magenta
            85,  // lime
            195,  // steel blue
            240,  // indigo
            45,  // amber
            355,  // crimson
            135,  // sea green
    };

    //*******************************************************
    public Draw_command(File_node node, Rectangle2D bounds, int color_family, int depth, long root_total_size, Logger logger)
    //*******************************************************
    {
        this(node, bounds, color_family, depth, root_total_size, false, logger);
    }

    //*******************************************************
    public Draw_command(File_node node, Rectangle2D bounds, int color_family, int depth, long root_total_size, boolean placeholder, Logger logger)
    //*******************************************************
    {
        this.logger = logger;
        this.node = node;
        this.bounds = bounds;
        this.color_family = color_family;
        this.depth = depth;
        this.root_total_size = root_total_size;
        this.place_holder = placeholder;
    }

    /**
     * Items with rectangles large enough to be visually meaningful get full
     * interactivity (tooltip, hover, context menu, labels).
     * Tiny rectangles (< 600 sq px, roughly 30×20) go to the Canvas layer.
     */
    public boolean is_interactive() {
        return bounds.getWidth() * bounds.getHeight() >= 600;
    }

    /** Paint a lightweight representation on a Canvas (no nodes, no event handlers). */
    //*******************************************************
    public void paint_on_canvas(GraphicsContext gc)
    //*******************************************************
    {
        double bw = bounds.getWidth();
        double bh = bounds.getHeight();
        if (bw < 1 || bh < 1) return;

        double sw = size_weight(node.get_size(), root_total_size);
        Color base = family_color(color_family, depth, sw);

        // Simple filled rect with slightly darker stroke — no gradient, no effects
        gc.setFill(base);
        gc.fillRect(bounds.getMinX(), bounds.getMinY(), bw, bh);
        gc.setStroke(base.darker());
        gc.setLineWidth(0.5);
        gc.strokeRect(bounds.getMinX(), bounds.getMinY(), bw, bh);
    }

    /** Create full interactive Rectangle node (gradient, shadow, hover, tooltip, menu, labels). */
    //*******************************************************
    public void execute(Pane pane, Application application, Window owner)
    //*******************************************************
    {
        double bw = bounds.getWidth();
        double bh = bounds.getHeight();

        Rectangle rect = new Rectangle(bounds.getMinX(), bounds.getMinY(), bw, bh);

        // Compute size weight (same log scale as familyColor)
        double sizeWeight = size_weight(node.get_size(), root_total_size);

        Color baseColor = family_color(color_family, depth, sizeWeight);

        // ── Cushion gradient ──
        // Linear gradient from bright top-left to dark bottom-right: beveled panel look.
        double cs = sizeWeight; // cushion strength: 0 (flat) → 1 (very 3D)

        // Highlight: bright top-left corner
        Color highlightColor = baseColor.deriveColor(0,
                Math.max(0.0, 1.0 - 0.20 * cs),       // slightly desaturated
                1.0 + 0.50 * cs, 1.0);                 // up to 50% brighter
        // Mid-tone: the base color itself
        Color midColor = baseColor;
        // Shadow: dark bottom-right corner
        Color shadowColor = baseColor.deriveColor(0,
                1.0 + 0.25 * cs,                       // more saturated
                1.0 - 0.45 * cs, 1.0);                 // up to 45% darker

        // Linear gradient: top-left → bottom-right (diagonal light)
        javafx.scene.paint.LinearGradient cushion = new javafx.scene.paint.LinearGradient(
                0, 0, 1, 1,    // start top-left, end bottom-right
                true,           // proportional
                CycleMethod.NO_CYCLE,
                new Stop(0.0,  highlightColor),
                new Stop(0.45, midColor),
                new Stop(1.0,  shadowColor)
        );
        rect.setFill(cushion);

        // ── Inner shadow for bevel depth ──
        double shadowRadius = 2.0 + 12.0 * cs;
        if (shadowRadius > 0.35 * Math.min(bw, bh)) {
            shadowRadius = 0.35 * Math.min(bw, bh);
        }
        if (shadowRadius > 1.5) {
            InnerShadow innerShadow = new InnerShadow();
            innerShadow.setRadius(shadowRadius);
            innerShadow.setColor(Color.color(0, 0, 0, 0.20 + 0.30 * cs));
            innerShadow.setOffsetX(1.0 * cs);
            innerShadow.setOffsetY(1.0 * cs);
            rect.setEffect(innerShadow);
        }

        rect.setStroke(shadowColor.darker());
        rect.setStrokeWidth(0.5 + 0.5 * cs);

        String name = node.get_file().getName();
        if (name.isEmpty()) name = node.get_file().getAbsolutePath();
        String sizeStr = format_size(node.get_size());
        if (place_holder) {
            sizeStr = "scanning… " + sizeStr;
        }

        // ── Hover: brighten the gradient ──
        rect.setOnMouseEntered(e -> {
            Color hh = highlightColor.brighter();
            Color hm = midColor.brighter();
            Color hs = shadowColor.brighter();
            rect.setFill(new javafx.scene.paint.LinearGradient(
                    0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, hh),
                    new Stop(0.45, hm),
                    new Stop(1.0, hs)));
        });
        rect.setOnMouseExited(e -> rect.setFill(cushion));

        Tooltip.install(rect, new Tooltip(name + "\n" + sizeStr));

        // ── Right-click context menu (lazy — only created on first right-click) ──
        // ── Left-click on directory → navigate into it ──
        rect.setOnMousePressed(ev -> {
            if (ev.isSecondaryButtonDown()) {
                ContextMenu contextMenu = create_ContextMenu(node, rescan_callback,application, owner, logger);
                contextMenu.show(rect, ev.getScreenX(), ev.getScreenY());
                ev.consume();
            }
        });
        rect.setOnMouseClicked(ev -> {
            if (ev.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && ev.getClickCount() == 1
                    && navigate_callback != null) {
                navigate_callback.accept(node);
                ev.consume();
            }
        });

        pane.getChildren().add(rect);

        // ── Labels ──
        if (bw < 20 || bh < 12) return;

        Color text_color = color_for_text(baseColor);
        double cx = bounds.getMinX() + bw / 2;

        Text name_Text = new Text(name);
        name_Text.setFill(text_color);
        Text size_Text = new Text(sizeStr);
        size_Text.setFill(text_color);

        double name_width = name_Text.getLayoutBounds().getWidth();
        double name_height = name_Text.getLayoutBounds().getHeight();
        double size_width = size_Text.getLayoutBounds().getWidth();
        double size_height = size_Text.getLayoutBounds().getHeight();

        boolean name_fits = name_width < bw - 4 && name_height < bh - 2;
        boolean size_fits = size_width < bw - 4 && size_height < bh - 2;
        boolean both_fit  = Math.max(name_width, size_width) < bw - 4 && (name_height + size_height + 2) < bh - 2;

        if (both_fit) {
            double total_height = name_height + size_height + 2;
            double top_y = bounds.getMinY() + (bh - total_height) / 2 + name_height;
            name_Text.setX(cx - name_width / 2);
            name_Text.setY(top_y);
            name_Text.setMouseTransparent(true);
            pane.getChildren().add(name_Text);

            size_Text.setX(cx - size_width / 2);
            size_Text.setY(top_y + size_height + 2);
            size_Text.setMouseTransparent(true);
            size_Text.setOpacity(0.7);
            pane.getChildren().add(size_Text);
        } else if (name_fits) {
            name_Text.setX(cx - name_width / 2);
            name_Text.setY(bounds.getMinY() + (bh + name_height) / 2 - 2);
            name_Text.setMouseTransparent(true);
            pane.getChildren().add(name_Text);
        } else if (size_fits) {
            size_Text.setX(cx - size_width / 2);
            size_Text.setY(bounds.getMinY() + (bh + size_height) / 2 - 2);
            size_Text.setMouseTransparent(true);
            size_Text.setOpacity(0.7);
            pane.getChildren().add(size_Text);
        }
    }

    //*******************************************************
    private static ContextMenu create_ContextMenu(File_node node, Runnable rescan_callback, Application application, Window owner, Logger logger)
    //*******************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Virtual_landscape.attach_contextmenu_to_browser(owner, context_menu);

        {
            if ( !node.is_this_a_directory())
            {
                Path p = node.get_file().toPath();
                if (Guess_file_type.is_this_path_extension_an_image(p,owner,logger))
                {
                    Menu_items.add_menu_item_for_context_menu(
                        "Show_Image",true,
                        null,//(new KeyCodeCombination(KeyCode.N,KeyCombination.SHORTCUT_DOWN)).getDisplayText(),
                        event -> {
                            Image_window.get_Image_window(p, new Path_list_provider_for_file_system(p.getParent(),owner,logger),null, owner,new Aborter("Image_viewer",logger),logger);
                        }, context_menu, owner, logger);



                }

            }
        }
        {
            Menu_items.add_menu_item_for_context_menu(
                    "Browse_in_new_window",true,
                    null,//(new KeyCodeCombination(KeyCode.N,KeyCombination.SHORTCUT_DOWN)).getDisplayText(),
                    event -> {
                        if (dbg) logger.log("Browse in new window!");
                        Path folder_path = node.get_file().toPath().getParent();
                        Window_builder.additional_no_past(application, Window_type.File_system_2D,new Path_list_provider_for_file_system(folder_path,owner,logger), owner, logger);
                    }, context_menu, owner, logger);

        }
        {
            String os = System.getProperty("os.name", "").toLowerCase();
            String reveal_label = os.contains("mac") ? "Reveal in Finder"
                    : os.contains("win") ? "Show in Explorer" : "Show in File Manager";
            MenuItem reveal_item = new MenuItem(reveal_label);
            reveal_item.setOnAction(ev -> {
                try {
                    File target = node.get_file();
                    if (os.contains("mac")) {
                        new ProcessBuilder("open", "-R", target.getAbsolutePath()).start();
                    } else if (os.contains("win")) {
                        new ProcessBuilder("explorer", "/select,", target.getAbsolutePath()).start();
                    } else {
                        File folder = target.isDirectory() ? target : target.getParentFile();
                        if (folder != null) {
                            new ProcessBuilder("xdg-open", folder.getAbsolutePath()).start();
                        }
                    }
                } catch (Exception ex) {
                    logger.log("" + ex);
                }
            });
            context_menu.getItems().add(reveal_item);
        }
        {
            MenuItem delete_item = new MenuItem("Delete");
            delete_item.setOnAction(ev -> {
                File target = node.get_file();
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete \"" + target.getName() + "\"?\n"
                                + format_size(node.get_size()) + "\n\n"
                                + target.getAbsolutePath(),
                        ButtonType.OK, ButtonType.CANCEL);
                confirm.setTitle("Confirm Delete");
                confirm.setHeaderText("This cannot be undone.");
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.OK) {
                        boolean deleted;
                        if (target.isDirectory()) {
                            deleted = delete_recursively(target);
                        } else {
                            deleted = target.delete();
                        }
                        if (deleted) {
                            if (rescan_callback != null) rescan_callback.run();
                        } else {
                            new Alert(Alert.AlertType.ERROR, "Failed to delete:\n" + target.getAbsolutePath())
                                    .showAndWait();
                        }
                    }
                });
            });
            context_menu.getItems().add(delete_item);
        }

        return context_menu;
    }

    //*******************************************************
    private static boolean delete_recursively(File file)
    //*******************************************************
    {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!delete_recursively(child)) return false;
                }
            }
        }
        return file.delete();
    }

    //*******************************************************
    private static double size_weight(long itemSize, long root_total_size)
    //*******************************************************
    {
        if (root_total_size <= 0 || itemSize <= 0) return 0.0;
        double ratio = (double) itemSize / root_total_size;
        double logRatio = Math.log10(ratio);
        return Math.max(0.0, Math.min(1.0, (logRatio + 8.0) / 8.0));
    }

    //*******************************************************
    private static Color family_color(int family, int depth, double sizeWeight)
    //*******************************************************
    {
        if (family < 0) {
            return Color.hsb(0, 0, 0.45);
        }
        double hue = FAMILY_HUES[family % FAMILY_HUES.length];
        double saturation = 0.15 + sizeWeight * 0.60;
        saturation = Math.max(0.10, saturation - depth * 0.02);
        double brightness = 0.92 - sizeWeight * 0.22;
        brightness = Math.min(0.95, brightness + depth * 0.01);
        return Color.hsb(hue, saturation, brightness);
    }

    //*******************************************************
    private static Color color_for_text(Color bg)
    //*******************************************************
    {
        double lum = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
        return lum < 0.55 ? Color.WHITE : Color.BLACK;
    }

    //*******************************************************
    private static String format_size(long size)
    //*******************************************************
    {
        if (size <= 0) return "0 B";
        final String[] units = { "B", "KB", "MB", "GB", "TB" };
        int digit_groups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(
                size / Math.pow(1024, digit_groups)) + " " + units[digit_groups];
    }
}
