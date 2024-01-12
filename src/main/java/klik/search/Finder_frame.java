package klik.search;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.actor.Job;
import klik.actor.Job_termination_reporter;
import klik.browser.Browser;
import klik.browser.System_open_actor;
import klik.browser.System_open_message;
import klik.files_and_paths.Guess_file_type;
import klik.images.Image_window;
import klik.look.my_i18n.I18n;
import klik.music.Audio_player;
import klik.util.Logger;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

//import javafx.scene.web.WebView;

enum Status
{
	undefined,
	ready,
	searching,
	stopping
}

//**********************************************************
public class Finder_frame implements Callback_for_image_found_publish, Job_termination_reporter
//**********************************************************
{
	private static final int MAX_MENU_ITEMS = 23;
	HashMap<String, List<String>> search_results = new HashMap<>();
	private final Label reason_to_stop;
	private final ProgressBar progress_bar;
	private final Button result_button;
	private ContextMenu contextMenu;
	private final Button toggle;
	Text visited_folders;
	Text visited_files;
	Logger logger;
	Status status = Status.undefined;

	//private final double W;
	private final Stage stage;
	private final VBox vbox;

	private final Map<String, Text> keyword_to_text =  new HashMap<>();
	private Job the_job;
	private boolean look_only_for_images = false;

	private final Aborter aborter = new Aborter();

	//**********************************************************
	public Finder_frame(Path target_path, List<String> keywords, double w, double h, Browser browser, Finder_actor finder_actor, Logger logger_)
	//**********************************************************
	{
		logger = logger_;
		//W = w;

		vbox = new VBox();
		{
			visited_folders = new Text(I18n.get_I18n_string("Visited_Folders", logger));
			vbox.getChildren().add(visited_folders);
			visited_files = new Text(I18n.get_I18n_string("Visited_Files", logger));
			vbox.getChildren().add(visited_files);
		}
		{
			CheckBox only_images = new CheckBox(I18n.get_I18n_string("Search_Only_Images", logger));
			only_images.setSelected(look_only_for_images);
			only_images.selectedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old_value, Boolean new_value) {
					look_only_for_images = new_value;
				}
			});
			vbox.getChildren().add(only_images);
		}
		toggle = new Button(I18n.get_I18n_string("Stop_Search", logger));
		toggle.setOnAction(new EventHandler<>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				if ( status != Status.ready)
				{
					stop_search();
				}
				else
				{
					search_results.clear();
					start_search(target_path, browser, finder_actor);
				}
			}
		});
		vbox.getChildren().add(toggle);

		VBox keyword_vbox = new VBox();
		for(String s : keywords )
		{
			add_keyword(s,keyword_vbox);
		}
		vbox.getChildren().add(keyword_vbox);
		{
			HBox hbox = new HBox();
			TextField new_keyword = new TextField("");
			hbox.getChildren().add(new_keyword);
			hbox.getChildren().add(create_spacer());
			Button add_keyword = new Button(I18n.get_I18n_string("Add_Keyword", logger));
			add_keyword.setOnAction(actionEvent -> {
				stop_search();
				if ( !new_keyword.getText().isEmpty()) add_keyword(new_keyword.getText(), keyword_vbox);
			});
			hbox.getChildren().add(add_keyword);
			vbox.getChildren().add(hbox);
		}

		progress_bar = new ProgressBar();
		progress_bar.setPrefWidth(w-10);
		vbox.getChildren().add(progress_bar);
		ScrollPane sp = new ScrollPane();
		sp.setPrefSize(w,h);
		sp.setContent(vbox);
		sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		sp.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);

		result_button = new Button("Show search results popup menu");
		result_button.setOnAction(new EventHandler<>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				contextMenu.show(result_button, Side.TOP, result_button.getLayoutX(), result_button.getLayoutY());
			}
		});
		result_button.setVisible(false);
		vbox.getChildren().add(result_button);
		reason_to_stop = new Label("");
		reason_to_stop.setVisible(false);
		vbox.getChildren().add(reason_to_stop);
		stage = new Stage();
		stage.setHeight(h);
		stage.setWidth(w);

		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent windowEvent) {
				aborter.abort();
			}
		});

		Scene scene = new Scene(sp, w, h, Color.WHITE);


		stage.setTitle(I18n.get_I18n_string("Search_by_keywords", logger));
		stage.setScene(scene);
		stage.show();
		status = Status.ready;
	}

	//**********************************************************
	void start_search(Path target_path, Browser browser, Finder_actor finder_actor)
	//**********************************************************
	{
		status = Status.searching;
		result_button.setVisible(false);
		reason_to_stop.setVisible(false);
		List<String> keywords = new ArrayList<>(keyword_to_text.keySet());
		the_job = Actor_engine.run(finder_actor,new Finder_message(target_path, keywords, look_only_for_images, this,aborter,browser),this,logger);
		toggle.setText(I18n.get_I18n_string("Stop_Search", logger));
	}

	//**********************************************************
	private void stop_search()
	//**********************************************************
	{
		aborter.abort();
		status = Status.stopping;
		logger.log("stop_search()");
		toggle.setDisable(true);
		ConcurrentLinkedQueue<Job> ll = new ConcurrentLinkedQueue<>();
		if ( the_job != null) ll.add(the_job);
		Actor_engine.get(logger).cancel_all(ll);
	}


	//**********************************************************
	private void add_keyword(String keyword, VBox keyword_vbox)
	//**********************************************************
	{
		HBox hBox = new HBox();
		keyword_vbox.getChildren().add(hBox);

		Text t1 = new Text(I18n.get_I18n_string("Keyword", logger)+": ->"+ keyword+ "<- "+I18n.get_I18n_string("Was_Found_In", logger)+" : ");
		hBox.getChildren().add(t1);
		hBox.getChildren().add(create_spacer());

		Text t2 = new Text("");
		keyword_to_text.put(keyword,t2);
		hBox.getChildren().add(t2);
		hBox.getChildren().add(create_spacer());

		Text t3 = new Text( I18n.get_I18n_string("File_Names",logger));
		hBox.getChildren().add(t3);
		hBox.getChildren().add(create_spacer());

		Button t4 = new Button(I18n.get_I18n_string("Remove_This_Keyword",logger));
		t4.setOnAction(new EventHandler<>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				stop_search();
				keyword_vbox.getChildren().remove(hBox);
				keyword_to_text.remove(keyword);
				search_results.remove(keyword);
			}
		});
		hBox.getChildren().add(t4);
	}

	//**********************************************************
	private Node create_spacer()
	//**********************************************************
	{
		final Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		return spacer;
	}



	//**********************************************************
	public void ping(Search_result sr, Search_statistics st, int occurence_count)
	//**********************************************************
	{
		Platform.runLater(() ->
			{
				visited_files.setText(I18n.get_I18n_string("Visited_Files", logger)+": "+st.visited_files);
				visited_folders.setText(I18n.get_I18n_string("Visited_Folders", logger)+": "+st.visited_folders);
				double p = progress_bar.getProgress();
				p += 1.0/100.0;
				if ( p > 1.0) p = 0.0;
				progress_bar.setProgress(p);
				if ( sr.keyword == null) return;
				Text t = keyword_to_text.get(sr.keyword);
				if ( t == null)
				{
					System.out.println("warning: no text for keyword ->"+sr.keyword+"<-");
				}
				else
				{
					t.setText(String.valueOf(occurence_count));
				}
			}
		);

	}



	//**********************************************************
	public void set_result(ContextMenu contextMenu_, String reason)
	//**********************************************************
	{
		contextMenu = contextMenu_;
		result_button.setVisible(true);
		reason_to_stop.setText(reason);
		reason_to_stop.setVisible(true);
	}


	//**********************************************************
	@Override
	public void add_one_Search_result(Search_result sr, Search_statistics st)
	//**********************************************************
	{
		if (( sr.keyword == null) || (sr.full_path == null))
		{
			ping(sr, st, 0);
			return;
		}
		List<String> list =search_results.computeIfAbsent(sr.keyword,x -> new ArrayList<>());
		if ( !list.contains(sr.full_path))  list.add(sr.full_path);
		ping(sr,st,list.size());

	}

	//**********************************************************
	@Override
	public void update_display_in_FX_thread(Browser b, String reason_to_stop)
	//**********************************************************
	{
		Platform.runLater(() -> show_similars(b, search_results,reason_to_stop));
	}

	//**********************************************************
	@Override
	public void has_ended()
	//**********************************************************
	{
		// dangerous usage...
		has_ended("callback end", null);
	}

	//**********************************************************
	@Override
	public void has_ended(String message, Job job)
	//**********************************************************
	{
		System.out.println("has_ended() called: "+ message);
		Platform.runLater(() ->{
				System.out.println("search ended: "+message);
				ready();
			});
	}

	private void ready()
	{
		toggle.setDisable(false);
		toggle.setText("Restart search");
		status = Status.ready;
	}



	int displayed_items_sequence_number = 0;

	//**********************************************************
	private void show_similars(Browser the_browser, HashMap<String, List<String>> similars, String reason)
	//**********************************************************
	{
		final ContextMenu contextMenu = new ContextMenu();
		for( Map.Entry<String, List<String>> e : similars.entrySet())
		{
			String name = e.getKey();
			List<String> path_set = e.getValue();
			String found = "keyword:" +name+", "+path_set.size()+" items";
			displayed_items_sequence_number = 0;
			Menu one_item = create_one_menu_item_for_keyword(the_browser, found, path_set);
			contextMenu.getItems().add(one_item);
			if ( Finder_actor.dbg) logger.log("items before: "+path_set.size()+" files,  displayed="+ displayed_items_sequence_number);
		}
		//contextMenu.show(popup.stage, popup.stage.getX()+10, popup.stage.getY()+10);
		set_result(contextMenu, reason);
	}


	//**********************************************************
	private Menu create_one_menu_item_for_keyword(Browser b, String found, List<String> path_set)
	//**********************************************************
	{
		Menu one_item = new Menu(found);
		Iterator<String> it = path_set.iterator();
		int remaining = path_set.size();
		if ( path_set.size() <= MAX_MENU_ITEMS)
		{
			remaining -= create_menu_items_for(b, one_item, it);
			return one_item;
		}
		if ( path_set.size() > MAX_MENU_ITEMS*MAX_MENU_ITEMS)
		{
			int max_level = 1;
			int possible = MAX_MENU_ITEMS;
			for(;max_level < 12; max_level++)
			{
				possible *= MAX_MENU_ITEMS;
				if ( possible >= remaining) break;
				max_level++;
			}
			max_level--;
			if ( Finder_actor.dbg) logger.log("possible="+possible);
			if ( Finder_actor.dbg) logger.log("max_level="+max_level);
			remaining -= create_menu_items_recursive(b, one_item, it, remaining, max_level);
			return one_item;
		}
		for ( int k = 0; k< MAX_MENU_ITEMS; k++)
		{
			Menu sub_menu = new Menu("part"+k);
			remaining -= create_menu_items_for(b,sub_menu, it);
			one_item.getItems().add(sub_menu );
			if ( remaining <= 0) break;
		}
		return one_item;
	}

	//**********************************************************
	private int create_menu_items_recursive(Browser the_browser,
											Menu parent,
											Iterator<String> path_iterator,
											int remaining_,
											int max_level)
	//**********************************************************
	{
		int local_remaining = remaining_;
		if (local_remaining <= 0) return 0;

		max_level--;
		if ( max_level == 0)
		{
			//logger.log("NO recurse because max_level = 0, remaining="+local_remaining);
			int done = create_menu_items_for(the_browser, parent, path_iterator);
			return done;
		}

		if ( local_remaining <=  MAX_MENU_ITEMS)
		{
			//logger.log("NO recurse, remaining ="+local_remaining);
			int done = create_menu_items_for(the_browser, parent, path_iterator);
			return done;
		}
		if ( Finder_actor.dbg) logger.log("recurse at level "+max_level+" for remaining = "+local_remaining);
		int done = 0;
		for ( int i = 0 ; i < MAX_MENU_ITEMS ; i++)
		{
			//String text = "subpart "+i+" remaining:"+local_remaining;
			String text = String.valueOf(displayed_items_sequence_number);
			Menu one_sub_item = new Menu(text);
			int returned_count_of_new_menu_items = create_menu_items_recursive(the_browser, one_sub_item,path_iterator,local_remaining, max_level);
			if ( returned_count_of_new_menu_items == 0) break;
			local_remaining -= returned_count_of_new_menu_items;
			//text += " after="+local_remaining;
			text += "-"+ displayed_items_sequence_number;
			one_sub_item.setText(text);
			parent.getItems().add(one_sub_item);
			done += returned_count_of_new_menu_items;
		}
		return done;
	}

	// create a leaf in the menu trees:
	//**********************************************************
	private int create_menu_items_for(Browser browser, Menu parent, Iterator<String> path_iterator)
	//**********************************************************
	{
		int returned_count_of_new_menu_items =  0;
		//logger.log(name)
		for ( int i = 0 ; i < MAX_MENU_ITEMS ; i++)
		{
			if (!path_iterator.hasNext()) break;
			String full_path = path_iterator.next();
			Path path = Path.of(full_path);
			//String p = f.getAbsolutePath()+ " "+local;
			String displayed_text = path.getFileName().toString() + "      full path: "+full_path;
			MenuItem one_sub_item = new MenuItem(displayed_text);
			returned_count_of_new_menu_items++;
			displayed_items_sequence_number++;
			parent.getItems().add(one_sub_item);
			one_sub_item.setOnAction(e -> {
				logger.log("going to open on menu select: "+displayed_text);

				if (Guess_file_type.is_file_an_image(path.toFile()))
				{
					Image_window is = Image_window.get_Image_window(browser, path, logger);
				}
				else if (Guess_file_type.is_this_path_a_music(path))
				{
					logger.log("opening audio file: " + path.toAbsolutePath());
					Audio_player.play_song(path.toFile(),logger);
				}
				else
				{
					System_open_actor.open_with_system(browser,path,logger);
				}
			});
		}
		return returned_count_of_new_menu_items;
	}


}
