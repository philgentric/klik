package klik.audio;


import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.browser.icons.animated_gifs.Ffmpeg_utils;
import klik.look.Look_and_feel_manager;
import klik.util.files_and_paths.*;
import klik.util.log.Logger;
import klik.util.ui.Menu_items;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
                    Old_and_new_Path oandn = new Old_and_new_Path(Path.of(full_path), new_path, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command, false);
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
                        playlist.remove_from_playlist(full_path),                context_menu,
                owner, logger);


        {
            String info_string = "Info: ";
            Double dur = Ffmpeg_utils.get_media_duration( Path.of(full_path), owner, logger);
            if ( dur != null) info_string += String.format("Duration %.1f s ", dur);
            double bitrate = Ffmpeg_utils.get_audio_bitrate( Path.of(full_path), owner, logger);
            if ( bitrate > 0) info_string += String.format(" Bitrate %.0f kb/s", bitrate);
            MenuItem the_menu_item = new MenuItem("Info : "+info_string);
            Look_and_feel_manager.set_menu_item_look(the_menu_item,owner,logger);
            context_menu.getItems().add(the_menu_item);
        }

        return context_menu;
    }

}
