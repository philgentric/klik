package klik.level2.deduplicate.manual;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.browser.Browser;
import klik.level2.deduplicate.My_File_and_status;
import klik.level2.deduplicate.console.Deduplication_console_window;
import klik.files_and_paths.Command_old_and_new_Path;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Old_and_new_Path;
import klik.files_and_paths.Status_old_and_new_Path;
import klik.images.Image_window;
import klik.look.Look_and_feel_manager;
import klik.util.From_disk;
import klik.util.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


//**********************************************************
public class N_image_stage
//**********************************************************
{
	Stage stage;
	public double W = 1200;
	public double H = 800;
	Logger logger;

	ImageView image_views[];
	javafx.scene.image.Image fxImages[];
	My_File_and_status file_of_the_images[];

	Aborter aborter = new Aborter();
	HBox hbox;
	File initial_dir =  null;
	int N;
	Againor againor;

	//**********************************************************
	public N_image_stage(
			Browser b,
			My_File_and_status file_of_the_images_[],
			Againor againor_,
			Logger logger_
			)
	//**********************************************************
	{
		logger = logger_;

		logger.log("N_image_stage !");

		againor = againor_;
		file_of_the_images = file_of_the_images_;
		N = file_of_the_images.length;
		for (int i = 1 ; i <= file_of_the_images.length ; i++ )
		{
			logger.log("f"+i+"="+file_of_the_images[i-1].my_file.file.getName());
		}
		image_views = new ImageView[file_of_the_images.length];
		fxImages = new javafx.scene.image.Image[file_of_the_images.length];

		Platform.runLater(() ->{
				stage = new Stage();
				hbox = new HBox();
				Scene scene = new Scene(hbox);
				stage.setScene(scene);//, W, H));
				stage.addEventHandler(MouseEvent.MOUSE_CLICKED,
						new EventHandler<MouseEvent>()
						{
							public void handle(final MouseEvent mouseEvent)
							{
								if (mouseEvent.getButton() == MouseButton.SECONDARY)
								{
									handle_mouse(b, stage, mouseEvent, logger);
								}
							}
						});

				if (!set_images_by_files(file_of_the_images))
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
	protected boolean set_images_by_files(My_File_and_status the_image_files[])
	//**********************************************************
	{
		String title = "";
		hbox.getChildren().clear();

		Arrays.sort(the_image_files,comp_by_path_length);

		for ( int i = 0 ; i < the_image_files.length ; i++)
		{
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

			Image image = From_disk.load_image_from_disk(local_file.my_file.file.toPath(),aborter, logger);
			ImageView image_view = new ImageView(image);
			image_view.setPreserveRatio(true);
			image_view.setSmooth(false);
			//image_view.setCache(true);
			int w = (int)(W/(double)N);
			image_view.prefWidth(w);
			image_view.setFitWidth(w);
			image_view.prefHeight(H);
			image_view.setFitHeight(H);
			hbox.getChildren().add(image_view);

		}
		stage.setWidth(W);
		stage.setHeight(H);
		stage.setTitle(title);
		return true;
	}
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
					Deduplication_console_window p = new Deduplication_console_window(title , 600, 600, false, new Aborter(), logger);
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
					l.add(new Old_and_new_Path(f.my_file.file.toPath(), null, Command_old_and_new_Path.command_move_to_trash, Status_old_and_new_Path.before_command));
					Files_and_Paths.safe_delete_files(the_Stage,l,new Aborter(),logger);

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


}

