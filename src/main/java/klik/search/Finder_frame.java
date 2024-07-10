//SOURCES ./Search_receiver.java
//SOURCES ./Keyword_slot.java
//SOURCES ./Search_session.java
//SOURCES ./Search_statistics.java
//SOURCES ./Search_status.java

package klik.search;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import klik.browser.Browser;
import klik.files_and_paths.Ding;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.I18n;
import klik.properties.Static_application_properties;
import klik.util.Fx_batch_injector;
import klik.util.Logger;

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

	final private Map<String, Keyword_slot> keyword_to_slot =  new HashMap<>(); // this is the textfield to report the number of matches
	private final Stage stage;
	private boolean look_only_for_images = false;
	private boolean use_extension = false;
	private boolean also_folders = true;
	private boolean check_case = false;
	Search_session session;
	Path target_path;

	Browser browser;
	long start_time;
	TextField extension_tf;
	VBox top_keyword_vbox;
	VBox bottom_keyword_vbox;
	private boolean extension_textfield_is_red = false;
	private boolean new_keyword_textfield_is_red = false;

	//**********************************************************
	public Finder_frame(Path target_path_, List<String> input_keywords, boolean look_only_for_images_, Browser browser, Logger logger_)
	//**********************************************************
	{
		this.target_path = target_path_;
		if ( !target_path.toFile().isDirectory()) target_path = target_path.getParent();
		look_only_for_images = look_only_for_images_;
		this.browser = browser;
		logger = logger_;
		stage = new Stage();


		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent windowEvent) {
				session.stop_search();
			}
		});

		stage.addEventHandler(KeyEvent.KEY_PRESSED,
				key_event -> {
					if (key_event.getCode() == KeyCode.ESCAPE) {
						stage.close();
						session.stop_search();
						key_event.consume();
					}
				});

		Pane main_vbox = define_main_vbox(input_keywords);
		Scene scene = new Scene(main_vbox);
		Look_and_feel_manager.set_region_look(main_vbox);

		stage.setTitle(I18n.get_I18n_string("Search_by_keywords", logger));
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

			Label static_visited_folders = new Label(I18n.get_I18n_string("Visited_Folders", logger));
			hbox.getChildren().add(static_visited_folders);
			Look_and_feel_manager.set_region_look(static_visited_folders);

			hbox.getChildren().add(horizontal_spacer());

			visited_folders = new Label();
			hbox.getChildren().add(visited_folders);
			Look_and_feel_manager.set_region_look(visited_folders);

			the_main_pane.getChildren().add(hbox);

		}
		{
			HBox hbox = new HBox();

			Label static_visited_files = new Label(I18n.get_I18n_string("Visited_Files", logger));
			hbox.getChildren().add(static_visited_files);
			Look_and_feel_manager.set_region_look(static_visited_files);

			hbox.getChildren().add(horizontal_spacer());

			visited_files = new Label();
			hbox.getChildren().add(visited_files);
			Look_and_feel_manager.set_region_look(visited_files);

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
			Label target_folder_label = new Label(target_path.toAbsolutePath().toString());
			settings_vbox.getChildren().add(target_folder_label);
			Button up = new Button(I18n.get_I18n_string("Search_Parent_Folder", logger));
			Look_and_feel_manager.set_button_look(up,true);

			settings_vbox.getChildren().add(up);

			up.setOnAction(new EventHandler<>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					session.stop_search();
					Path parent = target_path.getParent();
					if (parent != null)
					{
						target_path = parent;
						target_folder_label.setText(target_path.toAbsolutePath().toString());
						start_search();
					}
				}
			});
		}
		{
			CheckBox search_also_folders = new CheckBox(I18n.get_I18n_string("Search_Also_Folders", logger));
			search_also_folders.setSelected(also_folders);
			Look_and_feel_manager.set_CheckBox_look(search_also_folders);
			search_also_folders.selectedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old_value, Boolean new_value) {
					also_folders = new_value;
					logger.log("search_also_folders = "+also_folders);
				}
			});
			settings_vbox.getChildren().add(search_also_folders);
		}
		settings_vbox.getChildren().add(vertical_spacer());
		{
			CheckBox only_images = new CheckBox(I18n.get_I18n_string("Search_Only_Images", logger));
			only_images.setSelected(look_only_for_images);
			Look_and_feel_manager.set_CheckBox_look(only_images);
			only_images.selectedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old_value, Boolean new_value) {
					look_only_for_images = new_value;
				}
			});
			settings_vbox.getChildren().add(only_images);
		}
		settings_vbox.getChildren().add(vertical_spacer());

		{
			CheckBox check_case_cb = new CheckBox(I18n.get_I18n_string("Check_Case", logger));
			check_case_cb.setSelected(check_case);
			Look_and_feel_manager.set_CheckBox_look(check_case_cb);
			check_case_cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old_value, Boolean new_value) {
					check_case = new_value;
				}
			});
			settings_vbox.getChildren().add(check_case_cb);
		}
		settings_vbox.getChildren().add(vertical_spacer());

		{
			HBox hb = new HBox();
			CheckBox use_extension_cb = new CheckBox(I18n.get_I18n_string("Use_Extension", logger));
			use_extension_cb.setSelected(use_extension);
			Look_and_feel_manager.set_CheckBox_look(use_extension_cb);
			use_extension_cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old_value, Boolean new_value) {
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
				}
			});
			hb.getChildren().add(use_extension_cb);
			extension_tf = new TextField("");
			extension_tf.setMaxWidth(100);
			extension_tf.setOnAction(actionEvent -> {
				extension_textfield_is_red = false;
				extension_tf.setStyle("-fx-text-inner-color: blue;");
				use_extension = true;
				use_extension_cb.setSelected(true);
				session.stop_search();
				add_keyword_slot(extension_tf.getText().trim(), true);
				start_search();
			});

			extension_tf.textProperty().addListener((observableValue, old_val, new_val) -> {
				if ( !extension_textfield_is_red)
				{
					extension_tf.setStyle("-fx-text-inner-color: red;");
					extension_textfield_is_red = true;
				}
				logger.log("extension_tf  old_val:"+old_val+" new_val:"+new_val);
			});

			Look_and_feel_manager.set_TextField_look(extension_tf);
			hb.getChildren().add(extension_tf);
			hb.getChildren().add(horizontal_spacer());
			hb.getChildren().add(horizontal_spacer());
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
			new_keyword_textfield.setStyle("-fx-text-inner-color: blue;");
			new_keyword_textfield.textProperty().addListener((observableValue, old_val, new_val) -> {
				if ( !new_keyword_textfield_is_red)
				{
					new_keyword_textfield.setStyle("-fx-text-inner-color: red;");
					new_keyword_textfield_is_red = true;
				}
				logger.log("new_keyword_textfield  old_val:"+old_val+" new_val:"+new_val);
			});

			new_keyword_textfield.setMinWidth(300);
			Look_and_feel_manager.set_TextField_look(new_keyword_textfield);
			new_keyword_textfield.setStyle("-fx-text-inner-color: darkgrey;");
			new_keyword_textfield.setOnAction(actionEvent ->
					{
						new_keyword_action(new_keyword_textfield);
					});
			hbox.getChildren().add(new_keyword_textfield);
			hbox.getChildren().add(horizontal_spacer());

			Button add_keyword = new Button(I18n.get_I18n_string("Add_Keyword", logger));
			Look_and_feel_manager.set_button_look(add_keyword,true);
			add_keyword.setOnAction(actionEvent -> {
				new_keyword_action(new_keyword_textfield);
			});
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

		start = new Button(I18n.get_I18n_string("Start_Search", logger));
		start.setOnAction(new EventHandler<>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				start_search();
			}
		});
		settings_vbox.getChildren().add(start);
		Look_and_feel_manager.set_button_look(start,true);

		settings_vbox.getChildren().add(vertical_spacer());
		stop = new Button(I18n.get_I18n_string("Stop_Search", logger));
		stop.setDisable(true);
		stop.setOnAction(new EventHandler<>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				session.stop_search();
				stop.setDisable(true);
			}
		});
		settings_vbox.getChildren().add(stop);
		Look_and_feel_manager.set_button_look(stop,true);
		settings_vbox.getChildren().add(vertical_spacer());


		return settings_vbox;
	}


	//**********************************************************
	private void new_keyword_action(TextField new_keyword_textfield)
	//**********************************************************
	{
		introduce_new_keyword(new_keyword_textfield.getText().trim());
		new_keyword_textfield.setText(BASE_);
		new_keyword_textfield.setStyle("-fx-text-inner-color: blue;");
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
	public static Node horizontal_spacer()
	//**********************************************************
	{
		final Region spacer = new Region();
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
		return spacer;
	}




	//**********************************************************
	@Override // Search_receiver
	public void receive_intermediary_statistics(Search_statistics search_statistics)
	//**********************************************************
	{
		Fx_batch_injector.inject(() -> {

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
		logger.log("has_ended() "+search_status);
		if ( search_status != Search_status.interrupted)
		{
			long now = System.currentTimeMillis();
			if (now - start_time > 3000) {
				if (Static_application_properties.get_ding(logger)) {
					Ding.play("File finder took more than 3 seconds", logger);
				}
			}
		}
		if ( search_status == Search_status.invalid)
		{
			Fx_batch_injector.inject(() -> {
                stop.setDisable(true);
                start.setDisable(false);
            },logger);
			return;
		}
		Fx_batch_injector.inject(() -> {
            stop.setDisable(true);
            start.setDisable(false);
        },logger);

	}

	//**********************************************************
	private static final Comparator<? super String> string_length_comparator = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			return (Integer.valueOf(o2.length())).compareTo(Integer.valueOf(o1.length()));
		}
	};


	//**********************************************************
	void start_search()
	//**********************************************************
	{
		List<String> keywords = new ArrayList<>(keyword_to_slot.keySet());

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
		Search_config search_config = new Search_config(target_path,keywords,look_only_for_images,local_extension,also_folders,check_case);
		session = new Search_session(search_config,browser,this,logger);
		session.start_search();
		stop.setDisable(false);
		start.setDisable(true);
		start_time = System.currentTimeMillis();
	}

	public Map<String, Keyword_slot> get_keyword_to_slot() {
		return keyword_to_slot;
	}
}
