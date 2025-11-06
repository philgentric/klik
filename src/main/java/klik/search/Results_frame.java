// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.search;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.Window_type;
import klik.Instructions;
import klik.util.ui.progress.Progress;
import klik.util.execute.actor.Aborter;
import klik.audio.Audio_player_access;
import klik.browser.Drag_and_drop;
import klik.path_lists.Path_list_provider_for_file_system;
import klik.browser.items.Item_file_with_icon;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.path_lists.Path_list_provider;
import klik.look.my_i18n.My_I18n;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.ui.Jfx_batch_injector;
import klik.util.execute.System_open_actor;
import klik.util.files_and_paths.Guess_file_type;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;
import klik.util.ui.Menu_items;
import klik.util.ui.Text_frame;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class Results_frame
//**********************************************************
{
	final Logger logger;
	VBox the_result_vbox = new VBox();
	HashMap<String, List<Path>> search_results;
	Stage stage = new Stage();
    Progress progress;
	VBox vbox;
	//final Browser browser;
	final Aborter aborter;
	private final Path_list_provider path_list_provider;
	private final Path_comparator_source path_comparator_source;

	//**********************************************************
	public Results_frame(
			Path_list_provider path_list_provider,
			Path_comparator_source path_comparator_source,
			Aborter aborter,
			Logger logger)
	//**********************************************************
	{
		this.path_list_provider = path_list_provider;
		this.path_comparator_source = path_comparator_source;
		this.aborter = aborter;
		this.logger = logger;

		vbox = new VBox();
		Look_and_feel_manager.set_region_look(vbox,stage,logger);

		vbox.setAlignment(javafx.geometry.Pos.CENTER);

        progress = Progress.start(vbox,stage,logger);


		ScrollPane scroll_pane = new ScrollPane(the_result_vbox);
		vbox.getChildren().add(scroll_pane);
		Scene scene = new Scene(vbox, 1000, 800);
		//Scene scene = new Scene(scroll_pane, 800, 600);
		Look_and_feel_manager.set_region_look(scroll_pane,stage,logger);

		stage.setTitle(My_I18n.get_I18n_string("Search_Results", stage,logger));
		stage.setScene(scene);
		stage.setX(Finder_frame.MIN_WIDTH);
		stage.setY(0);
		stage.show();

		stage.addEventHandler(KeyEvent.KEY_PRESSED,
				key_event -> {
					if (key_event.getCode() == KeyCode.ESCAPE) {
						stage.close();
						key_event.consume();
					}
				});

		scroll_pane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		scroll_pane.setHbarPolicy(ScrollBarPolicy.ALWAYS);
		scroll_pane.setFitToWidth(true);
		scroll_pane.setFitToHeight(true);
		the_result_vbox.getChildren().clear();
	}

	//**********************************************************
	private void make_one_button(
			Window owner,
			String key, boolean is_max, Path path)
	//**********************************************************
	{
		//Rectangle2D rectangle = new Rectangle2D(window.getX(), window.getY(), window.getWidth(), window.getHeight());

		Button b = new Button(key +" => "+ path);
		if(is_max)
		{
			b.setGraphic(new Circle(10, Color.RED));
		}

		b.setMnemonicParsing(false); // avoid removal of first underscore
		Look_and_feel_manager.set_button_look(b, true,owner,logger);
		if (Files.isDirectory(path)) {
			Border border = new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID,new CornerRadii(5),new BorderWidths(1)));
			b.setBorder(border);
		}
		the_result_vbox.getChildren().add(b);
		b.setOnAction((ActionEvent e) -> {
			//logger.log("going to open on menu select: " + key);

			if (Files.isDirectory(path))
			{
				Instructions.additional_no_past(Window_type.File_system_2D, new Path_list_provider_for_file_system(path),owner,logger);
			}
			else if (Guess_file_type.is_this_file_an_image(path.toFile()))
			{
                Path_list_provider new_path_list_provider = new Path_list_provider_for_file_system(path.getParent());
				Item_file_with_icon.open_an_image(
						new_path_list_provider,
						path_comparator_source,
						path,
						owner,
						logger);
				//Image_window is = Image_window.get_Image_window(the_browser, path, logger);
			} else if (Guess_file_type.is_this_path_a_music(path)) {
				logger.log("opening audio file: " + path.toAbsolutePath());
				Audio_player_access.play_song_in_separate_process(path.toFile(), logger);
			} else if (Guess_file_type.is_this_path_a_text(path)) {
				logger.log("opening text file: " + path.toAbsolutePath());
				Text_frame.show(path, logger);
			} else {
				System_open_actor.open_with_system(path, stage, aborter, logger);
			}
		});

		// add a menu to the button!
		ContextMenu context_menu = new ContextMenu();
		Look_and_feel_manager.set_context_menu_look(context_menu,stage,logger);


        Menu_items.add_menu_item("Browse_in_new_window",
                e -> {
			//logger.log("Browse_in_new_window");
			Path local = path;
			if (! local.toFile().isDirectory()) local = local.getParent();
			Instructions.additional_no_past(Window_type.File_system_2D,new Path_list_provider_for_file_system(local),owner,logger);
		},context_menu,owner,logger);

		if (! path.toFile().isDirectory())
		{
			Menu_items.add_menu_item("Open_With_Registered_Application",
                    e-> {
					logger.log("Open_With_Registered_Application");
					System_open_actor.open_special(path,owner, aborter,logger);
				},context_menu,owner,logger);

            Menu_items.add_menu_item("Delete",
                    e -> {
                logger.log("Delete");
                double x = stage.getX()+100;
                double y = stage.getY()+100;
                Static_files_and_paths_utilities.move_to_trash(path,stage,x,y, null, aborter, logger);
                // need to remove the button from the list
                the_result_vbox.getChildren().remove(b);
            },context_menu,owner,logger);

			{
				double x = stage.getX()+100;
				double y = stage.getY()+100;
				MenuItem rename = Item_file_with_icon.get_rename_MenuItem(path,stage,x, y, aborter,logger);
				context_menu.getItems().add(rename);
			}

		}

		b.setOnContextMenuRequested((ContextMenuEvent event) -> {
			logger.log("show context menu of button:"+ path.toAbsolutePath());
			context_menu.show(b, event.getScreenX(), event.getScreenY());
		});


		Drag_and_drop.init_drag_and_drop_sender_side(b, null,path,logger);


	}


	//**********************************************************
	private static final Comparator<? super String> string_length_comparator = (Comparator<String>) (o1, o2) -> Integer.compare(o2.length(), o1.length());


	//**********************************************************
	public void inject_search_results(Search_result sr, String keys, boolean is_max, Window window)
	//**********************************************************
	{
		if ( search_results == null) search_results = new HashMap<>();
        List<Path> path_set = search_results.computeIfAbsent(keys, (s) -> new ArrayList<>());

        path_set.add(sr.path());

		Jfx_batch_injector.inject(() -> make_one_button(window, keys, is_max, sr.path()),logger);

	}

	//**********************************************************
	public void has_ended()
	//**********************************************************
	{

		Jfx_batch_injector.inject(() -> {
			stage.setTitle(My_I18n.get_I18n_string("Search_Results_Ended", stage,logger));
			//stage.getScene().getRoot().setCursor(Cursor.DEFAULT);

            progress.stop();
			List<Node> all_results = new ArrayList<>(the_result_vbox.getChildren());
			all_results.sort((o1, o2) -> {
				Button b1 = (Button) o1;
				Button b2 = (Button) o2;
				if (b1.getGraphic() != null && b2.getGraphic() == null) return -1;
				if (b1.getGraphic() == null && b2.getGraphic() != null) return 1;
				return string_length_comparator.compare(b1.getText(), b2.getText());
			});
			the_result_vbox.getChildren().clear();
			the_result_vbox.getChildren().addAll(all_results);

            progress.remove();

		},logger);


	}

	public void sort()
	{
	}
}
