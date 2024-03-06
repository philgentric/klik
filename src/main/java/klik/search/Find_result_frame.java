package klik.search;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.util.execute.System_open_actor;
import klik.files_and_paths.Guess_file_type;
import klik.images.Image_window;
import klik.level3.experimental.music.Audio_player;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.I18n;
import klik.util.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

//**********************************************************
public class Find_result_frame
//**********************************************************
{
	Logger logger;
	VBox the_result_vbox = new VBox();
	HashMap<String, List<Path>> search_results;
	Stage stage = new Stage();
	ImageView iv;

	//**********************************************************
	public Find_result_frame(Browser the_browser, HashMap<String, List<Path>> search_results, Search_session session, Logger logger_)
	//**********************************************************
	{
		logger = logger_;

		VBox vbox = new VBox();
		Look_and_feel_manager.set_region_look(vbox);

		vbox.setAlignment(javafx.geometry.Pos.CENTER);
		iv = new ImageView(Look_and_feel_manager.get_search_icon());
		iv.setFitHeight(100);
		iv.setPreserveRatio(true);
		vbox.getChildren().add(iv);

		ScrollPane scroll_pane = new ScrollPane(the_result_vbox);
		vbox.getChildren().add(scroll_pane);
		Scene scene = new Scene(vbox, 1000, 800);
		//Scene scene = new Scene(scroll_pane, 800, 600);
		Look_and_feel_manager.set_region_look(scroll_pane);

		stage.setTitle(I18n.get_I18n_string("Search_Results", logger));
		stage.setScene(scene);
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
		//if ( search_results != null) show_search_results(the_browser, search_results, session);

		//scene.getRoot().setCursor(Cursor.WAIT);


	}
/*
	//**********************************************************
	@Deprecated // use inject_search_results
	private void show_search_results(Browser the_browser, HashMap<String, List<Path>> search_results_, Search_session session)
	//**********************************************************
	{
		List<String> keyset = new ArrayList<>(search_results.keySet());
		if ( keyset.isEmpty())
		{
			Label label = new Label(I18n.get_I18n_string("No_Match", logger));
			Look_and_feel_manager.set_region_look(label);
			the_result_vbox.getChildren().add(label);
			return;
		}
		keyset.sort(string_length_comparator);
		for( String key : keyset)
		{
			boolean is_max = key.equals(session.get_max_key());
			Label label = new Label(I18n.get_I18n_string("Matched_Keywords", logger)+": "+key);
			Look_and_feel_manager.set_region_look(label);
			the_result_vbox.getChildren().add(label);
			List<Path> path_set = search_results.get(key);
			int count = 0;
			for ( Path path : path_set)
			{
				make_one_button(the_browser, key, is_max, path);

			}
			if(key.equals(session.get_max_key()))
			{
				// no need to list partial matches...
				break;
			}
		}
		logger.log("show_search_results done");
	}
*/
	//**********************************************************
	private void make_one_button(Browser the_browser, String key, boolean is_max, Path path)
	//**********************************************************
	{

		Button b = new Button(key +" => "+ path);
		if(is_max)
		{
			b.setGraphic(new Circle(10, Color.RED));
		}
		b.setMnemonicParsing(false); // avoid removal of first underscore
		Look_and_feel_manager.set_button_look(b, true);
		the_result_vbox.getChildren().add(b);
		b.setOnAction(ee -> {
			logger.log("going to open on menu select: " + key);

			if (Files.isDirectory(path)) {
				Browser_creation_context.additional_different_folder(path, the_browser,logger);
			}
			else if (Guess_file_type.is_file_an_image(path.toFile())) {
				Image_window is = Image_window.get_Image_window(the_browser, path, logger);
			} else if (Guess_file_type.is_this_path_a_music(path)) {
				logger.log("opening audio file: " + path.toAbsolutePath());
				Audio_player.play_song(path.toFile(), logger);
			} else {
				System_open_actor.open_with_system(the_browser, path, logger);
			}
		});

		// add a menu to the button!
		ContextMenu context_menu = new ContextMenu();
		Look_and_feel_manager.set_context_menu_look(context_menu);

		MenuItem browse = new MenuItem("Browse in new window");
		browse.setOnAction(event -> {
			logger.log("Browse in new window!");
			Path local = path;
			if (! local.toFile().isDirectory()) local = local.getParent();
			Browser_creation_context.additional_different_folder(local,the_browser,logger);
		});

		context_menu.getItems().add(browse);

		b.setOnContextMenuRequested((ContextMenuEvent event) -> {
			logger.log("show context menu of button:"+ path.toAbsolutePath());
			context_menu.show(b, event.getScreenX(), event.getScreenY());
		});	}


	//**********************************************************
	private static final Comparator<? super String> string_length_comparator = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			return (Integer.valueOf(o2.length())).compareTo(Integer.valueOf(o1.length()));
		}
	};


	//**********************************************************
	public void inject_search_results(Search_result sr, String keys, boolean is_max, Browser the_browser)
	//**********************************************************
	{
		if ( search_results == null) search_results = new HashMap<>();
		List<Path> path_set = search_results.get(keys);
		if ( path_set == null)
		{
			path_set = new ArrayList<>();
			search_results.put(keys,path_set);
		}

		path_set.add(sr.path());

		Platform.runLater(() ->
		{
			make_one_button(the_browser, keys, is_max, sr.path());
			//stage.getScene().getRoot().setCursor(Cursor.WAIT);
		} );

	}

	public void has_ended() {

		Platform.runLater(() -> {
			stage.setTitle(I18n.get_I18n_string("Search_Results_Ended", logger));
			//stage.getScene().getRoot().setCursor(Cursor.DEFAULT);
			iv.setImage(Look_and_feel_manager.get_search_end_icon());

		});

	}
}
