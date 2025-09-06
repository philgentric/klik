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
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.util.files_and_paths.*;
import klik.util.log.Logger;

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
        ContextMenu the_context_menu = new ContextMenu();
        Look_and_feel_manager.set_context_menu_look(the_context_menu, owner, logger);
        {
            MenuItem the_menu_item = new MenuItem("Browse folder");
            Look_and_feel_manager.set_menu_item_look(the_menu_item,owner,logger);

            the_menu_item.setOnAction((s) -> Audio_player.start_new_process_to_browse(Path.of(path()).getParent(), logger));
            the_context_menu.getItems().add(the_menu_item);
        }
        {
            MenuItem the_menu_item = new MenuItem(My_I18n.get_I18n_string("Rename", owner, logger));
            Look_and_feel_manager.set_menu_item_look(the_menu_item,owner,logger);
            the_menu_item.setOnAction((ActionEvent s) -> {

                Path new_path =  Static_files_and_paths_utilities.ask_user_for_new_file_name(owner, Path.of(path()), logger);
                if ( new_path == null) return;

                List<Old_and_new_Path> l = new ArrayList<>();
                Old_and_new_Path oandn = new Old_and_new_Path(Path.of(path()), new_path, Command_old_and_new_Path.command_rename, Status_old_and_new_Path.before_command, false);
                l.add(oandn);
                Moving_files.perform_safe_moves_in_a_thread( l, true, owner.getX()+100, owner.getY()+100,owner, new Aborter("dummy", logger), logger);

                playlist.remove_from_playlist(path());
                playlist.add_to_playlist(new_path.toAbsolutePath().toString());
                if ( playlist.the_song_path.equals(path())) playlist.the_song_path = new_path.toAbsolutePath().toString();
            });
            the_context_menu.getItems().add(the_menu_item);
        }
        {
            MenuItem the_menu_item = new MenuItem("Remove from list");
            Look_and_feel_manager.set_menu_item_look(the_menu_item,owner,logger);
            the_menu_item.setOnAction((ActionEvent s) -> playlist.remove_from_playlist(path()));
            the_context_menu.getItems().add(the_menu_item);
        }
        node().setOnContextMenuRequested((ContextMenuEvent event) -> the_context_menu.show(node(), event.getScreenX(), event.getScreenY()));
    }

}
