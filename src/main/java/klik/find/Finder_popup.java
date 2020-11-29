package klik.find;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import klik.util.Logger;

import java.util.List;

//import javafx.scene.web.WebView;

//**********************************************************
public class Finder_popup
//**********************************************************
{
	TextFlow textFlow;
	double W;
	public Stage stage;

	//**********************************************************
	public Finder_popup(String title, List<String> strings_to_display, double w, double h, Logger logger)
	//**********************************************************
	{
		W = w;
		textFlow = new TextFlow();
		textFlow.setLayoutX(40);
		textFlow.setLayoutY(40);
		if ( strings_to_display != null)
		{
			for(String s :strings_to_display )
			{
				Text t = new Text(s);
				textFlow.getChildren().add(t);
				textFlow.getChildren().add(new Text(System.lineSeparator()));
			}
			
		}
		ScrollPane sp = new ScrollPane();
		sp.setPrefSize(w	,h);
		sp.setContent(textFlow);
		sp.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		sp.setHbarPolicy(ScrollBarPolicy.ALWAYS);

		stage = new Stage();
		stage.setHeight(h);
		stage.setWidth(w);

		Scene scene = new Scene(sp, w, h, Color.WHITE);
		stage.setTitle(title);
		stage.setScene(scene);
		stage.show();		
	}

	static final int MAGIC = 42;
	protected static final boolean ultra_dbg = false;
	int count_dot;
	int count_star;
	int count_$;
	
	private void reset()
	{
		count_dot = 0;
		count_star = 0;
		count_$ = 0;
		
	}
	//**********************************************************
	public void ping() 
	//**********************************************************
	{
		Platform.runLater(new Runnable() 
		{	
			@Override
			public void run() 
			{
				if ( count_dot < MAGIC)
				{
					Text t = new Text(".");
					textFlow.getChildren().add(t);					
					if ( ultra_dbg) System.out.println("."+count_dot);
					count_dot++;
				}
				else
				{
					boolean pas_total = false;
					if ( pas_total)
					{
						textFlow.getChildren().add(new Text(System.lineSeparator()));
						count_dot = 0;
					}
					else
					{
						erase(MAGIC);
						count_dot = 0;
						Text t_star = new Text("*");
						textFlow.getChildren().add(t_star);
						count_star++;
						if ( ultra_dbg) System.out.println("*"+count_star);
						if ( count_star >= MAGIC)
						{
							count_star = 0;
							erase(MAGIC);
							Text t_$ = new Text("$");
							textFlow.getChildren().add(t_$);
							count_$++;
							if ( ultra_dbg) System.out.println("$"+count_$);

							if ( count_$ >= MAGIC)
							{
								textFlow.getChildren().add(new Text(System.lineSeparator()));
								count_$ = 0;
							}
						}

					}
				}
			}



		});



	}

	//**********************************************************
	public void pong(String text) 
	//**********************************************************
	{
		Text t = new Text(text);
		Platform.runLater(new Runnable() 
		{	
			@Override
			public void run() 
			{
				reset();
				textFlow.getChildren().add(new Text(System.lineSeparator()));
				textFlow.getChildren().add(t);
				textFlow.getChildren().add(new Text(System.lineSeparator()));

			}
		});				
	}


	//**********************************************************
	private void erase(int n) 
	//**********************************************************
	{
		for ( int i = 0 ; i < n ; i++)
		{
			int k = textFlow.getChildren().size()-1;
			if ( k < 0) break;
			textFlow.getChildren().remove(k);
			if ( ultra_dbg) System.out.println("erase");
		}
	}
	
	
	
/*	
	static FX_popup  pop;
	//**********************************************************
	public static void main(String[] args)
	//**********************************************************
	{
		Logger logger =  new Logger(new File("log2.txt"), true);
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
				pop = new FX_popup("test", strings_to_display, 600, 600, logger);
			}
		});

		while( pop == null)
		{
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		for(int i = 0; i < 1000; i++)
		{
			if ( ultra_dbg) System.out.println("ping");
			pop.ping();
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		pop.pong("wop1!");
		for(int i = 0; i < 23435; i++)
		{
			if ( ultra_dbg) System.out.println("ping");
			pop.ping();
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		pop.pong("wop2!");
		for(int i = 0; i < 1000000; i++)
		{
			if ( ultra_dbg) System.out.println("ping");
			pop.ping();
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


	}
*/
}
