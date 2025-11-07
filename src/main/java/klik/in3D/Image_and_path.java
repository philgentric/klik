// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

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
import klik.path_lists.Path_list_provider;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//*******************************************************
public class Image_and_path
//*******************************************************
{
    public static final int SPLIT_STRING_AFTER = 18;
    //private Image large_image; dont cache: it is cached as a Phong
    private final Logger logger;
    public final Window owner;
    public final Image small_image;
    public final Path path;

    //*******************************************************
    public Image_and_path( Path path, int small_image_size, int large_image_size, Window owner, Logger logger)
    //*******************************************************
    {
        this.path = path;
        this.owner = owner;
        this.logger = logger;
        if ( Files.isDirectory(path))
        {
            this.small_image = make_square(large_image_size);
        }
        else
        {
            this.small_image = make_square(small_image_size);
        }
    }

    //*******************************************************
    Image get_large_image(int large_image_size)
    //*******************************************************
    {
        return make_square(large_image_size);
//        if ( large_image == null) large_image = make_square(large_image_size);
//        return large_image;
    }

    //*******************************************************
    private Image make_square(int icon_size)
    //*******************************************************
    {
        Image image = get_image_as_icon(path,icon_size);
        if (( image.getHeight() <= 0)||( image.getWidth() <= 0))
        {
            logger.log(path+ " PANIC loaded image is broken");
            return null;
        }
        Image padded_image = pad_to_square(image);
        return padded_image;

    }

    //*******************************************************
    private Image pad_to_square(Image image)
    //*******************************************************
    {
        double size = Math.max(image.getHeight(), image.getWidth());
        Canvas canvas = new Canvas(size,size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0,0,size,size);

        double x = (size- image.getWidth())/2.0;
        double y = (size- image.getHeight())/2.0;
        gc.drawImage(image,x,y);

        WritableImage padded_image = new WritableImage((int)size,(int)size);
        SnapshotParameters p = new SnapshotParameters();
        p.setFill((Color.BLACK));

        canvas.snapshot(p,padded_image);

        return padded_image;
    }

    //*******************************************************
    private Image get_image_as_icon(Path path, int icon_size)
    //*******************************************************
    {
        if (Files.isDirectory(path))
        {
            //logger.log(path.toAbsolutePath() +" is folder");
            String relative_path = "icons/wood/folder.png";
            Image folder_icon = Jar_utils.load_jfx_image_from_jar(relative_path, icon_size, owner,logger);

            //Image folder_icon = Look_and_feel_manager.get_folder_icon(icon_size,owner,logger);
            return make_folder_icon_with_folder_name(folder_icon, path.getFileName().toString(), icon_size, icon_size);
        }
        if (!Guess_file_type.is_this_path_an_image(path))
        {
            logger.log("WARNING, displaying non-image files not implemented in 3D "+path.toAbsolutePath());
            return Jar_utils.get_broken_icon(icon_size,owner,logger);
        }
        //logger.log(path.toAbsolutePath() +" is image");

        try (InputStream is = new FileInputStream(path.toFile())) {
            return new Image(is, icon_size, icon_size, true, true);
        } catch (Exception e) {
            logger.log("❌ fatal " + e);
            return null;
        }
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

        boolean split = true;
        if (split)
        {


            //split it in pieces of 8 chars
            int beg = 0;
            int end = SPLIT_STRING_AFTER;
            double ty = 0;
            for (;;) {
                if (beg >= path_string.length()) break;
                if (end > path_string.length()) end = path_string.length();
                String piece = path_string.substring(beg, end);
                beg += SPLIT_STRING_AFTER;
                end += SPLIT_STRING_AFTER;
                Text text = new Text(piece);
                text.setFont(Font.font("AtkinsonHyperlegible-Bold.ttf", FontWeight.BOLD, 150));
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
                //logger.log("wrote ->" + piece + " at: " + tx + " , " + ty);
                ty += txtHeight;
            }
        }
        else
        {
            Text text = new Text(path_string);
            text.setFont(Font.font("AtkinsonHyperlegible-Bold.ttf", FontWeight.BOLD, 150));
            text.setFill(Color.WHITE);
            text.applyCss();        // force CSS layout

            double txtWidth = text.getLayoutBounds().getWidth();
            double txtHeight = text.getLayoutBounds().getHeight();

            double tx = (w - txtWidth) / 2;
            double ty = 0;

            // Use a temporary snapshot to render the Text node
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage txtImg = text.snapshot(params, null);

            gc.drawImage(txtImg, tx, ty);
            //logger.log("wrote ->" + path_string + " at: " + tx + " , " + ty);
        }
        // 3) take a snapshot of the whole canvas
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT); // keep alpha channel
        WritableImage snapshot = new WritableImage(w, h);
        canvas.snapshot(sp, snapshot);

        return snapshot;
    }


}
