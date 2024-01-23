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
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import klik.browser.Browser;
import klik.browser.System_open_actor;
import klik.files_and_paths.Ding;
import klik.files_and_paths.Guess_file_type;
import klik.images.Image_window;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.I18n;
import klik.level2.experimental.music.Audio_player;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class Finder_frame implements Search_receiver
//**********************************************************
{
	private final ProgressBar progress_bar;
	private final Button start;
	private final Button stop;
	Label visited_folders;
	Label visited_files;
	Logger logger;

	private final Map<String, Label> keyword_to_Label =  new HashMap<>();
	private final Stage stage;
	private final VBox top_vbox;
	private boolean look_only_for_images = false;
	private boolean use_extension = false;
	private boolean also_folders = true;
	private boolean check_case = false;
	Search_session session;
	Path target_path;

	Browser browser;
	VBox result_vbox;
	long start_time;
	TextField extension_tf;

	//**********************************************************
	public Finder_frame(Path target_path, List<String> input_keywords, double w, double h, Browser browser, Logger logger_)
	//**********************************************************
	{
		this.target_path = target_path;
		this.browser = browser;
		logger = logger_;
		top_vbox = new VBox();
		stage = new Stage();


		top_vbox.setAlignment(Pos.BASELINE_LEFT);

		/*
		the logic is:
		always look for file and maybe also folders
		only images means that we first check that the file is an image
		extension is then checked
		 */

		{
			CheckBox search_also_folders = new CheckBox(I18n.get_I18n_string("Search_Also_Folders", logger));
			search_also_folders.setSelected(also_folders);
			Look_and_feel_manager.set_region_look(search_also_folders);
			search_also_folders.selectedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old_value, Boolean new_value) {
					also_folders = new_value;
				}
			});
			top_vbox.getChildren().add(search_also_folders);
		}
		top_vbox.getChildren().add(vertical_spacer());
		{
			CheckBox only_images = new CheckBox(I18n.get_I18n_string("Search_Only_Images", logger));
			only_images.setSelected(look_only_for_images);
			Look_and_feel_manager.set_region_look(only_images);
			only_images.selectedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old_value, Boolean new_value) {
					look_only_for_images = new_value;
				}
			});
			top_vbox.getChildren().add(only_images);
		}
		top_vbox.getChildren().add(vertical_spacer());
		{
			HBox hb = new HBox();
			CheckBox use_extension_cb = new CheckBox(I18n.get_I18n_string("Use_Extension", logger));
			use_extension_cb.setSelected(use_extension);
			Look_and_feel_manager.set_region_look(use_extension_cb);
			use_extension_cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old_value, Boolean new_value) {
					use_extension = new_value;
				}
			});
			hb.getChildren().add(use_extension_cb);
			extension_tf = new TextField("");
			hb.getChildren().add(extension_tf);
			hb.getChildren().add(horizontal_spacer());
			top_vbox.getChildren().add(hb);
		}
		top_vbox.getChildren().add(vertical_spacer());
		{
			CheckBox check_case_cb = new CheckBox(I18n.get_I18n_string("Check_Case", logger));
			check_case_cb.setSelected(check_case);
			Look_and_feel_manager.set_region_look(check_case_cb);
			check_case_cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old_value, Boolean new_value) {
					check_case = new_value;
				}
			});
			top_vbox.getChildren().add(check_case_cb);
		}

		top_vbox.getChildren().add(vertical_spacer());

		VBox keyword_vbox = new VBox();
		for(String keyword : input_keywords )
		{
			if ( keyword.trim().isBlank()) continue;
			add_keyword(keyword.trim(),keyword_vbox);
		}
		top_vbox.getChildren().add(keyword_vbox);
		{
			HBox hbox = new HBox();
			TextField new_keyword = new TextField("");
			hbox.getChildren().add(new_keyword);
			Button add_keyword = new Button(I18n.get_I18n_string("Add_Keyword", logger));
			Look_and_feel_manager.set_button_look(add_keyword,true);
			add_keyword.setOnAction(actionEvent -> {
				if (new_keyword.getText().trim().isBlank()) return;
				if (keyword_to_Label.containsKey(new_keyword.getText().trim())) return;
				session.stop_search();
				result_vbox.getChildren().clear();
				if ( !new_keyword.getText().trim().isEmpty()) add_keyword(new_keyword.getText().trim(), keyword_vbox);
				new_keyword.setText("");
			});
			hbox.getChildren().add(add_keyword);
			hbox.getChildren().add(horizontal_spacer());
			top_vbox.getChildren().add(hbox);
		}
		top_vbox.getChildren().add(vertical_spacer());


		start = new Button(I18n.get_I18n_string("Start_Search", logger));
		start.setOnAction(new EventHandler<>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				start_search();
			}
		});
		top_vbox.getChildren().add(start);
		Look_and_feel_manager.set_button_look(start,true);

		top_vbox.getChildren().add(vertical_spacer());
		stop = new Button(I18n.get_I18n_string("Stop_Search", logger));
		stop.setDisable(true);
		stop.setOnAction(new EventHandler<>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				session.stop_search();
				stop.setDisable(true);
			}
		});
		top_vbox.getChildren().add(stop);
		Look_and_feel_manager.set_button_look(stop,true);
		top_vbox.getChildren().add(vertical_spacer());

		{
			visited_folders = new Label(I18n.get_I18n_string("Visited_Folders", logger));
			top_vbox.getChildren().add(visited_folders);
			Look_and_feel_manager.set_region_look(visited_folders);
			visited_files = new Label(I18n.get_I18n_string("Visited_Files", logger));
			top_vbox.getChildren().add(visited_files);
			Look_and_feel_manager.set_region_look(visited_files);

		}
		top_vbox.getChildren().add(vertical_spacer());

		progress_bar = new ProgressBar();
		//Look_and_feel_manager.set_region_look(progress_bar);
		progress_bar.prefWidthProperty().bind(stage.widthProperty().subtract(20));
		progress_bar.setMinHeight(20);
		top_vbox.getChildren().add(progress_bar);
		top_vbox.getChildren().add(vertical_spacer());

		/*
		//Text max_keywords = new Text("");
		//top_vbox.getChildren().add(max_keywords);

		reason_to_stop = new Label("");
		reason_to_stop.setVisible(false);
		top_vbox.getChildren().add(reason_to_stop);
		Look_and_feel_manager.set_region_look(reason_to_stop);
		*/


		result_vbox = new VBox();
		result_vbox.setAlignment(Pos.BASELINE_LEFT);
		ScrollPane scrollpane = new ScrollPane(result_vbox);
		//Look_and_feel_manager.set_region_look(scrollpane);
		Look_and_feel_manager.set_region_look(result_vbox);
		scrollpane.setPrefSize(w,h);
		scrollpane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		scrollpane.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);

		top_vbox.getChildren().add(scrollpane);
		VBox.setVgrow(scrollpane,Priority.ALWAYS);

		//stage.setHeight(h);
		//stage.setWidth(w);

		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent windowEvent) {
				session.stop_search();
			}
		});

		Scene scene = new Scene(top_vbox, w, h);
		Look_and_feel_manager.set_region_look(top_vbox);


		stage.setTitle(I18n.get_I18n_string("Search_by_keywords", logger));
		stage.setScene(scene);
		stage.show();
	}


	//**********************************************************
	private void add_keyword(String local_keyword, VBox keyword_vbox)
	//**********************************************************
	{
		HBox local_hbox = new HBox();
		keyword_vbox.getChildren().add(local_hbox);

		Label t1 = new Label(I18n.get_I18n_string("Keyword", logger)+": ->"+ local_keyword+ "<- "+I18n.get_I18n_string("Was_Found_In", logger)+" : ");
		local_hbox.getChildren().add(t1);
		Look_and_feel_manager.set_region_look(t1);
		local_hbox.getChildren().add(horizontal_spacer());

		Label t2 = new Label("");
		Look_and_feel_manager.set_region_look(t2);
		keyword_to_Label.put(local_keyword,t2);
		local_hbox.getChildren().add(t2);
		local_hbox.getChildren().add(horizontal_spacer());

		Label t3 = new Label( I18n.get_I18n_string("File_Names",logger));
		Look_and_feel_manager.set_region_look(t3);
		local_hbox.getChildren().add(t3);
		local_hbox.getChildren().add(horizontal_spacer());

		Button t4 = new Button(I18n.get_I18n_string("Remove_This_Keyword",logger));
		Look_and_feel_manager.set_button_look(t4,true);
		t4.setOnAction(new EventHandler<>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				session.stop_search();
				keyword_vbox.getChildren().remove(local_hbox);
				keyword_to_Label.remove(local_keyword);
			}
		});
		local_hbox.getChildren().add(t4);
		local_hbox.getChildren().add(horizontal_spacer());

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
		spacer.setMinHeight(4);
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
				double p = progress_bar.getProgress();
				p += 1.0/100.0;
				if ( p > 1.0) p = 0.0;
				progress_bar.setProgress(p);

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
				visited_files.setText(I18n.get_I18n_string("Visited_Files", logger)+": "+st.visited_files());
				visited_folders.setText(I18n.get_I18n_string("Visited_Folders", logger)+": "+st.visited_folders());

			}
		});


	}
	//**********************************************************
	@Override // Search_receiver
	public void has_ended(Search_status search_status, String message)
	//**********************************************************
	{
		logger.log("has_ended() "+search_status+" "+message);
		long now = System.currentTimeMillis();
		if ( now-start_time> 3000)
		{
			if (Static_application_properties.get_ding(logger))
			{
				Ding.play(logger);
			}
		}
		if ( search_status == Search_status.invalid)
		{
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					progress_bar.setProgress(0);
					stop.setDisable(true);
					start.setDisable(false);
					result_vbox.getChildren().clear();
				}
			});
			return;
		}
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				progress_bar.setProgress(1);
				stop.setDisable(true);
				start.setDisable(false);
				show_search_results(browser,session.get_search_results(),"reason: ended");
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
	private void show_search_results(Browser the_browser, HashMap<String, List<Path>> search_results, String reason)
	//**********************************************************
	{
		logger.log("show_search_results "+search_results.size()+" items");

		result_vbox.getChildren().clear();
		List<String> keyset = new ArrayList<>(search_results.keySet());
		keyset.sort(string_length_comparator);
		for( String key : keyset)
		{
			Label label = new Label(I18n.get_I18n_string("Matched_Keywords", logger)+": "+key);
			Look_and_feel_manager.set_region_look(label);
			result_vbox.getChildren().add(label);
			List<Path> path_set = search_results.get(key);
			int count = 0;
			for ( Path path : path_set)
			{
				Button b = new Button(key+" => "+path);
				Look_and_feel_manager.set_button_look(b, true);
				result_vbox.getChildren().add(b);
				b.setOnAction(ee -> {
					logger.log("going to open on menu select: " + key);

					if (Guess_file_type.is_file_an_image(path.toFile())) {
						Image_window is = Image_window.get_Image_window(browser, path, logger);
					} else if (Guess_file_type.is_this_path_a_music(path)) {
						logger.log("opening audio file: " + path.toAbsolutePath());
						Audio_player.play_song(path.toFile(), logger);
					} else {
						System_open_actor.open_with_system(browser, path, logger);
					}
				});
				/*count++;
				if ( count > 20)
				{
					logger.log("not showing more than 20 results");
					break;
				}*/
			}
			if(key.equals(session.get_max_key()))
			{
				// no need to list partial matches...
				break;
			}
		}
		logger.log("show_search_results done");
	}


	void start_search()
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
