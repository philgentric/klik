package klik.search;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.browser.System_open_actor;
import klik.files_and_paths.Guess_file_type;
import klik.images.Image_window;
import klik.level3.experimental.music.Audio_player;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.I18n;
import klik.util.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class Find_result_frame
//**********************************************************
{
	Logger logger;
	VBox the_result_vbox = new VBox();
	//**********************************************************
	public Find_result_frame(Browser the_browser, HashMap<String, List<Path>> search_results, Search_session session, Logger logger_)
	//**********************************************************
	{
		logger = logger_;
		Stage stage = new Stage();
		ScrollPane scroll_pane = new ScrollPane(the_result_vbox);

		Scene scene = new Scene(scroll_pane,800,600);
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
			Label label = new Label(I18n.get_I18n_string("Matched_Keywords", logger)+": "+key);
			Look_and_feel_manager.set_region_look(label);
			the_result_vbox.getChildren().add(label);
			List<Path> path_set = search_results.get(key);
			int count = 0;
			for ( Path path : path_set)
			{
				Button b = new Button(key+" => "+path);
				b.setMnemonicParsing(false); // avoid removal of first underscore
				Look_and_feel_manager.set_button_look(b, true);
				the_result_vbox.getChildren().add(b);
				b.setOnAction(ee -> {
					logger.log("going to open on menu select: " + key);

					if (Files.isDirectory(path)) {
						Browser_creation_context.additional_different_folder(path,the_browser,logger);
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



	//**********************************************************
	private static final Comparator<? super String> string_length_comparator = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			return (Integer.valueOf(o2.length())).compareTo(Integer.valueOf(o1.length()));
		}
	};


}
