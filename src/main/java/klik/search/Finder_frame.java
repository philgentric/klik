// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Search_receiver.java
//SOURCES ./Keyword_slot.java
//SOURCES ./Search_session.java
//SOURCES ./Search_statistics.java
//SOURCES ./Search_status.java

package klik.search;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import klik.util.execute.actor.Aborter;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.path_lists.Path_list_provider;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Booleans;
import klik.util.files_and_paths.Ding;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.ui.progress.Hourglass;
import klik.util.ui.progress.Progress_window;

import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class Finder_frame implements Search_receiver
//**********************************************************
{
	public static final int MIN_WIDTH = 600;
	private static final String BASE_ = "<type new keyword>";
	private Button start;
	private Button stop;
	Label visited_folders;
	Label visited_files;
	Logger logger;
	private final Aborter aborter;

	private Path target_path;

	final private Map<String, Keyword_slot> keyword_to_slot =  new HashMap<>(); // this is the textfield to report the number of matches
	final Stage stage;
	private boolean look_only_for_images = false;
	private boolean use_extension = false;
    private boolean search_folders_names = true;
    private boolean ignore_hidden = true;
	private boolean search_files_names = true;
	private boolean check_case = false;
	Search_session session;

	long start_time;
	TextField extension_tf;
	VBox top_keyword_vbox;
	VBox bottom_keyword_vbox;
	private boolean extension_textfield_is_red = false;
	private boolean new_keyword_textfield_is_red = false;
	private final Path_list_provider path_list_provider;
	private final Path_comparator_source path_comparator_source;

    private Hourglass hourglass;

	//**********************************************************
	public Finder_frame(
			List<String> input_keywords,
			boolean look_only_for_images,
			Path_list_provider path_list_provider,
			Path_comparator_source path_comparator_source,
			Aborter aborter,
			Logger logger)
	//**********************************************************
	{
		this.aborter = aborter;
		this.path_list_provider = path_list_provider;
		this.path_comparator_source = path_comparator_source;
		this.logger = logger;
		this.look_only_for_images = look_only_for_images;
		if ( !path_list_provider.get_folder_path().toFile().isDirectory())
		{
			logger.log(Stack_trace_getter.get_stack_trace("Not a directory: "+ path_list_provider.get_name()));
		}
		stage = new Stage();

		logger.log("Finder_frame created");

		stage.setOnCloseRequest((WindowEvent e ) -> {if ( session !=null) session.stop_search();});

		stage.addEventHandler(KeyEvent.KEY_PRESSED,
				key_event -> {
					if (key_event.getCode() == KeyCode.ESCAPE) {
						stage.close();
						session.stop_search();
						key_event.consume();
					}
				});

		Pane main_vbox = define_main_vbox(input_keywords);
        ScrollPane sp = new ScrollPane(main_vbox);
        sp.setFitToWidth(true);
		Scene scene = new Scene(sp);
		Look_and_feel_manager.set_region_look(main_vbox,stage,logger);

		stage.setTitle(My_I18n.get_I18n_string("Search_by_keywords", stage,logger));
		//stage.setMinWidth(MIN_WIDTH);
		stage.setX(0);
		stage.setScene(scene);
		stage.show();
		stage.sizeToScene();
	}

	//**********************************************************
	private Pane define_main_vbox(List<String> input_keywords)
	//**********************************************************
	{
		VBox the_main_pane = new VBox();
		Pane settings = define_settings_pane(input_keywords);
		the_main_pane.getChildren().add(settings);

		{
			HBox hbox = new HBox();

			Label static_visited_folders = new Label(My_I18n.get_I18n_string("Visited_Folders", stage,logger));
			hbox.getChildren().add(static_visited_folders);
			Look_and_feel_manager.set_region_look(static_visited_folders,stage,logger);

			hbox.getChildren().add(horizontal_spacer(stage,logger));

			visited_folders = new Label();
			hbox.getChildren().add(visited_folders);
			Look_and_feel_manager.set_region_look(visited_folders,stage,logger);

			the_main_pane.getChildren().add(hbox);

		}
		{
			HBox hbox = new HBox();

			Label static_visited_files = new Label(My_I18n.get_I18n_string("Visited_Files", stage,logger));
			hbox.getChildren().add(static_visited_files);
			Look_and_feel_manager.set_region_look(static_visited_files,stage,logger);

			hbox.getChildren().add(horizontal_spacer(stage,logger));

			visited_files = new Label();
			hbox.getChildren().add(visited_files);
			Look_and_feel_manager.set_region_look(visited_files,stage,logger);

			the_main_pane.getChildren().add(hbox);
		}

		return the_main_pane;
	}

	//**********************************************************
	private Pane define_settings_pane(List<String> input_keywords)
	//**********************************************************
	{
		VBox settings_vbox = new VBox();
		{
			target_path = path_list_provider.get_folder_path();
			Label target_folder_label = new Label(target_path.toAbsolutePath().toString());
			settings_vbox.getChildren().add(target_folder_label);
			Button up = new Button(My_I18n.get_I18n_string("Search_Parent_Folder", stage,logger));
			Look_and_feel_manager.set_button_look(up,true,stage,logger);

			settings_vbox.getChildren().add(up);

			up.setOnAction((ActionEvent e) -> {
                session.stop_search();
                Path parent = target_path.getParent();
                if (parent != null)
                {
                    target_path = parent;
                    target_folder_label.setText(target_path.toAbsolutePath().toString());
                    start_search();
                }
            });
		}
		{
			CheckBox search_folder_names_cb = new CheckBox(My_I18n.get_I18n_string("Search_Folder_names", stage,logger));
			search_folder_names_cb.setSelected(search_folders_names);
			Look_and_feel_manager.set_CheckBox_look(search_folder_names_cb,stage,logger);
			search_folder_names_cb.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean new_value) -> {
                search_folders_names = new_value;
                logger.log("search_folders_names = "+ search_folders_names);
            });
			settings_vbox.getChildren().add(search_folder_names_cb);
		}
        {
            CheckBox ignore_hidden_cb = new CheckBox(My_I18n.get_I18n_string("Ignore_hidden_items", stage,logger));
            ignore_hidden_cb.setSelected(ignore_hidden);
            Look_and_feel_manager.set_CheckBox_look(ignore_hidden_cb,stage,logger);
            ignore_hidden_cb.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean new_value) -> {
                ignore_hidden = new_value;
                logger.log("ignore_hidden = "+ ignore_hidden);
            });
            settings_vbox.getChildren().add(ignore_hidden_cb);
        }
		{
			CheckBox search_file_names_cb = new CheckBox(My_I18n.get_I18n_string("Search_File_names", stage,logger));
			search_file_names_cb.setSelected(search_files_names);
			Look_and_feel_manager.set_CheckBox_look(search_file_names_cb,stage,logger);
			search_file_names_cb.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean new_value) -> {
                search_files_names = new_value;
                logger.log("search_files_names = "+ search_files_names);
            });
			settings_vbox.getChildren().add(search_file_names_cb);
		}
		settings_vbox.getChildren().add(vertical_spacer());
		{
			CheckBox only_images = new CheckBox(My_I18n.get_I18n_string("Search_Only_Images", stage,logger));
			only_images.setSelected(look_only_for_images);
			Look_and_feel_manager.set_CheckBox_look(only_images,stage,logger);
			only_images.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean new_value) -> look_only_for_images = new_value);
			settings_vbox.getChildren().add(only_images);
		}
		settings_vbox.getChildren().add(vertical_spacer());

		{
			CheckBox check_case_cb = new CheckBox(My_I18n.get_I18n_string("Check_Case", stage,logger));
			check_case_cb.setSelected(check_case);
			Look_and_feel_manager.set_CheckBox_look(check_case_cb,stage,logger);
			check_case_cb.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean new_value) -> check_case = new_value);
			settings_vbox.getChildren().add(check_case_cb);
		}
		settings_vbox.getChildren().add(vertical_spacer());

		{
			HBox hb = new HBox();
			CheckBox use_extension_cb = new CheckBox(My_I18n.get_I18n_string("Use_Extension", stage,logger)+ "(e.g. pdf,jpg)");
			use_extension_cb.setSelected(use_extension);
			Look_and_feel_manager.set_CheckBox_look(use_extension_cb,stage,logger);
			use_extension_cb.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean new_value) -> {
                use_extension = new_value;
                if (use_extension)
                {
                    if(!extension_tf.getText().isBlank())
                    {
                        session.stop_search();
                        add_keyword_slot(extension_tf.getText().trim(), true);
                        start_search();
                    }
                }
                else
                {
                    session.stop_search();
                    Keyword_slot kts = keyword_to_slot.get(extension_tf.getText());
                    top_keyword_vbox.getChildren().remove(kts.hbox1);
                    bottom_keyword_vbox.getChildren().remove(kts.hbox2);
                    keyword_to_slot.remove(extension_tf.getText());
                    start_search();
                }
            });
			hb.getChildren().add(use_extension_cb);
			extension_tf = new TextField("");
			extension_tf.setMaxWidth(100);
			extension_tf.setOnAction((ActionEvent e) -> {
				extension_textfield_is_red = false;
				extension_tf.setStyle(extension_tf.getStyle()+"-fx-text-inner-color: blue;");
				use_extension = true;
				use_extension_cb.setSelected(true);
				session.stop_search();
				add_keyword_slot(extension_tf.getText().trim(), true);
				start_search();
			});

			extension_tf.textProperty().addListener((observable, old_val, new_val) -> {
				if ( !extension_textfield_is_red)
				{
					extension_tf.setStyle(extension_tf.getStyle()+"-fx-text-inner-color: red;");
					extension_textfield_is_red = true;
				}
				logger.log("extension_tf  old_val:"+old_val+" new_val:"+new_val);
			});

			Look_and_feel_manager.set_TextField_look(extension_tf,stage,logger);
			hb.getChildren().add(extension_tf);
			hb.getChildren().add(horizontal_spacer(stage,logger));
			settings_vbox.getChildren().add(hb);
		}
		settings_vbox.getChildren().add(vertical_spacer());


		top_keyword_vbox = new VBox();
		settings_vbox.getChildren().add(top_keyword_vbox);

		bottom_keyword_vbox = new VBox();
		settings_vbox.getChildren().add(bottom_keyword_vbox);


		{
			HBox hbox = new HBox();
			TextField new_keyword_textfield = new TextField(BASE_);
			new_keyword_textfield.setStyle(new_keyword_textfield.getStyle()+"-fx-text-inner-color: blue;");
			new_keyword_textfield.textProperty().addListener((observable, old_val, new_val) -> {
				if ( !new_keyword_textfield_is_red)
				{
					new_keyword_textfield.setStyle(new_keyword_textfield.getStyle()+"-fx-text-inner-color: red;");
					new_keyword_textfield_is_red = true;
				}
				logger.log("new_keyword_textfield  old_val:"+old_val+" new_val:"+new_val);
			});

			new_keyword_textfield.setMinWidth(300);
			Look_and_feel_manager.set_TextField_look(new_keyword_textfield,stage,logger);
			new_keyword_textfield.setStyle(new_keyword_textfield.getStyle()+"-fx-text-inner-color: darkgrey;");
			new_keyword_textfield.setOnAction((ActionEvent e) ->new_keyword_action(new_keyword_textfield));
			hbox.getChildren().add(new_keyword_textfield);
			hbox.getChildren().add(horizontal_spacer(stage,logger));

			Button add_keyword = new Button(My_I18n.get_I18n_string("Add_Keyword", stage,logger));
			Look_and_feel_manager.set_button_look(add_keyword,true,stage,logger);
			add_keyword.setOnAction((ActionEvent e) -> new_keyword_action(new_keyword_textfield));
			hbox.getChildren().add(add_keyword);
			top_keyword_vbox.getChildren().add(hbox);
		}
		top_keyword_vbox.getChildren().add(vertical_spacer());
		for(String keyword : input_keywords )
		{
			if ( keyword.trim().isBlank()) continue;
			add_keyword_slot(keyword.trim(),false);
		}
		top_keyword_vbox.getChildren().add(vertical_spacer());

		start = new Button(My_I18n.get_I18n_string("Start_Search", stage,logger));
		start.setOnAction((ActionEvent e) -> start_search());
		settings_vbox.getChildren().add(start);
		Look_and_feel_manager.set_button_look(start,true,stage,logger);

		settings_vbox.getChildren().add(vertical_spacer());
		stop = new Button(My_I18n.get_I18n_string("Stop_Search", stage,logger));
		stop.setDisable(true);
		stop.setOnAction((ActionEvent e) -> {
            session.stop_search();
            stop.setDisable(true);
        });
		settings_vbox.getChildren().add(stop);
		Look_and_feel_manager.set_button_look(stop,true,stage,logger);
		settings_vbox.getChildren().add(vertical_spacer());


		return settings_vbox;
	}


	//**********************************************************
	private void new_keyword_action(TextField new_keyword_textfield)
	//**********************************************************
	{
		introduce_new_keyword(new_keyword_textfield.getText().trim());
		new_keyword_textfield.setText(BASE_);
		new_keyword_textfield.setStyle(new_keyword_textfield.getStyle()+"-fx-text-inner-color: blue;");
		new_keyword_textfield_is_red = false;
	}

	//**********************************************************
	private void introduce_new_keyword(String new_keyword)
	//**********************************************************
	{
		if (new_keyword.isBlank()) return;
		if (keyword_to_slot.containsKey(new_keyword)) return;
		session.stop_search();
		add_keyword_slot(new_keyword,false);
		stage.sizeToScene();
		start_search();
	}


	//**********************************************************
	private void add_keyword_slot(String local_keyword, boolean is_extension)
	//**********************************************************
	{
		Keyword_slot ks = new Keyword_slot(local_keyword,this,is_extension,logger);
		keyword_to_slot.put(local_keyword,ks);
	}

	//**********************************************************
	public static Node horizontal_spacer(Window owner, Logger logger)
	//**********************************************************
	{
		final Region spacer = new Region();
        Look_and_feel_manager.set_region_look(spacer,owner,logger);
        HBox.setHgrow(spacer, Priority.ALWAYS);
		return spacer;
	}

	//**********************************************************
	private Node vertical_spacer()
	//**********************************************************
	{
		final Region spacer = new Region();
		spacer.setMinHeight(8);
		spacer.setPrefHeight(8);
		spacer.setMaxHeight(8);
		VBox.setVgrow(spacer, Priority.ALWAYS);
        Look_and_feel_manager.set_region_look(spacer,stage,logger);
		return spacer;
	}




	//**********************************************************
	@Override // Search_receiver
	public void receive_intermediary_statistics(Search_statistics search_statistics)
	//**********************************************************
	{
		Jfx_batch_injector.inject(() -> {

            for (String input_keyword: keyword_to_slot.keySet())
            {
                Keyword_slot ks = keyword_to_slot.get(input_keyword);
                Label t = ks.get_result_label();
                if (t == null) {
                    System.out.println("SHOULD NOT HAPPEN: no Text component in UI for keyword ->" + input_keyword + "<-");
                } else {
                    if (search_statistics.matched_keyword_counts().get(input_keyword) == null )
                    {
                        t.setText(String.valueOf( 0));
                    }
                    else {
                        t.setText(String.valueOf( search_statistics.matched_keyword_counts().get(input_keyword)));
                    }
                }
            }
            visited_files.setText(""+search_statistics.visited_files());
            visited_folders.setText(""+search_statistics.visited_folders());

        },logger);


	}
	//**********************************************************
	@Override // Search_receiver
	public void has_ended(Search_status search_status)
	//**********************************************************
	{
        if( hourglass!=null) hourglass.close();
		//logger.log("has_ended() "+search_status);
		if ( search_status != Search_status.interrupted)
		{
			long now = System.currentTimeMillis();
			if (now - start_time > 3000) {
				if (Booleans.get_boolean(Feature.Play_ding_after_long_processes.name(),stage)) {
					Ding.play("File finder took more than 3 seconds", logger);
				}
			}
		}
		if ( search_status == Search_status.invalid)
		{
			Jfx_batch_injector.inject(() -> {
                stop.setDisable(true);
                start.setDisable(false);
            },logger);
			return;
		}
		Jfx_batch_injector.inject(() -> {
            stop.setDisable(true);
            start.setDisable(false);
        },logger);

	}

	//**********************************************************
	void start_search()
	//**********************************************************
	{
        double x = stage.getX()+100;
        double y = stage.getY()+100;

        hourglass = Progress_window.show(false,"Searching",10*60*60,x,y,stage,logger);
		List<String> keywords = new ArrayList<>(keyword_to_slot.keySet());
        logger.log("Finder_frame::start_search() "+keywords+" in: "+target_path);

		String local_extension = null;
		if ( use_extension)
		{
			String extension = extension_tf.getText();
			if (extension != null)
			{
				if (!extension.isBlank())
				{
					local_extension = extension.trim().toLowerCase();
					logger.log("extension="+local_extension);
					keywords.remove(local_extension);
				}
			}
		}
		Search_config search_config = new Search_config(target_path,keywords,look_only_for_images,local_extension, search_folders_names,search_files_names, ignore_hidden, check_case);
		session = new Search_session(
				path_list_provider,
				path_comparator_source,
				search_config,
				this,stage,logger);
		session.start_search();
		stop.setDisable(false);
		start.setDisable(true);
		start_time = System.currentTimeMillis();
	}

	public Map<String, Keyword_slot> get_keyword_to_slot() {
		return keyword_to_slot;
	}
}
