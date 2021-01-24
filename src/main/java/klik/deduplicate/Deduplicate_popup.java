package klik.deduplicate;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import klik.change.Command_old_and_new_Path;
import klik.change.Old_and_new_Path;
import klik.change.Status_old_and_new_Path;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import klik.util.System_out_logger;
import klik.util.Tool_box;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

//import klik.util.Disruptor_logger;

//**********************************************************
public class Deduplicate_popup
//**********************************************************
{
	Stage stage;
	VBox vbox;
	double W;
	//**********************************************************
	public Deduplicate_popup(String title,
							 List<String> buttons_to_display,
							 double w, double h,
							 Againor againor,
							 Logger logger)
	//**********************************************************
	{
		stage = new Stage();
		vbox = new VBox();
		
		W = w;
		if ( buttons_to_display != null)
		{
			for(String s :buttons_to_display )
			{
				{
					Button b = new Button("OPEN file: "+s);
					b.setOnAction(new EventHandler<ActionEvent>() 
					{
						@Override public void handle(ActionEvent e) 
						{
							try
							{
								Desktop.getDesktop().open(new File(s));
							}
							catch (IOException e1)
							{
								logger.log(Stack_trace_getter.get_stack_trace(e1.toString()));
								Tool_box.popup_text("An error occurred",e1.toString());
							}
						}
					});				
					vbox.getChildren().add(b);
				}
				{
					Button b = new Button("DELETE file: "+s);
					b.setOnAction(new EventHandler<ActionEvent>() 
					{
						@Override public void handle(ActionEvent e) 
						{
							List<Old_and_new_Path> l = new ArrayList<Old_and_new_Path>();
							File old_file = new File(s);
							l.add(new Old_and_new_Path(old_file.toPath(), null, Command_old_and_new_Path.command_delete, Status_old_and_new_Path.before_command));
							Tool_box.safe_delete_all(l,logger);
							againor.again();
							stage.close();
						}
					});				
					vbox.getChildren().add(b);					
				}
			}
			
		}

		stage.setHeight(h);
		stage.setWidth(w);

		Scene scene = new Scene(vbox, w, h, Color.WHITE);
		stage.setTitle(title);
		stage.setScene(scene);
		stage.show();		
	}


	static Deduplicate_popup pop;
	//**********************************************************
	public static void main(String[] args)
	//**********************************************************
	{
		//Logger logger =  new Disruptor_logger("FX_popup2.txt", true);
		Logger logger =  new System_out_logger();
		try {
			SwingUtilities.invokeAndWait(() -> new JFXPanel());
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		List<String> strings_to_display = new ArrayList<String>();
		strings_to_display.add("sttring1");
		strings_to_display.add("sttringé");
		Platform.runLater(new Runnable() 
		{	
			@Override
			public void run() 
			{
				pop = new Deduplicate_popup("test", strings_to_display, 600, 600, null, logger);
			}
		});

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}

}
