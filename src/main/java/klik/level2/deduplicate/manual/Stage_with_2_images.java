//SOURCES ../My_File_and_status.java
package klik.level2.deduplicate.manual;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.Browser;
import klik.util.ui.Jfx_batch_injector;
import klik.util.execute.System_open_actor;
import klik.util.files_and_paths.*;
import klik.level2.deduplicate.File_pair;
import klik.level2.deduplicate.My_File_and_status;
import klik.images.Image_window;
import klik.look.Look_and_feel_manager;
import klik.properties.File_sort_by;
import klik.properties.Static_application_properties;
import klik.util.files_and_paths.From_disk;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


//**********************************************************
public class Stage_with_2_images
//**********************************************************
{
	Stage stage;
	public double H = 1000;
	public double W = 1400;
	Logger logger;

	public final Browser browser;
	public final Aborter private_aborter;
	VBox the_big_vbox;
	Againor againor;

	//**********************************************************
	public Stage_with_2_images(
			Browser browser_,
			File_pair pair,
			Againor againor_,
			Aborter private_aborter_,
			Logger logger_
			)
	//**********************************************************
	{
		browser = browser_;
		logger = logger_;
		private_aborter = private_aborter_;

		// there is an obscure bug with random order
		if ( File_sort_by.get_sort_files_by(logger) == File_sort_by.RANDOM_ASPECT_RATIO)
		{
			File_sort_by.set_sort_files_by(File_sort_by.NAME, logger);
		}

		logger.log("Stage_with_2_images !");

		againor = againor_;


		Jfx_batch_injector.inject(() ->{
				stage = new Stage();
				the_big_vbox = new VBox();
				Look_and_feel_manager.set_region_look(the_big_vbox);
				Scene scene = new Scene(the_big_vbox);
				stage.setScene(scene);//, W, H));
				stage.setOnCloseRequest((e) -> private_aborter.abort("Stage_with_2_images closing"));

				if (!set_images_by_files(pair,browser))
				{
					stage.hide();
					againor.again(true);
					return;
				}
				stage.show();
				//set_stage_size_to_fullscreen(stage);
			},logger);


	}

	static Comparator<? super My_File_and_status> comp_by_path_length = new Comparator<My_File_and_status>() {
		@Override
		public int compare(My_File_and_status o1, My_File_and_status o2) {
			Integer i1 = o1.my_file.file.getAbsolutePath().toString().length();
			Integer i2 = o2.my_file.file.getAbsolutePath().toString().length();
			return i1.compareTo(i2);
		}
	};

	//**********************************************************
	protected boolean set_images_by_files(File_pair the_pair, Browser browser)
	//**********************************************************
	{
		String title = "";
		the_big_vbox.getChildren().clear();

		Button skip = new Button("Skip this pair");
		Look_and_feel_manager.set_button_look(skip,true);
		the_big_vbox.getChildren().add(skip);
		skip.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event)
			{
				againor.again(false);
				if ( stage != null) stage.hide();
			}
		});

		My_File_and_status the_image_files[] = new My_File_and_status[2];
		the_image_files[0] = the_pair.f1;
		the_image_files[1] = the_pair.f2;
		Arrays.sort(the_image_files,comp_by_path_length);

		HBox hbox = new HBox();
		the_big_vbox.getChildren().add(hbox);
		for ( int i = 0 ; i < the_image_files.length ; i++)
		{
			title = display_one_picture_with_buttons(the_pair, browser, title, hbox, the_image_files[i]);
			if (title == null) return false;

			if ( i == 0) {
				Separator separator = new Separator();
				separator.setOrientation(Orientation.VERTICAL);
				separator.setStyle("-fx-background-color: blue;");
				hbox.getChildren().add(separator);
			}
		}
		stage.setWidth(W);
		//stage.setHeight(H);
		stage.setTitle(title);
		return true;
	}

	//**********************************************************
	private String display_one_picture_with_buttons(File_pair the_pair, Browser browser, String title, HBox hbox, My_File_and_status file_and_status)
	//**********************************************************
	{
		VBox the_vbox = new VBox();
		if ( file_and_status == null)
		{
			logger.log("file_and_status == null");
			return null;
		}
		if (!file_and_status.my_file.file.exists())
		{
			logger.log("file already gone");
			return null;
		}
		title += file_and_status.my_file.file.getName()+"-";


		Button view = new Button("View this one");
		Look_and_feel_manager.set_button_look(view,true);
		view.setOnAction(event -> {
            if (the_pair.is_image) {
                Image_window is = Image_window.get_Image_window(browser, file_and_status.my_file.file.toPath(), logger);
            } else {
                System_open_actor.open_with_system(browser, file_and_status.my_file.file.toPath(), logger);
            }
        });
		the_vbox.getChildren().add(view);

		Button delete_button = new Button("Delete this one");
		Look_and_feel_manager.set_button_look(delete_button,true);
		delete_button.setOnAction(event -> {
            List<Old_and_new_Path> l = new ArrayList<>();
			Path p = file_and_status.my_file.file.toPath();
			Path trash_dir = Static_application_properties.get_trash_dir(p,logger);
			Path new_Path = (Paths.get(trash_dir.toString(), p.getFileName().toString()));

			l.add(new Old_and_new_Path(p, new_Path, Command_old_and_new_Path.command_move_to_trash, Status_old_and_new_Path.before_command,false));
            Moving_files.safe_delete_files(stage,l, private_aborter,logger);
            againor.again(true);
            if ( stage != null) stage.close();
        });
		the_vbox.getChildren().add(delete_button);
		double w = W/2;
		{
			HBox hbox2 = new HBox();
			{
				Label label = new Label("Folder:"+file_and_status.my_file.file.getParentFile().getAbsolutePath());
				Look_and_feel_manager.set_region_look(label);
				label.setMinWidth(w);
				label.setWrapText(true);
				label.setTextOverrun(OverrunStyle.LEADING_WORD_ELLIPSIS);
				hbox2.getChildren().add(label);
			}
			Region spacer = new Region();
			HBox.setHgrow(spacer, Priority.ALWAYS);
			hbox2.getChildren().add(spacer);
			the_vbox.getChildren().add(hbox2);
		}
		{
			HBox hbox2 = new HBox();
			{
				Label label = new Label("File:"+file_and_status.my_file.file.getName());
				Look_and_feel_manager.set_region_look(label);
				label.setMinWidth(w);
				label.setWrapText(true);
				hbox2.getChildren().add(label);

			}
			Region spacer = new Region();
			HBox.setHgrow(spacer, Priority.ALWAYS);
			hbox2.getChildren().add(spacer);
			the_vbox.getChildren().add(hbox2);
		}
		if ( the_pair.is_image)
		{
			Image image = From_disk.load_native_resolution_image_from_disk(file_and_status.my_file.file.toPath(), true, private_aborter, logger);
			ImageView image_view = new ImageView(image);
			image_view.setPreserveRatio(true);
			image_view.setFitWidth(w);
			image_view.setFitHeight(H);
			the_vbox.getChildren().add(image_view);
		}

		hbox.getChildren().add(the_vbox);
		return title;
	}

	public void close() {
		if ( stage != null) stage.close();
	}

	public void set_pair(File_pair pair)
	{
		set_images_by_files(pair,browser);
		stage.show();
	}

	/*
	//**********************************************************
	private void handle_mouse(Browser b, Stage the_Stage, MouseEvent e, Logger logger)
	//**********************************************************
	{
		logger.log("handle_mouse");
		final ContextMenu context_menu = new ContextMenu();
		Look_and_feel_manager.set_context_menu_look(context_menu);


		for ( int i = 0 ; i < file_of_the_images.length ; i++)
		{
			My_File_and_status f = file_of_the_images[i];
			MenuItem file_info = new MenuItem("INFO File"+i+"="+ f.my_file.file.getAbsolutePath());
			context_menu.getItems().add(file_info);
			file_info.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event)
				{
					String title = f.my_file.file.getAbsolutePath();
					Deduplication_console_window p = new Deduplication_console_window(title , 600, 600, false, aborter, logger);
				}
			});

			MenuItem file_open = new MenuItem("Open File"+i+"="+ f.my_file.file.getAbsolutePath());
			context_menu.getItems().add(file_open);
			file_open.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event)
				{

					Image_window is = Image_window.get_Image_window(b,f.my_file.file.toPath(),logger);
				}
			});

			MenuItem delete_file_info = new MenuItem("Delete File"+i+"="+ f.my_file.file.getAbsolutePath());
			context_menu.getItems().add(delete_file_info);
			delete_file_info.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event)
				{
					List<Old_and_new_Path> l = new ArrayList<>();
					l.add(new Old_and_new_Path(f.my_file.file.toPath(), null, Command_old_and_new_Path.command_move_to_trash, Status_old_and_new_Path.before_command,false));
					Moving_files.safe_delete_files(the_Stage,l,aborter,logger);
					againor.again(true);
					the_Stage.close();
				}
			});
		}

		MenuItem skip = new MenuItem("Skip this pair");
		context_menu.getItems().add(skip);
		skip.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event)
			{
				againor.again(false);
				the_Stage.close();
			}
		});
		context_menu.show(hbox, e.getScreenX(), e.getScreenY());
	}
*/


}

