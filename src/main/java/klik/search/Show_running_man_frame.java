package klik.search;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.look.Look_and_feel_manager;
import klik.util.Logger;
import klik.util.Stack_trace_getter;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Show_running_man_frame
//**********************************************************
{
	private final Aborter aborter;
	private final int timeout_s;
	Logger logger;
	Stage stage;
	ImageView iv;
	long start;
	private final LinkedBlockingDeque<String> in = new LinkedBlockingDeque<>();

	//**********************************************************
	public static LinkedBlockingDeque <String> show_running_man(String wait_message, int timeout_s, Aborter aborter, Logger logger)
	//**********************************************************
	{
		Show_running_man_frame local = new Show_running_man_frame(aborter, timeout_s, logger);
		Platform.runLater(()-> {
			local.define_fx(wait_message);
		});
		return local.in;
	}

	//**********************************************************
	private Show_running_man_frame(Aborter aborter_, int timeout_s_, Logger logger_)
	//**********************************************************
	{
		aborter = aborter_;
        timeout_s = timeout_s_;
        logger = logger_;
	}

	//**********************************************************
	private void define_fx(String wait_message)
	//**********************************************************
	{
		start = System.currentTimeMillis();
		logger.log("Show_running_man_frame: "+wait_message);
		stage = new Stage();
		VBox vbox = new VBox();
		Look_and_feel_manager.set_region_look(vbox);

		vbox.setAlignment(javafx.geometry.Pos.CENTER);
		iv = new ImageView(Look_and_feel_manager.get_search_icon());
		iv.setFitHeight(100);
		stage.setMinWidth(600);
		iv.setPreserveRatio(true);
		vbox.getChildren().add(iv);
		Scene scene = new Scene(vbox);

		stage.setTitle(wait_message);//I18n.get_I18n_string("Wait", logger));
		stage.setScene(scene);
//		stage.setX(Finder_frame.MIN_WIDTH);
//		stage.setY(0);
		stage.show();

		stage.addEventHandler(KeyEvent.KEY_PRESSED,
				key_event -> {
					if (key_event.getCode() == KeyCode.ESCAPE) {
						stage.close();
						key_event.consume();
					}
				});

		Runnable r = () -> {
			try {
                int count = 0;
                for(;;)
				{
                    String x = in.poll(1, TimeUnit.SECONDS);
                    if (x == null)
					{
                        if (aborter.should_abort())
						{
							has_ended("aborted",false);
							return;
						}
                        else count++;
                        if ( count > timeout_s)
						{
							timeout();
							return;
						}
						continue;
                    }
                    has_ended(wait_message + "... finished!", true);
                    return;
                }
			} catch (InterruptedException e) {
				logger.log("Show running man wait interrupted");
			}
		};
		Actor_engine.execute(r,new Aborter("Show running man",logger),logger);
	}

	//**********************************************************
	private void timeout()
	//**********************************************************
	{
		logger.log(Stack_trace_getter.get_stack_trace("Show running man, time out"));
		has_ended("Time out", false);
	}


	//**********************************************************
	public void has_ended(String message, boolean sleep)
	//**********************************************************
	{

		long sleep_time = System.currentTimeMillis()-start;
		if ( sleep_time > 3000) sleep_time = 3000;
		Platform.runLater(() -> {
			stage.setTitle(message);//I18n.get_I18n_string("Search_Results_Ended", logger));
			iv.setImage(Look_and_feel_manager.get_search_end_icon());
		});

		if ( sleep) {
			long finalSleep_time = sleep_time;
			Runnable r = () -> {
				try {
					Thread.sleep(finalSleep_time);
				} catch (InterruptedException e) {
				}
				Platform.runLater(() -> stage.close());
			};

			Actor_engine.execute(r, new Aborter("wait!", logger), logger);
		}
		else
		{
			Platform.runLater(() -> stage.close());
		}
	}
}
