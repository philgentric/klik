package klik.search;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.look.Look_and_feel_manager;
import klik.util.Fx_batch_injector;
import klik.util.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Show_running_man_frame_with_abort_button
//**********************************************************
{
	public final Aborter aborter;
	private final int timeout_s;
	Logger logger;
	Stage stage;
	ImageView iv;
	long start;
	private final CountDownLatch latch = new CountDownLatch(1);
	Label in_flight_label;


	//**********************************************************
	public static Show_running_man_frame_with_abort_button show_running_man(String wait_message, int timeout_s, Logger logger)
	//**********************************************************
	{
		Show_running_man_frame_with_abort_button local = new Show_running_man_frame_with_abort_button(timeout_s, logger);
		launch(local, wait_message,logger);
		return local;
	}
	//**********************************************************
	private static CountDownLatch launch(Show_running_man_frame_with_abort_button local, String wait_message, Logger logger)
	//**********************************************************
	{
		if ( Platform.isFxApplicationThread())
		{
			local.define_fx(wait_message);
		}
		else
		{
			Fx_batch_injector.inject(()->local.define_fx(wait_message),logger);
		}
		return local.latch;
	}

	//**********************************************************
	private Show_running_man_frame_with_abort_button( int timeout_s_, Logger logger_)
	//**********************************************************
	{
		aborter = new Aborter("Show_running_man_frame",logger_);
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

		{
			in_flight_label = new Label("In flight items: ");
			vbox.getChildren().add(in_flight_label);
		}
		{
			Button abort = new Button("Abort");
			vbox.getChildren().add(abort);
			abort.setOnAction(e -> aborter.abort("aborted by user"));
		}


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
					//String x = in.poll(1, TimeUnit.SECONDS);
					boolean x = latch.await(1, TimeUnit.SECONDS);
					//if (x == null)
					if (!x)
					{
						// timeout
                        if (aborter.should_abort())
						{
							has_ended("aborted",false);
							return;
						}
                        else count++;
                        if ( count > timeout_s)
						{
							has_ended("Time count out", false);
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
		Actor_engine.execute(r,logger);
	}
	
	//**********************************************************
	public void has_ended(String message, boolean sleep)
	//**********************************************************
	{
		//logger.log("running man has ended "+message);

		long sleep_time = System.currentTimeMillis()-start;
		if ( sleep_time > 3000) sleep_time = 3000;
		Fx_batch_injector.inject(() -> {
			stage.setTitle(message);//I18n.get_I18n_string("Search_Results_Ended", logger));
			iv.setImage(Look_and_feel_manager.get_search_end_icon());
		},logger);

		if ( sleep) {
			long finalSleep_time = sleep_time;
			Runnable r = () -> {
				try {
					Thread.sleep(finalSleep_time);
				} catch (InterruptedException e) {
				}
				Fx_batch_injector.inject(() -> stage.close(),logger);

			};

			Actor_engine.execute(r, logger);
		}
		else
		{
			Fx_batch_injector.inject(() -> stage.close(),logger);
		}
	}

	public void close() {
		latch.countDown();
	}



	public void wait_and_block_until_finished(AtomicInteger in_flight)
	{
		Runnable tracker = () -> {
            for(;;)
            {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
				int in_flight_local = in_flight.get();

				Fx_batch_injector.inject(()-> in_flight_label.setText("Items in flight: " +in_flight_local),logger);

                if( in_flight_local == 0)
                {
                    close();
                    return;
                }
            }
        };
		Actor_engine.execute(tracker, logger);
	}
}
