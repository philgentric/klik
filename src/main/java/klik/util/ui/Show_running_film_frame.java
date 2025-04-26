package klik.util.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Show_running_film_frame implements Hourglass
//**********************************************************
{
	public final Aborter aborter;
	private final int timeout_s;
	Logger logger;
	Stage stage;
	ImageView iv;
	long start;
	private final CountDownLatch latch = new CountDownLatch(1);

	//**********************************************************
	public static Hourglass show_running_film(Window owner, double x, double y, String wait_message, int timeout_s, Aborter aborter, Logger logger)
	//**********************************************************
	{
		Show_running_film_frame local = new Show_running_film_frame(aborter, timeout_s,logger);
		launch(local, wait_message, owner, x, y);
		return local;
	}


	//**********************************************************
	private static Hourglass launch(Show_running_film_frame local, String wait_message, Window owner, double x, double y)
	//**********************************************************
	{
		//logger.log("Show_running_film_frame: wait_message= "+wait_message);
		if ( Platform.isFxApplicationThread())
		{
			local.define_fx(wait_message, owner,x,y);
		}
		else
		{
			Jfx_batch_injector.now(()->local.define_fx(wait_message, owner, x,y));
		}
		return local;
	}

	//**********************************************************
	private Show_running_film_frame(Aborter aborter_, int timeout_s_, Logger logger_)
	//**********************************************************
	{
		aborter = aborter_;
		timeout_s = timeout_s_;
        logger = logger_;
	}

	//**********************************************************
	private void define_fx(String wait_message, Window owner, double x, double y)
	//**********************************************************
	{
		start = System.currentTimeMillis();
		//logger.log("Show_running_film_frame: "+wait_message);
		stage = new Stage();
		VBox vbox = new VBox();
		Look_and_feel_manager.set_region_look(vbox);

		vbox.setAlignment(javafx.geometry.Pos.CENTER);
		iv = new ImageView(Look_and_feel_manager.get_running_film_icon());
		iv.setFitHeight(100);
		stage.setMinWidth(300);
		iv.setPreserveRatio(true);
		vbox.getChildren().add(iv);


		Scene scene = new Scene(vbox);

		stage.setTitle(wait_message);//My_I18n.get_I18n_string("Wait", logger));
		stage.setScene(scene);
		stage.initOwner(owner);
		stage.setX(x);
		stage.setY(y);
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
					boolean b = latch.await(1, TimeUnit.SECONDS);
					if (!b)
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
		if ( sleep_time > 1000) sleep_time = 1000;
		Jfx_batch_injector.inject(() -> {
			stage.setTitle(message);//My_I18n.get_I18n_string("Search_Results_Ended", logger));
			iv.setImage(Look_and_feel_manager.get_sleeping_man_icon());
		},logger);

		if ( sleep) {
			long finalSleep_time = sleep_time;
			Runnable r = () -> {
				try {
					Thread.sleep(finalSleep_time);
				} catch (InterruptedException e) {
				}
				Jfx_batch_injector.inject(() -> stage.close(),logger);

			};

			Actor_engine.execute(r, logger);
		}
		else
		{
			Jfx_batch_injector.inject(() -> stage.close(),logger);
		}
	}

	@Override // Hourglass
	public void close() {
		latch.countDown();
	}


}
