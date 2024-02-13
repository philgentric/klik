package klik.level2.deduplicate.manual;

import javafx.application.Platform;
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
import klik.browser.System_open_actor;
import klik.files_and_paths.*;
import klik.level2.deduplicate.File_pair;
import klik.level2.deduplicate.My_File_and_status;
import klik.images.Image_window;
import klik.look.Look_and_feel_manager;
import klik.properties.File_sort_by;
import klik.properties.Static_application_properties;
import klik.util.From_disk;
import klik.util.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


//**********************************************************
public class Stage_with_2_images
//**********************************************************
{
	Stage stage;
	public double W = 1200;
	//public double H = 800;
	Logger logger;

	File_pair the_pair;
	public final Aborter private_aborter;
	VBox the_big_vbox;
	Againor againor;

	//**********************************************************
	public Stage_with_2_images(
			Browser browser,
			File_pair pair,//My_File_and_status file_of_the_images_[],
			Againor againor_,
			Aborter private_aborter_,
			Logger logger_
			)
	//**********************************************************
	{
		logger = logger_;
		private_aborter = private_aborter_;

		// there is an obscure bug with random order
		if ( Static_application_properties.get_sort_files_by(logger) == File_sort_by.RANDOM_ASPECT_RATIO)
		{
			Static_application_properties.set_sort_files_by(File_sort_by.NAME, logger);
		}

		logger.log("Stage_with_2_images !");

		againor = againor_;
		the_pair = pair;


		Platform.runLater(() ->{
				stage = new Stage();
				the_big_vbox = new VBox();
				Scene scene = new Scene(the_big_vbox);
				stage.setScene(scene);//, W, H));
				stage.setOnCloseRequest((e) -> private_aborter.abort());

				if (!set_images_by_files(browser))
				{
					stage.close();
					againor.again(true);
					return;
				}
				stage.show();
				//set_stage_size_to_fullscreen(stage);
			});


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
	protected boolean set_images_by_files(Browser browser)
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
				stage.close();
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
			VBox the_vbox = new VBox();
			My_File_and_status local_file = the_image_files[i];
			if ( local_file == null)
			{
				logger.log("local_file == null");
				return false;
			}
			if (!local_file.my_file.file.exists())
			{
				logger.log("file already gone");
				return false;
			}
			title += local_file.my_file.file.getName()+"-";

			int w = (int) (W / (double) 2);
			if ( the_pair.is_image) {
				Image image = From_disk.load_native_resolution_image_from_disk(local_file.my_file.file.toPath(), true, private_aborter, logger);
				ImageView image_view = new ImageView(image);
				image_view.setPreserveRatio(true);
				image_view.setSmooth(false);
				//image_view.setCache(true);
				image_view.prefWidth(w);
				image_view.setFitWidth(w);
				the_vbox.getChildren().add(image_view);
			}
			//else
			{
				HBox hbox2 = new HBox();
				Label label = new Label("File:"+local_file.my_file.file.getAbsolutePath());
				Look_and_feel_manager.set_region_look(label);
				label.setPrefWidth(w);
				label.setWrapText(true);
				hbox2.getChildren().add(label);
				Region spacer = new Region();
				HBox.setHgrow(spacer, Priority.ALWAYS);
				hbox2.getChildren().add(spacer);
				the_vbox.getChildren().add(hbox2);
			}
			Button view = new Button("View this one");
			Look_and_feel_manager.set_button_look(view,true);
			view.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event)
				{
					if ( the_pair.is_image)
					{
						Image_window is = Image_window.get_Image_window(browser,local_file.my_file.file.toPath(),logger);
					}
					else {
						System_open_actor.open_with_system(browser,local_file.my_file.file.toPath(),logger);
					}
				}
			});
			the_vbox.getChildren().add(view);

			Button delete_button = new Button("Delete this one");
			Look_and_feel_manager.set_button_look(delete_button,true);
			delete_button.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event)
				{
					List<Old_and_new_Path> l = new ArrayList<>();
					l.add(new Old_and_new_Path(local_file.my_file.file.toPath(), null, Command_old_and_new_Path.command_move_to_trash, Status_old_and_new_Path.before_command,false));
					Moving_files.safe_delete_files(stage,l, private_aborter,logger);
					againor.again(true);
					stage.close();
				}
			});
			the_vbox.getChildren().add(delete_button);
			hbox.getChildren().add(the_vbox);

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

	public void close() {
		stage.close();
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

