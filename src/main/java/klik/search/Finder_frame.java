package klik.search;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
import klik.util.Logger;

import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class Finder_frame implements Search_receiver
//**********************************************************
{
	private Button start;
	private Button stop;
	Label visited_folders;
	Label visited_files;
	Logger logger;

	private final Map<String, Label> keyword_to_Label =  new HashMap<>(); // this is the textfield to report the number of matches
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

	//**********************************************************
	public Finder_frame(Path target_path, List<String> input_keywords, Browser browser, Logger logger_)
	//**********************************************************
	{
		this.target_path = target_path;
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
		stage.setMinWidth(800);
		stage.setScene(scene);
		stage.show();
	}

	//**********************************************************
	private Pane define_main_vbox(List<String> input_keywords)
	//**********************************************************
	{
		VBox the_main_pane = new VBox();
		Pane settings = define_settings_pane(input_keywords);
		the_main_pane.getChildren().add(settings);

		{
			visited_folders = new Label(I18n.get_I18n_string("Visited_Folders", logger));
			the_main_pane.getChildren().add(visited_folders);
			Look_and_feel_manager.set_region_look(visited_folders);
			visited_files = new Label(I18n.get_I18n_string("Visited_Files", logger));
			the_main_pane.getChildren().add(visited_files);
			Look_and_feel_manager.set_region_look(visited_files);
		}

		return the_main_pane;
	}

	//**********************************************************
	private VBox define_results_vbox(List<String> inputKeywords)
	//**********************************************************
	{
		VBox local_result_vbox = new VBox();
		local_result_vbox.setAlignment(Pos.BASELINE_LEFT);



		return local_result_vbox;
	}

	//**********************************************************
	private Pane define_settings_pane(List<String> input_keywords)
	//**********************************************************
	{
		VBox settings_vbox = new VBox();
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
				}
			});
			hb.getChildren().add(use_extension_cb);
			extension_tf = new TextField("");
			extension_tf.setMaxWidth(100);

			Look_and_feel_manager.set_TextField_look(extension_tf);
			hb.getChildren().add(extension_tf);
			hb.getChildren().add(horizontal_spacer());
			hb.getChildren().add(horizontal_spacer());
			settings_vbox.getChildren().add(hb);
		}
		settings_vbox.getChildren().add(vertical_spacer());


		VBox top_keyword_vbox = new VBox();
		settings_vbox.getChildren().add(top_keyword_vbox);

		VBox bottom_keyword_vbox = new VBox();
		settings_vbox.getChildren().add(bottom_keyword_vbox);


		{
			HBox hbox = new HBox();
			TextField new_keyword = new TextField("<enter new keyword here>");
			new_keyword.setMinWidth(300);
			Look_and_feel_manager.set_TextField_look(new_keyword);
			hbox.getChildren().add(new_keyword);
			hbox.getChildren().add(horizontal_spacer());

			Button add_keyword = new Button(I18n.get_I18n_string("Add_Keyword", logger));
			Look_and_feel_manager.set_button_look(add_keyword,true);
			add_keyword.setOnAction(actionEvent -> {
				if (new_keyword.getText().trim().isBlank()) return;
				if (keyword_to_Label.containsKey(new_keyword.getText().trim())) return;
				session.stop_search();
				//the_result_vbox.getChildren().clear();
				if ( !new_keyword.getText().trim().isEmpty()) add_keyword(new_keyword.getText().trim(), top_keyword_vbox, bottom_keyword_vbox);
				new_keyword.setText("");
			});
			hbox.getChildren().add(add_keyword);
			top_keyword_vbox.getChildren().add(hbox);
		}
		top_keyword_vbox.getChildren().add(vertical_spacer());
		for(String keyword : input_keywords )
		{
			if ( keyword.trim().isBlank()) continue;
			add_keyword(keyword.trim(),top_keyword_vbox,bottom_keyword_vbox);
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
	private void add_keyword(String local_keyword, VBox top_keyword_vbox, VBox bottom_keyword_vbox)
	//**********************************************************
	{
		HBox local_hbox2 = new HBox();

		{
			// first part is the keyword line showing the "delete" button, in the top panel
			HBox local_hbox1 = new HBox();
			top_keyword_vbox.getChildren().add(local_hbox1);
			TextField l1 = new TextField(I18n.get_I18n_string("Keyword", logger));
			local_hbox1.getChildren().add(l1);
			Look_and_feel_manager.set_region_look(l1);
			local_hbox1.getChildren().add(horizontal_spacer());

			TextField t1 = new TextField("->"+local_keyword+"<-");
			local_hbox1.getChildren().add(t1);
			Look_and_feel_manager.set_region_look(t1);
			local_hbox1.getChildren().add(horizontal_spacer());

			Button t4 = new Button(I18n.get_I18n_string("Remove_This_Keyword", logger));
			Look_and_feel_manager.set_button_look(t4, true);
			t4.setOnAction(new EventHandler<>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					session.stop_search();
					top_keyword_vbox.getChildren().remove(local_hbox1);
					bottom_keyword_vbox.getChildren().remove(local_hbox2);
					keyword_to_Label.remove(local_keyword);
				}
			});
			local_hbox1.getChildren().add(t4);
		}

		// second part is in the result section
		{
			Label t1 = new Label("->"+local_keyword+"<- ");
			Look_and_feel_manager.set_region_look(t1);
			local_hbox2.getChildren().add(t1);
			local_hbox2.getChildren().add(horizontal_spacer());

			Label t2 = new Label(I18n.get_I18n_string("Was_Found_In", logger));
			Look_and_feel_manager.set_region_look(t2);
			local_hbox2.getChildren().add(t2);
			local_hbox2.getChildren().add(horizontal_spacer());

			Label t3 = new Label(""); // this is the label that will be updated during search with the match count
			Look_and_feel_manager.set_region_look(t3);
			keyword_to_Label.put(local_keyword,t3);
			local_hbox2.getChildren().add(t3);
			local_hbox2.getChildren().add(horizontal_spacer());

			Label t4 = new Label( I18n.get_I18n_string("File_Names",logger));
			Look_and_feel_manager.set_region_look(t4);
			local_hbox2.getChildren().add(t4);
			bottom_keyword_vbox.getChildren().add(local_hbox2);
		}
	}

	//**********************************************************
	private Node horizontal_spacer()
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
	public void receive_intermediary(Search_statistics st)
	//**********************************************************
	{
		Platform.runLater(new Runnable() {
			@Override
			public void run() {

				for (String input_keyword: keyword_to_Label.keySet())
				{
					Label t = keyword_to_Label.get(input_keyword);
					if (t == null) {
						System.out.println("SHOULD NOT HAPPEN: no Text component in UI for keyword ->" + input_keyword + "<-");
					} else {
						if (st.matched_keyword_counts().get(input_keyword) == null )
						{
							t.setText(String.valueOf( 0));
						}
						else {
							t.setText(String.valueOf( st.matched_keyword_counts().get(input_keyword)));
						}
					}
				}
				visited_files.setText(I18n.get_I18n_string("Visited_Files", logger)+": \t\t"+st.visited_files());
				visited_folders.setText(I18n.get_I18n_string("Visited_Folders", logger)+": \t\t"+st.visited_folders());

			}
		});


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
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					stop.setDisable(true);
					start.setDisable(false);

				}
			});
			return;
		}
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				stop.setDisable(true);
				start.setDisable(false);
				Find_result_frame find_result_frame = new Find_result_frame(browser,session.get_search_results(),session,logger);
			}
		});

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
		List<String> keywords = new ArrayList<>(keyword_to_Label.keySet());
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
}
