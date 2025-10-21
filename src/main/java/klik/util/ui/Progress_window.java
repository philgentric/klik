package klik.util.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.Shared_services;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Progress_window implements Hourglass
//**********************************************************
{
    public final Aborter aborter;
	private final int timeout_s;
	Logger logger;
	Stage stage;
	ImageView iv;
	long start;
	public final CountDownLatch latch = new CountDownLatch(1);
	Label in_flight_label;
	Label ETA_label;
    private final boolean with_abort_button;
    Progress_spinner spinner;


	//**********************************************************
	public static Progress_window show(
            boolean with_abort_button,
            String wait_message,
            int timeout_s,
            double x,
            double y,
            Window owner,
            Logger logger)
	//**********************************************************
	{
		Progress_window local = new Progress_window(with_abort_button, timeout_s, logger);
		launch(local, wait_message,x,y,owner,logger);
		return local;
	}

	//**********************************************************
	public static Progress_window show(
            AtomicInteger in_flight,
            String wait_message,
            int timeout_s,
            double x,
            double y,
            Window owner,
            Logger logger)
	//**********************************************************
	{
        Progress_window local = new Progress_window(true, timeout_s, logger);
		launch(local, wait_message,x,y,owner,logger);
		local.report_progress_and_close_when_finished(in_flight);
		return local;
	}

	//**********************************************************
	private static Hourglass launch(
            Progress_window local,
            String wait_message,
            double x,
            double y,
            Window owner,
            Logger logger)
	//**********************************************************
	{
		if ( Platform.isFxApplicationThread())
		{
			local.define_fx(wait_message,owner,x,y);
		}
		else
		{
			Jfx_batch_injector.inject(()->local.define_fx(wait_message,owner,x,y),logger);
		}
		return local;
	}

	//**********************************************************
	private Progress_window(boolean with_abort_button, int timeout_s_, Logger logger_)
	//**********************************************************
	{
        this.with_abort_button = with_abort_button;
		if ( with_abort_button) aborter = new Aborter("Progress_window",logger_);
		else aborter = Shared_services.aborter();
        timeout_s = timeout_s_;
        logger = logger_;
	}

	//**********************************************************
	private void define_fx(String wait_message, Window owner, double x, double y)
	//**********************************************************
	{
		start = System.currentTimeMillis();
		//logger.log("Progress_window: "+wait_message);
		stage = new Stage();
        stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        stage.setMinWidth(300);
        stage.setX(x);
        stage.setY(y);

        VBox vbox = new VBox();
		Look_and_feel_manager.set_region_look(vbox,owner,logger);

		vbox.setAlignment(javafx.geometry.Pos.CENTER);

        switch(Look_and_feel_manager.get_instance(owner,logger).get_look_and_feel_style())
        {
            case light, dark, wood:
                iv = new ImageView(Look_and_feel_manager.get_running_film_icon(owner,logger));
                iv.setFitHeight(100);
                iv.setPreserveRatio(true);
                vbox.getChildren().add(iv);
                break;
            case modena:
            case material:
            default:
                spinner = new Progress_spinner();
                Pane pane = spinner.start();
                vbox.getChildren().add(pane);
                break;
        }

		{
			in_flight_label = new Label();
			vbox.getChildren().add(in_flight_label);
			Look_and_feel_manager.set_label_look(in_flight_label,owner,logger);
		}
		{
			ETA_label = new Label();
			vbox.getChildren().add(ETA_label);
			Look_and_feel_manager.set_label_look(ETA_label,owner,logger);
		}
        if ( with_abort_button)
		{
			Button abort = new Button("Abort");
            Look_and_feel_manager.set_button_look(abort,false,stage,logger);
			vbox.getChildren().add(abort);
			abort.setOnAction(e -> aborter.abort("aborted by user"));
		}


		Scene scene = new Scene(vbox);

		//stage.setTitle(wait_message);
        in_flight_label.setText(wait_message);
		stage.setScene(scene);
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
		Actor_engine.execute(r,"Progress window monitor",logger);
	}
	
	//**********************************************************
	public void has_ended(String message, boolean sleep)
	//**********************************************************
	{
		//logger.log("running man has ended "+error_message);

		long sleep_time = System.currentTimeMillis()-start;
		if ( sleep_time > 3000) sleep_time = 3000;
		Jfx_batch_injector.inject(() -> {
			//stage.setTitle(message);
            in_flight_label.setText(message);
            if (iv != null)
            {
                iv.setImage(Look_and_feel_manager.get_the_end_icon(stage, logger));
            }
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

			Actor_engine.execute(r, "sleep and close",logger);
		}
		else
		{
			Jfx_batch_injector.inject(() -> stage.close(),logger);
		}
	}

	@Override // Hourglass
	//**********************************************************
	public void close() {
		latch.countDown();
	}
	//**********************************************************



	//**********************************************************
	private void report_progress_and_close_when_finished(AtomicInteger in_flight)
	//**********************************************************
	{

		Runnable tracker = () -> {
			long start = System.currentTimeMillis();
			int start_amount = in_flight.get();
            for(;;)
            {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
				int in_flight_local = in_flight.get();

				if( in_flight_local<= 0)
				{
					close();
					return;
				}

				// in case in_flight increases in the meantime...
				if ( in_flight_local > start_amount)
				{
					start = System.currentTimeMillis();
					start_amount = in_flight_local;
				}

				long elapsed = System.currentTimeMillis() - start;
				int done = start_amount - in_flight_local;
				double speed = (double)done / elapsed * 1000; // items/s
				int eta_s = (int)((double)in_flight_local / speed);
				int eta_m = 0;
				int eta_h = 0;
				if ( eta_s > 60)
				{
					eta_m = eta_s / 60;
					eta_s = eta_s % 60;
					if ( eta_m > 60)
					{
						eta_h = eta_m / 60;
						eta_m = eta_m % 60;
					}
				}
				String eta_string;
				if ( eta_h > 0) eta_string = String.format("ETA: %02d hours %02d minutes %02d seconds", eta_h, eta_m, eta_s);
				else if ( eta_m > 0) eta_string = String.format("ETA: %02d m %02d s", eta_m, eta_s);
				else eta_string = String.format("ETA: %02d s", eta_s);

				String finalEta_string = eta_string;
				Jfx_batch_injector.inject(()->
				{
					ETA_label.setText(finalEta_string);
					in_flight_label.setText("Items in flight: " +in_flight_local);
				},logger);

            }
        };
		Actor_engine.execute(tracker, "Progress window ETA monitor",logger);
	}

	//**********************************************************
    public void set_text(String text)
	//**********************************************************
	{
		if (stage != null) {
			Jfx_batch_injector.inject(() -> in_flight_label.setText(text), logger);
		}
	}
}
