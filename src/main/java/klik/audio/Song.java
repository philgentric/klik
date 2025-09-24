package klik.audio;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.icons.animated_gifs.Ffmpeg_utils;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.experimental.image_playlist.Playlist_path_list_provider;
import klik.look.Look_and_feel_manager;
import klik.machine_learning.feature_vector.Feature_vector_cache;
import klik.machine_learning.feature_vector.Feature_vector_source;
import klik.machine_learning.similarity.Most_similar;
import klik.machine_learning.similarity.Similarity_engine;
import klik.machine_learning.song_similarity.Feature_vector_source_for_song_similarity;
import klik.util.files_and_paths.*;
import klik.util.log.Logger;
import klik.util.ui.Menu_items;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

//**********************************************************
public record Song(String path, Node node)
//**********************************************************
{
    //**********************************************************
    public void process_visible(Playlist playlist, Window owner, Logger logger)
    //**********************************************************
    {
        //logger.log("is visible: "+ path);
        if ( node instanceof Button button)
        {
            button.setOnAction(event ->
            {
                logger.log("changing song: "+ path);
                playlist.change_song(path);
            });
        }
        else {
            node.setOnMouseClicked(event ->
            {
                if ( event.getButton() != MouseButton.PRIMARY)
                {
                    logger.log("not primary");
                    return;
                }
                logger.log("changing song: "+ path);
                playlist.change_song(path);
            });

        }
        add_context_menu_to_node(playlist,owner,logger);
    }

    //**********************************************************
    public void process_invisible(Logger logger)
    //**********************************************************
    {
        //logger.log("is invisible: "+ path);
        node.setOnMouseClicked(null);
        node().setOnContextMenuRequested(null);
    }


    //**********************************************************
    private void add_context_menu_to_node(
            Playlist playlist,
            Window owner, Logger logger)
    //**********************************************************
    {
        ContextMenu context_menu = get_context_menu_for_a_song(playlist, path(),owner,logger);
        node().setOnContextMenuRequested((ContextMenuEvent event) -> context_menu.show(node(), event.getScreenX(), event.getScreenY()));

    }

    //**********************************************************
    public static ContextMenu get_context_menu_for_a_song(
            Playlist playlist,
            String full_path,
            Window owner, Logger logger)
    //**********************************************************
    {
        ContextMenu context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(context_menu, owner, logger);

        Menu_items.add_menu_item(
                "Play_Similar_Song",
                (ActionEvent e) ->
                        play_similar(Path.of(full_path), playlist, owner, logger),
                context_menu,
                owner, logger);


        Menu_items.add_menu_item(
            "Browse",
            (ActionEvent e) ->
            Audio_player.start_new_process_to_browse(Path.of(full_path).getParent(), logger),
            context_menu,
                owner, logger);

        Menu_items.add_menu_item(
                "Rename",
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

        Menu_items.add_menu_item(
                "Remove_From_Playlist",
                (ActionEvent e) ->
                        playlist.remove_from_playlist(full_path),
                context_menu, owner, logger);


        {
            String info_string = "Info: ";
            Double dur = Ffmpeg_utils.get_media_duration( Path.of(full_path), owner, logger);
            if ( dur != null) info_string += String.format("Duration %.1f s ", dur);
            double bitrate = Ffmpeg_utils.get_audio_bitrate( Path.of(full_path), owner, logger);
            if ( bitrate > 0) info_string += String.format(" Bitrate %.0f kb/s", bitrate);
            MenuItem the_menu_item = new MenuItem("Info : "+info_string);
            Look_and_feel_manager.set_menu_item_look(the_menu_item,owner,logger);
            context_menu.getItems().add(the_menu_item);
            the_menu_item.setOnAction(e->Audio_info_frame.show(Path.of(full_path),owner,logger));
        }

        Menu_items.add_menu_item(
                "Edit_Song_Metadata",
                (ActionEvent e) -> Ffmpeg_metadata_editor.edit_metadata_of_a_file_in_a_thread(Path.of(full_path), owner, logger),
                context_menu, owner, logger);

        return context_menu;
    }

    //**********************************************************
    private static void play_similar(Path path, Playlist playlist, Window owner, Logger logger)
    //**********************************************************
    {
        Actor_engine.execute(()->
                play_similar_in_thread(path, playlist, owner, logger),logger
        );
    }

    //**********************************************************
    private static void play_similar_in_thread(Path path, Playlist playlist, Window owner, Logger logger)
    //**********************************************************
    {

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
        Path_list_provider path_list_provider = new Playlist_path_list_provider(Playlist.get_playlist_file(owner).toPath(),logger);
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
        AtomicLong count_pairs_examined = new AtomicLong(0);
        List<Most_similar> similars = similarity_engine.find_similars(
                path,
                new ArrayList<>(),
                3,
                false,
                100000000.0,
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

        Platform.runLater(()-> playlist.change_song(similar.toAbsolutePath().toString()));
    }

}
