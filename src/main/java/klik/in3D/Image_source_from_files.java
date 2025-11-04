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
import klik.path_lists.Path_list_provider_for_file_system;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Feature_cache;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;
import klik.util.perf.Perf;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

//*******************************************************
public class Image_source_from_files implements Image_source
//*******************************************************
{
    private final int small_icon_size;
    private final int large_icon_size;
    private int i = 0;
    private final List<Path> paths = new ArrayList<>();
    private final Map<Path,Image_and_path> cache = new HashMap<>();
    private final Path_list_provider_for_file_system path_list_provider;
    private final Logger logger;
    private final Window owner;
    //*******************************************************
    public Image_source_from_files(Path folder, int small_icon_size, int large_icon_size, Window owner, Logger logger)
    //*******************************************************
    {
        this.small_icon_size = small_icon_size;
        this.large_icon_size = large_icon_size;
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
        returned = new Image_and_path(p,small_icon_size,large_icon_size,owner,logger);
        cache.put(p,returned);
        return  returned;
    }


    //*******************************************************
    @Override
    public int how_many_items()
    //*******************************************************
    {
        return paths.size();
    }


}
