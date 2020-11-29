package klik.deduplicate;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import javafx.stage.Screen;
import javafx.stage.Stage;
import klik.change.Status_old_and_new_Path;
import klik.change.Command_old_and_new_Path;
import klik.change.Old_and_new_Path;
import klik.images.From_disk;
import klik.images.Image_stage;
import klik.look.Look_and_feel_manager;
import klik.util.Logger;
import klik.look.Look_and_feel;
import klik.util.Tool_box;


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
	File file_of_the_images[];


	HBox hbox;
	File initial_dir =  null;
	int N;
	Againor againor;

	//**********************************************************
	public N_image_stage(
			File file_of_the_images_[],
			Againor againor_,
			Logger logger_
			)
	//**********************************************************
	{
		againor = againor_;
		file_of_the_images = file_of_the_images_;
		N = file_of_the_images.length;
		logger = logger_;
		for (int i = 1 ; i <= file_of_the_images.length ; i++ )
		{
			logger.log("f"+i+"="+file_of_the_images[i-1].getName());			
		}
		image_views = new ImageView[file_of_the_images.length];
		fxImages = new javafx.scene.image.Image[file_of_the_images.length];

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				stage = new Stage();
				hbox = new HBox();
				Scene scene = new Scene(hbox);
				stage.setScene(scene);//, W, H));
/*
				stage.widthProperty().addListener((obs, oldVal, newVal) -> {
					reset_window_size_to_default(stage);
					set_images_by_files(file_of_the_images);
				});
				stage.heightProperty().addListener((obs, oldVal, newVal) -> {
					reset_window_size_to_default(stage);
					set_images_by_files(file_of_the_images);
				});
*/
				stage.addEventHandler(MouseEvent.MOUSE_CLICKED,
						new EventHandler<MouseEvent>()
						{
							public void handle(final MouseEvent mouseEvent)
							{
								if (mouseEvent.getButton() == MouseButton.SECONDARY)
								{
									handle_mouse(logger, stage, mouseEvent);
								}
							}
						});

				if (set_images_by_files(file_of_the_images) == false)
				{
					stage.close();
					againor.again();
					return;
				}
				stage.show();
				//set_stage_size_to_fullscreen(stage);
			}
		});


	}

	//**********************************************************
	private void reset_window_size_to_default(Stage stage) 
	//**********************************************************
	{
		W = stage.getWidth();
		H = stage.getHeight();
	}


	//**********************************************************
	private void set_stage_size_to_fullscreen(Stage stage) 
	//**********************************************************
	{
		Screen screen = null;
		if ( stage.isShowing() )
		{
			// we detect on which SCREEN the stage is (the user may have moved it)
			double minX = stage.getX();
			double minY = stage.getY();
			double width = stage.getWidth();
			double height = stage.getHeight();
			Rectangle2D r = new Rectangle2D(minX+10 , minY+10 , width-100 , height-100 );
			ObservableList<Screen> screens = Screen.getScreensForRectangle(r);
			for ( Screen s : screens )
			{
				screen = s;
			}
		}
		else
		{
			screen = Screen.getPrimary();
		}

		//Screen screen = Screen.getPrimary();
		Rectangle2D bounds = screen.getVisualBounds();

		Scene scene = stage.getScene();
		//logger.log("scene getX"+scene.getX());
		//logger.log("scene getY"+scene.getY());
		stage.setX(bounds.getMinX());
		stage.setY(bounds.getMinY());
		stage.setWidth(bounds.getWidth());
		stage.setHeight(bounds.getHeight());

		W = stage.getWidth();
		H = stage.getHeight()-scene.getY();
	}


	static Comparator<? super File> comp_by_path_length = new Comparator<File>() {
		@Override
		public int compare(File o1, File o2) {
			Integer i1 = o1.getAbsolutePath().toString().length();
			Integer i2 = o2.getAbsolutePath().toString().length();
			return i1.compareTo(i2);
		}
	};

	//**********************************************************
	protected boolean set_images_by_files(File the_image_files[])
	//**********************************************************
	{
		String title = "";
		hbox.getChildren().clear();

		Arrays.sort(the_image_files,comp_by_path_length);

		for ( int i = 0 ; i < the_image_files.length ; i++)
		{
			File local_file = the_image_files[i];
			if ( local_file == null)
			{
				logger.log("local_file == null");
				return false;
			}
			if ( local_file.exists() == false)
			{
				logger.log("file already gone");
				return false;
			}
			title += local_file.getName()+"-";

			Image image = From_disk.load_image_fx_from_disk(local_file.toPath(),logger);
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
	private void handle_mouse(Logger logger, Stage stage, MouseEvent e)
	//**********************************************************
	{
		logger.log("handle_mouse");
		final ContextMenu contextMenu = new ContextMenu();
		contextMenu.setStyle("-fx-foreground-color: white;-fx-background-color: darkgrey;");

		/*
		MenuItem redisplay = new MenuItem("redisplay");
		contextMenu.getItems().add(redisplay);
		redisplay.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event)
			{
				//xsshow_first_one_jpeg(logger);
				set_images();
			}


		});
		*/

		for ( int i = 0 ; i < file_of_the_images.length ; i++)
		{
			File f = file_of_the_images[i];

			MenuItem file_info = new MenuItem("INFO File"+i+"="+ f.getAbsolutePath());
			contextMenu.getItems().add(file_info);
			file_info.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event)
				{
					String title = f.getAbsolutePath();
					FX_popup p = new FX_popup(title , f, 600, 600, logger);
				}
			});

			MenuItem file_open = new MenuItem("Open File"+i+"="+ f.getAbsolutePath());
			contextMenu.getItems().add(file_open);
			file_open.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event)
				{

					Image_stage is = Image_stage.get_Image_stage(null,f.toPath(), true, logger);
				}
			});

			MenuItem delete_file_info = new MenuItem("Delete File"+i+"="+ f.getAbsolutePath());
			contextMenu.getItems().add(delete_file_info);
			delete_file_info.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event)
				{
					List<Old_and_new_Path> l = new ArrayList<>();
					l.add(new Old_and_new_Path(f.toPath(),null, Command_old_and_new_Path.command_delete, Status_old_and_new_Path.before_command));
					Tool_box.safe_delete_all(l,logger);

					againor.again();
					stage.close();
				}
			});
		}

		MenuItem skip = new MenuItem("Skip this pair");
		contextMenu.getItems().add(skip);
		skip.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event)
			{
				againor.again();
				stage.close();
			}
		});
		contextMenu.show(hbox, e.getScreenX(), e.getScreenY());
	}


}

