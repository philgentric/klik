// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.audio;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.*;
import javafx.stage.Window;
import klikr.Instructions;
import klikr.Window_type;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.animated_gifs.Ffmpeg_utils;
import klikr.browser.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.path_lists.Path_list_provider_for_playlist;
import klikr.look.Look_and_feel_manager;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.feature_vector.Feature_vector_source;
import klikr.machine_learning.similarity.Most_similar;
import klikr.machine_learning.similarity.Similarity_engine;
import klikr.machine_learning.song_similarity.Feature_vector_source_for_song_similarity;
import klikr.util.files_and_paths.*;
import klikr.util.files_and_paths.old_and_new.Command;
import klikr.util.files_and_paths.old_and_new.Old_and_new_Path;
import klikr.util.files_and_paths.old_and_new.Status;
import klikr.util.log.Logger;
import klikr.util.ui.Menu_items;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

//**********************************************************
public class Song
//**********************************************************
{
    public final String path;
    public final  Node node;
    //**********************************************************
    Song(String path, Node node)
    //**********************************************************
    {
        this.path = path;
        this.node = node;
    }
    //**********************************************************
    public void init(Playlist playlist, Aborter aborter, Window owner, Logger logger)
    //**********************************************************
    {
        //logger.log("is visible: "+ path);
        if ( node instanceof Button button)
        {
            button.setOnAction(event ->
            {
                long start = System.currentTimeMillis();
                logger.log("changing song: "+ path);
                playlist.change_song(path, start,false);
            });
        }
        else
        {
            node.setOnMouseClicked(event ->
            {
                if ( event.getButton() != MouseButton.PRIMARY)
                {
                    logger.log("not primary");
                    return;
                }
                long start = System.currentTimeMillis();
                logger.log("changing song: "+ path);
                playlist.change_song(path, start,false);
            });

        }
        add_context_menu_to_node(playlist,aborter,owner,logger);
    }

    //**********************************************************
    public void process_invisible(Logger logger)
    //**********************************************************
    {
        //logger.log("is invisible: "+ path);
        node.setOnMouseClicked(null);
        node.setOnContextMenuRequested(null);
    }


    //**********************************************************
    private void add_context_menu_to_node(
            Playlist playlist,
            Aborter aborter,
            Window owner, Logger logger)
    //**********************************************************
    {
        node.setOnContextMenuRequested((ContextMenuEvent event) ->
                {
                    ContextMenu context_menu = get_context_menu_for_a_song(playlist, path,aborter, owner,logger);
                    context_menu.show(node, event.getScreenX(), event.getScreenY());
                });

    }

    //**********************************************************
    public static ContextMenu get_context_menu_for_a_song(
            Playlist playlist,
            String full_path,
            Aborter aborter,
            Window owner, Logger logger)
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu, owner, logger);

        Menu_items.add_menu_item_for_context_menu(
                "Play_Similar_Song",null,
                (ActionEvent e) ->
                        play_similar(Path.of(full_path), playlist, owner, logger),
                context_menu,
                owner, logger);


        Menu_items.add_menu_item_for_context_menu(
            "Browse_in_new_window",
                null,//(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN)).getDisplayText(),
                (ActionEvent e) ->
                        Instructions.additional_no_past(Window_type.File_system_2D, new Path_list_provider_for_file_system(Path.of(full_path).getParent(), owner, logger), owner, logger),
                context_menu,
                owner, logger);

        Menu_items.add_menu_item_for_context_menu(
                "Rename",
                null,//(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN)).getDisplayText(),

                (ActionEvent e) ->
                {

                    Path new_path =  Static_files_and_paths_utilities.ask_user_for_new_file_name(owner, Path.of(full_path), logger);
                    if ( new_path == null) return;

                    List<Old_and_new_Path> l = new ArrayList<>();
                    Old_and_new_Path oandn = new Old_and_new_Path(Path.of(full_path), new_path, Command.command_rename, Status.before_command, false);
                    l.add(oandn);
                    Moving_files.perform_safe_moves_in_a_thread( l, true, owner.getX()+100, owner.getY()+100,owner, new Aborter("dummy", logger), logger);

                    playlist.remove_from_playlist(full_path);
                    playlist.add_to_playlist(new_path.toAbsolutePath().toString());
                    if ( playlist.the_song_path.equals(full_path)) playlist.the_song_path = new_path.toAbsolutePath().toString();
                },
                context_menu,
                owner, logger);

        Menu_items.add_menu_item_for_context_menu(
                "Remove_From_Playlist",null,
                (ActionEvent e) ->
                        playlist.remove_from_playlist(full_path),
                context_menu, owner, logger);


        {
            String info_string = "Info: ";
            Double dur = Playlist.duration_cache.get(Path.of(full_path), aborter,null, owner);
            if ( dur != null) info_string += String.format("Duration %.1f s ", dur);
            double bitrate = Playlist.bitrate_cache.get(Path.of(full_path), aborter,null, owner);
            if ( bitrate > 0) info_string += String.format(" Bitrate %.0f kb/s", bitrate);
            MenuItem the_menu_item = new MenuItem(info_string);
            Look_and_feel_manager.set_menu_item_look(the_menu_item,owner,logger);
            context_menu.getItems().add(the_menu_item);
            the_menu_item.setOnAction(e-> Audio_info_frame.show(Path.of(full_path),owner,logger));
        }

        Menu_items.add_menu_item_for_context_menu(
                "Edit_Song_Metadata",null,
                (ActionEvent e) -> Ffmpeg_metadata_editor.edit_metadata_of_a_file_in_a_thread(Path.of(full_path), owner, logger),
                context_menu, owner, logger);

        return context_menu;
    }

    //**********************************************************
    private static void play_similar(Path path, Playlist playlist, Window owner, Logger logger)
    //**********************************************************
    {
        Actor_engine.execute(()->
                play_similar_in_thread(path, playlist, owner, logger),"Find and play similar song",logger
        );
    }

    //**********************************************************
    private static void play_similar_in_thread(Path path, Playlist playlist, Window owner, Logger logger)
    //**********************************************************
    {
        long start = System.currentTimeMillis();

        Aborter aborter = new Aborter("dummy",logger);

        Path_comparator_source path_comparator_source = new Path_comparator_source() {
            @Override
            public Comparator<Path> get_path_comparator() {
                return new Comparator<Path>() {
                    @Override
                    public int compare(Path o1, Path o2) {
                        return o1.compareTo(o2);
                    }
                };
            }
        };
        Path_list_provider path_list_provider = new Path_list_provider_for_playlist(Playlist.get_playlist_file(owner).toPath(),owner, logger);
        Similarity_engine similarity_engine = new Similarity_engine(
                path_list_provider.only_file_paths(false),
                path_list_provider,
                path_comparator_source,
                owner,
                aborter,
                logger);
        Feature_vector_source fvs = new Feature_vector_source_for_song_similarity(aborter);
        Feature_vector_cache fvc = new Feature_vector_cache("audio_feature_vector_cache", fvs, aborter, logger);
        Supplier<Feature_vector_cache> fv_cache_supplier = () -> fvc;
        LongAdder count_pairs_examined = new LongAdder();
        List<Most_similar> similars = similarity_engine.find_similars_generic(
                path,
                new ArrayList<>(),
                3,
                100000000.0,// MAGIC
                fv_cache_supplier,
                owner, 100,100,
                count_pairs_examined,
                aborter);

        if ( similars.size() == 0)
        {
            logger.log("no similar song found");
            return;
        }
        Path similar = similars.get(0).path();
        logger.log("similar song found : "+ similar);

        Platform.runLater(()-> playlist.change_song(similar.toAbsolutePath().toString(), start,false));

        aborter.abort("end of similar search");
    }


    //**********************************************************
    public void set_background_to_unselected()
    //**********************************************************
    {
        Platform.runLater(() ->{
            //BackgroundFill background_fill = new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY);
            //((Button)node).setBackground(new Background(background_fill));

            if ( node.getStyleClass().contains("my_button_highlight"))
            {
                node.getStyleClass().remove("my_button_highlight");
                node.getStyleClass().add("my_button");
            }

        });
    }


    //**********************************************************
    public void set_background_to_selected()
    //**********************************************************
    {
        Platform.runLater(() -> {
            //BackgroundFill background_fill = new BackgroundFill(Color.GREY, CornerRadii.EMPTY, Insets.EMPTY);
            //((Button)node).setBackground(new Background(background_fill));

            if (node.getStyleClass().contains("my_button")) {
                node.getStyleClass().remove("my_button");
                node.getStyleClass().add("my_button_highlight");
            }
        });
    }
}
