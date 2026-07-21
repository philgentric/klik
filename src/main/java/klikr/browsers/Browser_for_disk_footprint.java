package klikr.browsers;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.*;
import klikr.browser_core.Window_manager;
import klikr.browser_core.diskview.*;
import klikr.browser_core.virtual_landscape.Shutdown_target;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.progress.Hourglass;
import klikr.util.ui.progress.Progress_window;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

//*******************************************************
public class Browser_for_disk_footprint implements Owner_provider, Selection_manager, File_comparator_provider, Shutdown_target
//*******************************************************
{
    private final Stage stage;
    private Pane drawing_pane;
    private File_node current_root;    // currently displayed root (may be a subfolder)
    private File_node scan_root;       // the original scanned tree root
    private Label status_label;
    private Button up_button;
    public final Logger logger;
    public final int ID;



    //*******************************************************
    public Browser_for_disk_footprint(Window_builder window_builder, Logger logger)
    //*******************************************************
    {
        this.logger = logger;

        stage = new Stage();
        ID = Window_manager.register();
        Optional<Path> p = window_builder.path_list_provider.get_folder_path();
        if (p.isEmpty())
        {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
            return;
        }


        BorderPane root = new BorderPane();
        Look_and_feel_manager.set_region_look(root,stage,logger);

        ToolBar toolBar = new ToolBar();
        Look_and_feel_manager.set_region_look(toolBar,stage,logger);
        Button refresh_button = new Button(My_I18n.get_I18n_string("Refresh",stage,logger));
        Look_and_feel_manager.set_region_look(refresh_button,true,stage,logger);
        up_button = new Button(My_I18n.get_I18n_string("Parent_Folder",stage,logger));
        Look_and_feel_manager.set_region_look(up_button,true,stage,logger);
        status_label = new Label("OK");
        Look_and_feel_manager.set_region_look(status_label,stage,logger);

        toolBar.getItems().addAll(refresh_button, up_button, new Separator(), status_label);
        root.setTop(toolBar);

        drawing_pane = new Pane();
        drawing_pane.setStyle("-fx-background-color: #1e1e1e;");
        drawing_pane.widthProperty().addListener((obs, oldVal, newVal) -> refresh());
        drawing_pane.heightProperty().addListener((obs, oldVal, newVal) -> refresh());

        root.setCenter(drawing_pane);

        refresh_button.setOnAction(e -> {
            if (current_root != null) {
                start_scan(current_root.get_file());
            }
        });

        up_button.setOnAction(e -> {
            if (current_root == null) return;



            // Case 1: navigated into a subfolder of the scan tree → go up within the tree
            if (scan_root != null && current_root != scan_root)
            {
                Optional<Hourglass> x = Progress_window.show("Scanning disk",20*60,stage.getX()+100,stage.getY()+100,stage,logger);
                File_node parent = find_parent(scan_root, current_root);
                if (parent != null)
                {
                    navigate_to(parent,x);
                }
                else
                {
                    navigate_to(scan_root, x);
                }
                x.ifPresent(Hourglass::close);
                return;
            }

            // Case 2: already at scan root → scan the parent folder on disk
            File parentDir = current_root.get_file().getParentFile();
            if (parentDir != null && parentDir.isDirectory()) {
                start_scan(parentDir);
            }
        });

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
        stage.setScene(scene);

        stage.show();
        stage.setTitle("Disk footprint of: "+window_builder.path_list_provider.get_folder_path());

        stage.setOnCloseRequest(event->{
            shutdown();
        });
        start_scan(p.get().toFile());

    }

    @Override
    public Window get_owner() {
        return null;
    }

    @Override // Selection_manager
    public void set_unique_selected_item(Path path)
    {
        // not used
    }

    //*******************************************************
    @Override // File_comparator_provider
    public Comparator<? super Path> get_file_comparator()
    //*******************************************************
    {
        return null;
    }

    @Override
    public void shutdown() {
        Window_manager.unregister(ID,logger);
    }

    //*******************************************************
    private void update_up_button()
    //*******************************************************
    {
        if (current_root == null) {
            up_button.setDisable(true);
            return;
        }
        up_button.setDisable(current_root.get_file().getParentFile() == null);
    }

    //*******************************************************
    private void navigate_to(File_node node, Optional<Hourglass> x)
    //*******************************************************
    {
        Platform.runLater(() -> {
            current_root = node;
            update_up_button();
            status_label.setText(node.get_file().getAbsolutePath() + " — " + format_size_internal(node.get_size()));
            refresh();
            stage.setTitle("Disk footprint of: "+node.get_file().getAbsolutePath());
            x.ifPresent(Hourglass::close);


        });

    }

    /**
     * Given any clicked node, find the direct child of currentRoot that contains it
     * and navigate into that child. If the clicked node IS a direct child, navigate
     * into it (if it's a directory). Files at top level are ignored.
     */
    //*******************************************************
    private void navigate_to_child(File_node clickedNode)
    //*******************************************************
    {
        if (current_root == null) return;

        Optional<Hourglass> x = Progress_window.show("Scanning disk",20*60,stage.getX()+100,stage.getY()+100,stage,logger);
        // Is clickedNode itself a direct child of currentRoot?
        File_node topChild = find_top_level_child(current_root, clickedNode);
        if (topChild != null && topChild.is_this_a_directory()) {
            navigate_to(topChild, x);
        }
        x.ifPresent(Hourglass::close);
        // If topChild is a file (not a directory), or not found, do nothing
    }

    /** Find the parent of 'target' in the tree rooted at 'root', using file paths. */
    //*******************************************************
    private static File_node find_parent(File_node root, File_node target)
    //*******************************************************
    {
        File parentFile = target.get_file().getParentFile();
        if (parentFile == null) return null;
        String parentPath = parentFile.getAbsolutePath();
        String rootPath = root.get_file().getAbsolutePath();
        if (!parentPath.startsWith(rootPath)) return null;
        if (parentPath.equals(rootPath)) return root;

        // Walk down from root using path components
        String remainder = parentPath.substring(rootPath.length());
        if (remainder.startsWith(File.separator)) remainder = remainder.substring(1);
        String[] parts = remainder.split(java.util.regex.Pattern.quote(File.separator));
        File_node current = root;
        for (String part : parts) {
            File_node child = current.find_child(part);
            if (child == null) return null;
            current = child;
        }
        return current;
    }

    /**
     * Find which direct child of 'root' is the ancestor of (or is) 'target'.
     * Uses file paths — O(1), no tree traversal.
     */
    //*******************************************************
    private static File_node find_top_level_child(File_node root, File_node target)
    //*******************************************************
    {
        if (target == root) return null;
        String rootPath = root.get_file().getAbsolutePath();
        String targetPath = target.get_file().getAbsolutePath();
        if (!targetPath.startsWith(rootPath)) return null;

        // Find the name of the direct child folder that contains target
        String remainder = targetPath.substring(rootPath.length());
        if (remainder.startsWith(File.separator)) remainder = remainder.substring(1);
        int sep = remainder.indexOf(File.separatorChar);
        String childName = (sep >= 0) ? remainder.substring(0, sep) : remainder;

        return root.find_child(childName);
    }

    //*******************************************************
    private void start_scan(File folder)
    //*******************************************************
    {
        stage.setTitle("Disk footprint of: "+folder.getAbsolutePath());
        status_label.setText("Scanning " + folder.getAbsolutePath() + "...");
        scan_root = null;  // will be set when scan/cache completes

        // Thread 1: load cache, then do progressive depth reveal
        Actor_engine.execute(() -> {
            scan_in_thread(folder);
        },"diskview scan",logger);
    }

    private void scan_in_thread(File folder)
    {
        Optional<Hourglass> x = Progress_window.show("Scanning disk",20*60,stage.getX()+100,stage.getY()+100,stage,logger);
        try {
            File_node cacheHint = current_root;
            if (cacheHint == null || !cacheHint.get_file().equals(folder)) {
                cacheHint = Scan_cache.load(folder,logger);
            }
            final File_node cached = cacheHint;
            final double w = drawing_pane.getWidth();
            final double h = drawing_pane.getHeight();

            // Progressive depth reveal — one frame at a time, waiting for FX to finish each
            if (cached != null && cached.get_size() > 0 && w > 0 && h > 0) {
                int prevCount = 0;
                for (int depth = 1; depth <= 30; depth++)
                {
                    List<Draw_command> cmds = build_Draw_commands2(cached, w, h, depth);
                    if (cmds.size() == prevCount) break;
                    prevCount = cmds.size();

                    final int d = depth;
                    final List<Draw_command> frame = cmds;
                    CountDownLatch latch = new CountDownLatch(1);
                    Platform.runLater(() -> {
                        try {
                            current_root = cached;
                            if (scan_root == null) scan_root = cached;
                            update_up_button();
                            status_label.setText("Depth " + d + "… " + frame.size()
                                    + " items — " + format_size_internal(cached.get_size()));
                            apply_Draw_commands(frame);
                        } finally {
                            latch.countDown();
                        }
                    });
                    latch.await();
                    // After the latch, FX has finished but hasn't painted yet.
                    // Yield so the FX thread can process a paint pulse.
                    Thread.yield();
                }
                Platform.runLater(() ->
                        status_label.setText("Cache loaded — " + format_size_internal(cached.get_size()) + " — scanning…"));
            }

            // After reveal is done, start the scan on THIS same thread
            // (Disk_scanner internally spawns one virtual thread per folder)
            long start_time = System.currentTimeMillis();
            Disk_scanner scanner = new Disk_scanner(logger);
            File_node root = scanner.scan(folder, cached);
            long elapsed = System.currentTimeMillis() - start_time;
            int scanned = scanner.getScanned_folders();
            int cacheHits = scanner.getFoldersSkipped();

            Scan_cache.save_in_a_thread(root,logger);

            double w2 = drawing_pane.getWidth();
            double h2 = drawing_pane.getHeight();
            if (w2 > 0 && h2 > 0)
            {
                List<Draw_command> freshCmds = build_Draw_commands(root, w2, h2);
                Platform.runLater(() -> {
                    current_root = root;
                    scan_root = root;
                    update_up_button();
                    String msg = String.format("Scanned %d folders in %d ms — %s",
                            scanned + cacheHits, elapsed, format_size_internal(root.get_size()));
                    if (cacheHits > 0) {
                        msg += String.format("  [%d cached, %d rescanned]", cacheHits, scanned);
                    }
                    status_label.setText(msg);
                    apply_Draw_commands(freshCmds);
                });
            }
        } catch (Exception e) {
            logger.log(""+e);
        }
        finally {
            x.ifPresent(Hourglass::close);
        }
    }

    /** Async version: computes layout on a virtual thread, draws via Platform.runLater. Good for resize events. */
    //*******************************************************
    private void refresh()
    //*******************************************************
    {
        if (current_root == null) return;
        if (drawing_pane.getWidth() <= 0 || drawing_pane.getHeight() <= 0) return;

        double w = drawing_pane.getWidth();
        double h = drawing_pane.getHeight();
        File_node root = current_root;

        Actor_engine.execute(() -> {
            try {
                List<Draw_command> commands = build_Draw_commands(root, w, h);
                Platform.runLater(() -> apply_Draw_commands(commands));
            } catch (Throwable e) {
                logger.log(""+e);
            }
        },"diskview refresh",logger);
    }


    //*******************************************************
    private List<Draw_command> build_Draw_commands(File_node root, double w, double h)
    //*******************************************************
    {
        return build_Draw_commands2(root, w, h, Integer.MAX_VALUE);
    }

    //*******************************************************
    private List<Draw_command> build_Draw_commands2(File_node root, double w, double h, int maxDepth)
    //*******************************************************
    {
        long root_total_size = root.get_size();
        List<Draw_command> commands = new java.util.ArrayList<>();
        compute_layout(root, 0, 0, w, h, 0, -1, root_total_size, maxDepth, commands);
        return commands;
    }

    //*******************************************************
    private void apply_Draw_commands(List<Draw_command> commands)
    //*******************************************************
    {
        drawing_pane.getChildren().clear();

        double pane_width = drawing_pane.getWidth();
        double pane_height = drawing_pane.getHeight();

        // Separate interactive vs canvas items
        List<Draw_command> canvas_items = new ArrayList<>();
        List<Draw_command> interactive_items = new ArrayList<>();
        for (Draw_command cmd : commands) {
            if (cmd.is_interactive()) {
                interactive_items.add(cmd);
            } else {
                canvas_items.add(cmd);
            }
        }

        // ── Layer 1: Canvas for all non-interactive (small) items ──
        Canvas canvas = new Canvas(pane_width, pane_height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, pane_width, pane_height);

        for (Draw_command cmd : canvas_items) {
            cmd.paint_on_canvas(gc);
        }

        // Click on canvas → find the clicked item → navigate to its top-level folder
        canvas.setOnMouseClicked(ev -> {
            if (ev.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
            double mx = ev.getX();
            double my = ev.getY();

            Actor_engine.execute(()->
            {            explore_item(canvas_items, mx, my);
            },"explore_item",logger);
        });

        drawing_pane.getChildren().add(canvas);

        // ── Layer 2: real nodes for interactive items ──
        Runnable rescan = () -> {
            if (current_root != null) start_scan(current_root.get_file());
        };
        java.util.function.Consumer<File_node> navigate = this::navigate_to_child;
        for (Draw_command cmd : interactive_items) {
            cmd.rescan_callback = rescan;
            cmd.navigate_callback = navigate;
            cmd.execute(drawing_pane, Klikr_application.application,stage);
        }
    }

    private void explore_item(List<Draw_command> canvas_items, double mx, double my)
    {
        // Hit-test: find the smallest (deepest) canvas item containing the click point
        Draw_command hit = null;
        double hitArea = Double.MAX_VALUE;
        for (Draw_command cmd : canvas_items) {
            Rectangle2D b = cmd.bounds;
            if (mx >= b.getMinX() && mx <= b.getMinX() + b.getWidth()
                    && my >= b.getMinY() && my <= b.getMinY() + b.getHeight()) {
                double area = b.getWidth() * b.getHeight();
                if (area < hitArea) {
                    hitArea = area;
                    hit = cmd;
                }
            }
        }
        if (hit != null && current_root != null) {
            navigate_to_child(hit.node);
        }
    }

    /**
     * color_family  -1 means "not yet assigned" (root level);
     *                     at depth 0→1 each top-level child gets its own family index.
     * max_depth     stop recursing at this depth — folders beyond are drawn as solid leaves.
     */
    //*******************************************************
    private void compute_layout(File_node node, double x, double y, double w, double h,
                                int depth, int color_family, long root_total_size, int max_depth,
                                List<Draw_command> commands)
    //*******************************************************
    {
        if (w <= 0 || h <= 0) return;

        Layout_result layout = Treemap_layout.calculateAt(node, x, y, w, h);

        if (layout.children == null || layout.children.isEmpty())
        {
            if (node.get_size() > 0) {
                commands.add(new Draw_command(node, new Rectangle2D(x, y, w, h), color_family, depth, root_total_size,logger));
            }
            return;
        }

        for (Layout_result child : layout.children)
        {
            Rectangle2D b = child.bounds;
            if (b.getWidth() <= 0 || b.getHeight() <= 0) continue;

            int family = color_family;
            if (family < 0) {
                // Stable color: hash the name so the same folder always gets the same hue,
                // regardless of which level is the current root.
                family = Math.floorMod(child.node.get_file().getName().hashCode(), Draw_command.FAMILY_HUES.length);
            }

            if (!child.node.is_this_a_directory()) {
                commands.add(new Draw_command(child.node, b, family, depth + 1, root_total_size,logger));
            } else if (depth + 1 < max_depth && depth < 50
                    && !child.node.get_children().isEmpty()
                    && b.getWidth() >= 4 && b.getHeight() >= 4) {
                compute_layout(child.node, b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight(),
                        depth + 1, family, root_total_size, max_depth, commands);
            } else {
                // Directory drawn as solid placeholder — not yet expanded
                commands.add(new Draw_command(child.node, b, family, depth + 1, root_total_size, child.node.is_this_a_directory(),logger));
            }
        }
    }



    //*******************************************************
    private String format_size_internal(long size)
    //*******************************************************
    {
        if (size <= 0) return "0 B";
        final String[] units = { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(
                size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }


}