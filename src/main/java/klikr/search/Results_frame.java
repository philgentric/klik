// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.search;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.Window_type;
import klikr.Instructions;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.Scrollable_text_field;
import klikr.util.ui.progress.Progress;
import klikr.util.execute.actor.Aborter;
import klikr.audio.Audio_player_gradle_start;
import klikr.browser.Drag_and_drop;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.browser.items.Item_file_with_icon;
import klikr.browser.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.look.my_i18n.My_I18n;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.execute.System_open_actor;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.look.Look_and_feel_manager;
import klikr.util.log.Logger;
import klikr.util.ui.Menu_items;
import klikr.util.ui.Text_frame;

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
    HashMap<Search_result, Boolean> search_results_is_max;
    HashMap<Button, Search_result> search_results_buttons;
	Stage stage = new Stage();
    Progress progress;
	VBox vbox;
	//final Browser browser;
	final Aborter aborter;
	private final Path_list_provider path_list_provider;
	private final Path_comparator_source path_comparator_source;
    private final static boolean use_scrollable_textfield = true;

    //**********************************************************
	public Results_frame(
			Path_list_provider path_list_provider,
			Path_comparator_source path_comparator_source,
			Aborter aborter,
            Window owner,
			Logger logger)
	//**********************************************************
	{
		this.path_list_provider = path_list_provider;
		this.path_comparator_source = path_comparator_source;
		this.aborter = aborter;
		this.logger = logger;

        stage.initOwner(owner);

		vbox = new VBox();
		Look_and_feel_manager.set_region_look(vbox,stage,logger);
		vbox.setAlignment(javafx.geometry.Pos.CENTER);
        progress = Progress.start(vbox,stage,logger);
        the_result_vbox.getChildren().add(vbox);

		ScrollPane scroll_pane = new ScrollPane(the_result_vbox);
        Scene scene = new Scene(scroll_pane, 1000, 800);
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
		scroll_pane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scroll_pane.setFitToWidth(true);
		scroll_pane.setFitToHeight(true);
		the_result_vbox.getChildren().clear();
	}

	//**********************************************************
	private Button make_one_button(String keys, boolean is_max, Path path, Window owner)
	//**********************************************************
	{

        String displayed_text = path.toAbsolutePath().toString();
        /*Path folder = path_list_provider.get_folder_path();
        int i = folder.toAbsolutePath().toString().length();
        if ( i < displayed_text.length())
        {
            displayed_text = keys + " => " + displayed_text.substring(i);
        }*/

        Button b = new Button();
        b.maxWidthProperty().bind(stage.widthProperty().subtract(1));

        Node graphic = null;
        if ( use_scrollable_textfield)
        {
            graphic = new Scrollable_text_field(displayed_text,path, b,owner,aborter,logger);
        }
        else
		{
            HBox hbox = new HBox();
            hbox.setAlignment(Pos.BASELINE_LEFT);
            if (is_max) {
                hbox.getChildren().add(new Circle(10, Color.RED));
            }
            Text text = new Text(displayed_text);
            text.wrappingWidthProperty().bind(stage.widthProperty().subtract(70));
            hbox.getChildren().add(text);
            graphic = hbox;
        }
        if(is_max)
        {
            b.setGraphic(new Circle(10, Color.RED));
        }
        b.setGraphic(graphic);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

		//Look_and_feel_manager.set_button_look(b, true,owner,logger);
		if (Files.isDirectory(path))
        {
            Look_and_feel_manager.set_button_look(b, true,owner,logger);
		}
        else
        {
            Look_and_feel_manager.set_button_look(b, false,owner,logger);
        }
		the_result_vbox.getChildren().add(b);
		b.setOnAction((ActionEvent e) -> {
			//logger.log("going to open on menu select: " + key);

			open(path, owner);
		});

		// add a menu to the button!
		ContextMenu context_menu = new ContextMenu();
		Look_and_feel_manager.set_context_menu_look(context_menu,stage,logger);


        Menu_items.create_browse_in_new_window_menu_item(context_menu,path,owner,logger);

		if (! path.toFile().isDirectory())
		{
			Menu_items.create_open_with_klik_registered_application_menu_item(context_menu,path,owner,aborter,logger);

            Menu_items.add_menu_item_for_context_menu("Delete",
                    (new KeyCodeCombination(KeyCode.BACK_SPACE)).getDisplayText(),
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
			//logger.log("show context menu of button:"+ path.toAbsolutePath());
			context_menu.show(b, event.getScreenX(), event.getScreenY());
		});


		Drag_and_drop.init_drag_and_drop_sender_side(b, null,path,logger);

        return b;
	}

	//**********************************************************
	private void open(Path path, Window owner)
	//**********************************************************
	{
		Actor_engine.execute(()-> Platform.runLater(()->open_internal(path,owner)),"opening search result",logger);
	}

	//**********************************************************
	private void open_internal(Path path, Window owner)
	//**********************************************************
	{
		if (Files.isDirectory(path))
		{
			Instructions.additional_no_past(Window_type.File_system_2D, new Path_list_provider_for_file_system(path, owner,logger), owner,logger);
		}
		else if (Guess_file_type.is_this_file_an_image(path.toFile(), owner, logger))
		{
			Path_list_provider new_path_list_provider = new Path_list_provider_for_file_system(path.getParent(), owner,logger);
			Item_file_with_icon.open_an_image(
					new_path_list_provider,
					path_comparator_source,
					path,
					owner,
					logger);
			//Image_window is = Image_window.get_Image_window(the_browser, path, logger);
		} else if (Guess_file_type.is_this_path_a_music(path, owner, logger)) {
			logger.log("opening audio file: " + path.toAbsolutePath());
			Audio_player_gradle_start.play_song_in_separate_process(path.toFile(), logger);
		} else if (Guess_file_type.is_this_path_a_text(path, owner, logger)) {
			logger.log("opening text file: " + path.toAbsolutePath());
			Text_frame.show(path, logger);
		} else {
			System_open_actor.open_with_system(path, stage, aborter, logger);
		}
	}


	//**********************************************************
	private static final Comparator<? super String> string_length_comparator = (Comparator<String>) (o1, o2) -> Integer.compare(o2.length(), o1.length());


	//**********************************************************
	public void inject_search_results(Search_result sr, String keys, boolean is_max, Window window)
	//**********************************************************
	{
		if ( search_results == null) search_results = new HashMap<>();
        if ( search_results_is_max == null) search_results_is_max = new HashMap<>();
        if ( search_results_buttons == null) search_results_buttons = new HashMap<>();
        search_results_is_max.put(sr,is_max);
        List<Path> path_set = search_results.computeIfAbsent(keys, (s) -> new ArrayList<>());

        path_set.add(sr.path());

		Jfx_batch_injector.inject(() ->
                {
                    Button b = make_one_button(keys, is_max, sr.path(),window);
                    search_results_buttons.put(b,sr);
                },logger);

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

    //**********************************************************
    public void sort()
    //**********************************************************
    {
	}

    //**********************************************************
    public void erase_all_non_max()
    //**********************************************************
    {
        Jfx_batch_injector.inject(() -> {
            List<Node> to_be_deleted = new ArrayList<>();
            for( Node n : the_result_vbox.getChildren())
            {
                if ( n instanceof  Button button)
                {
                    Search_result  sr =  search_results_buttons.get(button);
                    if ( sr == null)
                    {
                        logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
                    }
                    else
                    {
                        Boolean bool = search_results_is_max.get(sr);
                        if ( bool == null)
                        {
                            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
                        }
                        else
                        {
                            if (!bool)  to_be_deleted.add(button);
                        }
                    }
                }
            }
            the_result_vbox.getChildren().removeAll(to_be_deleted);
        },logger);


    }
}
