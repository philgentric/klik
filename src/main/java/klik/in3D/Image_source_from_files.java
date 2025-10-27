package klik.in3D;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Window;
import klik.look.Jar_utils;
import klik.look.Look_and_feel_manager;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.properties.boolean_features.Booleans;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.image.Static_image_utilities;
import klik.util.log.Logger;
import klik.util.perf.Perf;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

//*******************************************************
public class Image_source_from_files implements Image_source
//*******************************************************
{
    private final int icon_size;
    private int i = 0;
    private final List<Path> paths = new ArrayList<>();
    private final Map<Path,Image_and_path> cache = new HashMap<>();
    private final Path_list_provider_for_file_system path_list_provider;
    private final Logger logger;
    private final Window owner;
    //*******************************************************
    public Image_source_from_files(Path folder, int icon_size, Window owner, Logger logger)
    //*******************************************************
    {
        this.icon_size = icon_size;
        this.owner = owner;
        this.logger = logger;

        path_list_provider = new Path_list_provider_for_file_system(folder);

        try ( Perf p = new Perf("Image_source_from_files: sorting"))
        {
            List<Path> folders = path_list_provider.only_folder_paths(Feature_cache.get(Feature.Show_hidden_folders));
            Collections.sort(folders);
            paths.addAll(folders);

            List<Path> files = path_list_provider.only_file_paths(Feature_cache.get(Feature.Show_hidden_files));
            Collections.sort(files);
            paths.addAll(files);
        }
    }


    //*******************************************************
    @Override
    public Image_and_path get(int i)
    //*******************************************************
    {
        if ( i < 0 ) return null;
        if ( i >= paths.size() ) return null;
        Path p = paths.get(i);
        Image_and_path returned = cache.get(p);
        if ( returned != null) return returned;
        returned = make_one(p);
        cache.put(p,returned);
        return  returned;
    }

    //*******************************************************
    private Image_and_path make_one(Path path)
    //*******************************************************
    {
        if (Files.isDirectory(path))
        {
            //logger.log(path.toAbsolutePath() +" is folder");
            String relative_path = "icons/wood/folder.png";
            Image folder_icon = Jar_utils.load_jfx_image_from_jar(relative_path, icon_size, owner,logger);

            //Image folder_icon = Look_and_feel_manager.get_folder_icon(icon_size,owner,logger);
            folder_icon = make_folder_icon_with_folder_name(folder_icon, path.getFileName().toString(), icon_size, icon_size);

            return new Image_and_path(folder_icon,path);
        }
        if (!Guess_file_type.is_this_path_an_image(path))
        {
            logger.log("WARNING, displaying non-image files not implemented "+path.toAbsolutePath());
            return new Image_and_path(Jar_utils.get_broken_icon(icon_size,owner,logger),path);
        }
        //logger.log(path.toAbsolutePath() +" is image");
        try (InputStream is = new FileInputStream(path.toFile())) {
            return new Image_and_path(
                    new Image(is, icon_size, icon_size, true, true),
                    path);
        } catch (Exception e) {
            logger.log("❌ fatal " + e);
            return null;
        }
    }

    //*******************************************************
    @Override
    public int how_many_items()
    //*******************************************************
    {
        return paths.size();
    }

    //*******************************************************
    private Image make_folder_icon_with_folder_name(
            Image icon, String path_string, int w, int h)
    //*******************************************************
    {
        Canvas canvas = new Canvas(w, h);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // background (transparent)
        gc.setFill(Color.TRANSPARENT);
        gc.fillRect(0, 0, w, h);

        // 1) draw the icon (centered)
        if (icon != null) {
            double iconSize = Math.min(w, h); // 60 % of texture
            double ix = (w - iconSize) / 2;
            double iy = (h - iconSize) / 2;
            gc.drawImage(icon, ix, iy, iconSize, iconSize);
        }

        // 2) draw the text
        //split it in pieces of 8 chars
        int beg = 0;
        int end = 8;
        double ty = 0;
        for(;;)
        {
            if ( beg >= path_string.length()) break;
            if ( end > path_string.length()) end = path_string.length();
            String piece = path_string.substring(beg,end);
            beg += 8;
            end += 8;
            Text text = new Text(piece);
            text.setFont(Font.font("AtkinsonHyperlegible-Bold.ttf", FontWeight.BOLD, 100));
            text.setFill(Color.WHITE);
            text.applyCss();        // force CSS layout

            double txtWidth = text.getLayoutBounds().getWidth();
            double txtHeight = text.getLayoutBounds().getHeight();

            double tx = (w - txtWidth) / 2;

            // Use a temporary snapshot to render the Text node
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage txtImg = text.snapshot(params, null);

            gc.drawImage(txtImg, tx, ty);
            logger.log("wrote ->"+piece+" at: "+tx+" , "+ty);
            ty += txtHeight;
        }
        // 3) take a snapshot of the whole canvas
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT); // keep alpha channel
        WritableImage snapshot = new WritableImage(w, h);
        canvas.snapshot(sp, snapshot);

        return snapshot;
    }

}
