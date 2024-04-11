package klik.search;

import javafx.application.Platform;
import javafx.scene.Node;
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
import klik.browser.Drag_and_drop;
import klik.look.Font_size;
import klik.util.Fx_batch_injector;
import klik.util.Text_frame;
import klik.util.execute.System_open_actor;
import klik.files_and_paths.Guess_file_type;
import klik.images.Image_window;
import klik.audio.Audio_player;
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
public class Results_frame
//**********************************************************
{
	Logger logger;
	VBox the_result_vbox = new VBox();
	HashMap<String, List<Path>> search_results;
	Stage stage = new Stage();
	ImageView iv;

	//**********************************************************
	public Results_frame(Browser the_browser, HashMap<String, List<Path>> search_results, Search_session session, Logger logger_)
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
			//logger.log("going to open on menu select: " + key);

			if (Files.isDirectory(path)) {
				Browser_creation_context.additional_different_folder(path, the_browser,logger);
			}
			else if (Guess_file_type.is_file_an_image(path.toFile())) {
				Image_window is = Image_window.get_Image_window(the_browser, path, logger);
			} else if (Guess_file_type.is_this_path_a_music(path)) {
				logger.log("opening audio file: " + path.toAbsolutePath());
				Audio_player.play_song(path.toFile(), logger);
			} else if (Guess_file_type.is_this_path_a_text(path)) {
				logger.log("opening text file: " + path.toAbsolutePath());
				Text_frame.show(path, logger);
			} else {
				System_open_actor.open_with_system(the_browser, path, logger);
			}
		});

		// add a menu to the button!
		ContextMenu context_menu = new ContextMenu();
		Look_and_feel_manager.set_context_menu_look(context_menu);


		MenuItem browse = new MenuItem( I18n.get_I18n_string("Browse",logger));
		browse.setOnAction(event -> {
			logger.log("Browse in new window");
			Path local = path;
			if (! local.toFile().isDirectory()) local = local.getParent();
			Browser_creation_context.additional_different_folder(local,the_browser,logger);
		});
		context_menu.getItems().add(browse);

		if (! path.toFile().isDirectory())
		{
			String text = I18n.get_I18n_string("Open_With_Registered_Application",logger);
			MenuItem open_special = new MenuItem(text);
			open_special.setOnAction(event -> {
				logger.log("Open_With_Registered_Application");
				System_open_actor.open_special(the_browser.my_Stage.the_Stage,path,the_browser.aborter,logger);
			});
			context_menu.getItems().add(open_special);

		}

		b.setOnContextMenuRequested((ContextMenuEvent event) -> {
			logger.log("show context menu of button:"+ path.toAbsolutePath());
			context_menu.show(b, event.getScreenX(), event.getScreenY());
		});


		Drag_and_drop.init_drag_and_drop_sender_side(b,the_browser,path,logger);


	}


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

		Fx_batch_injector.inject(() -> make_one_button(the_browser, keys, is_max, sr.path()),logger);

	}

	//**********************************************************
	public void has_ended()
	//**********************************************************
	{

		Fx_batch_injector.inject(() -> {
			stage.setTitle(I18n.get_I18n_string("Search_Results_Ended", logger));
			//stage.getScene().getRoot().setCursor(Cursor.DEFAULT);
			iv.setImage(Look_and_feel_manager.get_search_end_icon());

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

		},logger);


	}

	public void sort()
	{
	}
}
