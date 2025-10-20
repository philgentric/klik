//SOURCES ../My_File_and_status.java
//SOURCES ../../../util/files_and_paths/File_pair.java
//SOURCES ../../../audio/UI_instance_holder.java
package klik.experimental.deduplicate.manual;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.audio.Audio_player_access;
import klik.browser.items.Item_file_with_icon;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.path_lists.Path_list_provider;
import klik.properties.Non_booleans_properties;
import klik.util.files_and_paths.old_and_new.Command;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.files_and_paths.old_and_new.Status;
import klik.util.image.Full_image_from_disk;
import klik.util.ui.Jfx_batch_injector;
import klik.util.execute.System_open_actor;
import klik.util.files_and_paths.*;
import klik.look.Look_and_feel_manager;
import klik.properties.File_sort_by;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


//**********************************************************
public class Stage_with_2_images
//**********************************************************
{
	Stage stage;
	public double H = 1000;
	public double W = 1400;
	Logger logger;

	public final Window owner;
	public final Aborter aborter;
	VBox the_big_vbox;
	Againor againor;
	private final AtomicInteger count_deleted;

	//**********************************************************
	public Stage_with_2_images(
			String title,
			Window owner,
			File_pair pair,
			Againor againor_,
			AtomicInteger count_deleted_,
			Path_list_provider path_list_provider,
			Path_comparator_source path_comparator_source,
			Aborter private_aborter_,
			Logger logger_
			)
	//**********************************************************
	{
		this.owner = owner;
		logger = logger_;
		count_deleted = count_deleted_;
		aborter = private_aborter_;

		// there was an obscure bug with random order?
		if ( File_sort_by.get_sort_files_by(path_list_provider.get_folder_path(),owner) == File_sort_by.RANDOM_ASPECT_RATIO)
		{
			File_sort_by.set_sort_files_by(path_list_provider.get_folder_path(),File_sort_by.NAME,owner,logger);
		}

		logger.log("Stage_with_2_images !");

		againor = againor_;


		Jfx_batch_injector.inject(() ->{
				stage = new Stage();
				//stage.setAlwaysOnTop(true);
				the_big_vbox = new VBox();
				Look_and_feel_manager.set_region_look(the_big_vbox,stage,logger);
				Scene scene = new Scene(the_big_vbox);
				stage.setScene(scene);//, W, H));
				stage.setOnCloseRequest((e) -> aborter.abort("Stage_with_2_images closing"));

				if (!set_images_by_files(title,pair,path_list_provider, path_comparator_source,aborter))
				{
					stage.hide();
					againor.again();
					return;
				}
				stage.show();
				//set_stage_size_to_fullscreen(stage);
			},logger);


	}

	static Comparator<? super File> comp_by_path_length = Comparator.comparingInt((File o) -> o.getAbsolutePath().length());

	//**********************************************************
	protected boolean set_images_by_files(String title, File_pair the_pair,
										  Path_list_provider path_list_provider,
										  Path_comparator_source path_comparator_source,
										  Aborter aborter)
	//**********************************************************
	{
		the_big_vbox.getChildren().clear();

		Button skip = new Button("Skip this pair");
		Look_and_feel_manager.set_button_look(skip,true,stage,logger);
		the_big_vbox.getChildren().add(skip);
		skip.setOnAction((ActionEvent e) -> {
            againor.again();
            if ( stage != null) stage.hide();
        });

		File[] the_image_files = new File[2];
		the_image_files[0] = the_pair.f1();
		the_image_files[1] = the_pair.f2();
		Arrays.sort(the_image_files,comp_by_path_length);

		HBox hbox = new HBox();
		the_big_vbox.getChildren().add(hbox);
		Display_data dd = new Display_data(title, 0, 0);
		for ( int i = 0 ; i < the_image_files.length ; i++)
		{
			dd = display_one_picture_with_buttons(the_pair, path_list_provider, path_comparator_source, aborter,dd, hbox, the_image_files[i]);
			if (dd == null) return false;

			if ( i == 0) {
				Separator separator = new Separator();
				separator.setOrientation(Orientation.VERTICAL);
				separator.setStyle("-fx-background-color: blue;");
				hbox.getChildren().add(separator);
			}
		}
		stage.setWidth(W);
		//stage.setHeight(H);
		stage.setTitle("Distance: "+title);
		return true;
	}

	record Display_data(String title, double image_width, double image_height){}
	//**********************************************************
	private Display_data display_one_picture_with_buttons(
			File_pair the_pair,
			Path_list_provider path_list_provider,
			Path_comparator_source path_comparator_source,
			Aborter aborter,
			Display_data previous, HBox hbox, File file)
	//**********************************************************
	{
		VBox the_vbox = new VBox();
		if ( file == null)
		{
			logger.log("file == null");
			return null;
		}
		if (!file.exists())
		{
			logger.log("file already gone");
			return null;
		}
		String title = previous.title()+" "+file.getName();
		double width = 0;
		double height = 0;

		Button view = new Button("Open this one");
		Look_and_feel_manager.set_button_look(view,true,stage,logger);
		view.setOnAction(event -> {
			boolean is_image = true;
			if ( !Guess_file_type.is_this_file_an_image(the_pair.f1())) is_image = false;
			if ( !Guess_file_type.is_this_file_an_image(the_pair.f2())) is_image = false;
            if (is_image)
			{
				Runnable r = () -> Platform.runLater(()->Item_file_with_icon.open_an_image(path_list_provider, path_comparator_source,file.toPath(),owner,logger));
				Actor_engine.execute(r,"Open image",logger);
            }
			else
			{
				if ( Guess_file_type.is_this_extension_an_audio(Extensions.get_extension(file.getName())))
				{
					Audio_player_access.play_song_in_separate_process(file,logger);
				}
				else
				{
					System_open_actor.open_with_system(file.toPath(), owner, aborter, logger);
				}
            }
        });
		the_vbox.getChildren().add(view);

		Button delete_button = new Button("Delete this one");
		Look_and_feel_manager.set_button_look(delete_button,true,stage,logger);
		delete_button.setOnAction(event -> {
            List<Old_and_new_Path> l = new ArrayList<>();
			Path p = file.toPath();
			Path trash_dir = Non_booleans_properties.get_trash_dir(p,owner,logger);
			Path new_Path = (Paths.get(trash_dir.toString(), p.getFileName().toString()));

			l.add(new Old_and_new_Path(p, new_Path, Command.command_move_to_trash, Status.before_command,false));
            double x = stage.getX()+100;
			double y = stage.getY()+100;
			Moving_files.safe_delete_files(l, x,y,stage,aborter,logger);
			count_deleted.incrementAndGet();

			againor.again();
            if ( stage != null) stage.close();
        });
		the_vbox.getChildren().add(delete_button);
		double w = W/2;
		{
			HBox hbox2 = new HBox();
			{
				Label label = new Label("Folder:"+file.getParentFile().getAbsolutePath());
				Look_and_feel_manager.set_region_look(label,stage,logger);
				label.setMinWidth(w);
				label.setWrapText(true);
				label.setTextOverrun(OverrunStyle.LEADING_WORD_ELLIPSIS);
				hbox2.getChildren().add(label);
			}
			Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer,owner,logger);
			HBox.setHgrow(spacer, Priority.ALWAYS);
			hbox2.getChildren().add(spacer);
			the_vbox.getChildren().add(hbox2);
		}
		{
			HBox hbox2 = new HBox();
			{
				Label label = new Label("File:"+file.getName());
				Look_and_feel_manager.set_region_look(label,stage,logger);
				label.setMinWidth(w);
				label.setWrapText(true);
				hbox2.getChildren().add(label);

			}
			Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer,owner,logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
			hbox2.getChildren().add(spacer);
			the_vbox.getChildren().add(hbox2);
		}
		{
			HBox hbox2 = new HBox();
			{
				String size_in_kB = file.length()/1000+"kB";
				Label label = new Label("File size: "+size_in_kB);
				Look_and_feel_manager.set_region_look(label,stage,logger);
				label.setMinWidth(w);
				label.setWrapText(true);
				hbox2.getChildren().add(label);

			}
			Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer,owner,logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
			hbox2.getChildren().add(spacer);
			the_vbox.getChildren().add(hbox2);
		}
		boolean is_image = true;
		if ( !Guess_file_type.is_this_file_an_image(the_pair.f1())) is_image = false;
		if ( !Guess_file_type.is_this_file_an_image(the_pair.f2())) is_image = false;
		if ( is_image)
		{
			Optional<Image> op = Full_image_from_disk.load_native_resolution_image_from_disk(file.toPath(), true, owner,aborter, logger);
            if (op.isPresent())
            {
                Image image = op.get();
                HBox hbox2 = new HBox();
                {
                    width = image.getWidth();
                    height = image.getHeight();
                    String lab = "Image size: " + width + " x " + height;
                    boolean same = true;
                    if (previous.image_width() != width) same = false;
                    if (previous.image_height() != height) same = false;
                    if (same) {
                        lab += " ========  SAME SIZE";
                    }
                    Label label = new Label(lab);
                    Look_and_feel_manager.set_region_look(label, stage, logger);
                    label.setMinWidth(w);
                    label.setWrapText(true);
                    hbox2.getChildren().add(label);
                }
                Region spacer = new Region();
                Look_and_feel_manager.set_region_look(spacer,owner,logger);
                HBox.setHgrow(spacer, Priority.ALWAYS);
                hbox2.getChildren().add(spacer);
                the_vbox.getChildren().add(hbox2);

                ImageView image_view = new ImageView(image);
                image_view.setPreserveRatio(true);
                image_view.setFitWidth(w);
                image_view.setFitHeight(H);
                the_vbox.getChildren().add(image_view);
            }
		}

		hbox.getChildren().add(the_vbox);
		return new Display_data(title,width,height);
	}

	public void close() {
		if ( stage != null) stage.close();
	}

	public void set_pair(
			String title,
			File_pair pair,
			Path_list_provider path_list_provider,
			Path_comparator_source path_comparator_source,
			Aborter aborter)
	{
		if ( !pair.both_file_exist()) return;
		set_images_by_files(title,pair, path_list_provider, path_comparator_source,aborter);
		stage.show();
	}

}

