package klik.unstable.experimental.performance_monitoring;

import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import klik.util.log.Logger;
import klik.util.ui.Jfx_batch_injector;

//**********************************************************
public class Performance_stage
//**********************************************************
{
    public static final double WIDTH = 1200;
    private static Performance_stage instance;
    private TextFlow textFlow;
    private final Logger logger;
    private Stage the_stage;

    //**********************************************************
    public static void show_stage(Logger logger)
    //**********************************************************
    {
        if ( instance == null) instance = new Performance_stage(logger);
        instance.the_stage.show();
    }

    //**********************************************************
    private Performance_stage(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        textFlow = new TextFlow();
        textFlow.setLayoutX(40);
        textFlow.setLayoutY(40);


        ScrollPane sp = new ScrollPane();
        sp.setPrefSize(WIDTH, 1000);
        sp.setContent(textFlow);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        the_stage = new Stage();
        the_stage.setHeight(600);
        the_stage.setWidth(WIDTH);

        Scene scene = new Scene(sp, WIDTH, 600, Color.WHITE);
        the_stage.setTitle("Performance history");
        the_stage.setScene(scene);
        the_stage.show();

    }

    //**********************************************************
    public static void clear()
    //**********************************************************
    {
        if ( instance != null) instance.clear_internal();
    }

    //**********************************************************
    private void clear_internal()
    //**********************************************************
    {
        Jfx_batch_injector.inject(new Runnable() {
            @Override
            public void run() {
                textFlow.getChildren().clear();
            }
        },instance.logger);
    }

    //**********************************************************
    public static void add(String line)
    //**********************************************************
    {
        if ( instance != null) instance.add_internal(line);
    }

    //**********************************************************
    private void add_internal(String line)
    //**********************************************************
    {
        Jfx_batch_injector.inject(new Runnable() {
            @Override
            public void run() {
                Text text = new Text(line+"\n");
                text.setFont(Font.font("verdana", FontWeight.NORMAL, FontPosture.REGULAR,24));
                text.setWrappingWidth(WIDTH);
                textFlow.getChildren().add(text);
            }
        },instance.logger);


    }
}
