package klik.util.active_list_stage;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

//**********************************************************
public class Active_list_stage
//**********************************************************
{

    public static final int WIDTH = 1000;

    //**********************************************************
    public static void show_active_list_stage(String title, Map<LocalDateTime,String> lines, Active_list_stage_action on_action)
    //**********************************************************
    {
       // List<Button> list = new ArrayList<>();
        ScrollPane sp = new ScrollPane();
        sp.setPrefSize(WIDTH, 1000);
        VBox vbox = new VBox();
        sp.setContent(vbox);
        List<LocalDateTime> keys = new ArrayList<>(lines.keySet());
        Collections.sort(keys);
        Collections.reverse(keys);

        for(LocalDateTime key: keys)
        {
            String time_stamp_string = key.getYear()
                    +" "+key.getMonth()+
                    " " +String.format("%02d",key.getDayOfMonth())
                    +" "+String.format("%02d",key.getHour())
                    +"h"+String.format("%02d",key.getMinute());
            String button_text = time_stamp_string+ " "+lines.get(key);

            Button b = new Button(button_text);
            b.setAlignment(Pos.BASELINE_LEFT);
            b.setTextAlignment(TextAlignment.LEFT);
            b.setPrefWidth(WIDTH);
            if (on_action != null){
                b.setOnAction(actionEvent -> on_action.on_click(lines.get(key)));
            }

           // list.add(b);
            vbox.getChildren().add(b);
        }
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        Stage local_stage = new Stage();
        local_stage.setHeight(600);
        local_stage.setWidth(1000);

        Scene scene = new Scene(sp, 1000, 600, Color.WHITE);
        local_stage.setTitle(title);
        local_stage.setScene(scene);
        local_stage.show();
    }


}
