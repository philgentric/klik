package klik.util.execute;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import klik.util.log.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


//**********************************************************
public class Debug_console
//**********************************************************
{

    //**********************************************************
    public static Node get_button(Window owner, Logger logger)
    //**********************************************************
    {
        Button exe = new Button("Open debug execution window");
        exe.setOnAction(event ->
        {
            create_debug_console(owner,logger);
        });
        return exe;
    }
    //**********************************************************
    private static void create_debug_console(Window owner, Logger logger)
    //**********************************************************
    {
        Stage stage = new Stage();
        stage.initOwner(owner);
        VBox vbox = new VBox();
        TextField tf = new TextField("< enter command here>");
        vbox.getChildren().add(tf);
        {
            Button exe = new Button("Execute (via file.sh)");
            vbox.getChildren().add(exe);
            exe.setOnAction(event ->
            {
                String cmd = tf.getText();
                Execute_via_script_in_tmp_file.execute(cmd, true, false, stage, logger);
            });
        }
        {
            Button exe = new Button("Execute (processBuilder)");
            vbox.getChildren().add(exe);
            TextArea ta = new TextArea();
            exe.setOnAction(event ->
            {
                String cmd = tf.getText();
                String[] pieces = cmd.split("\\s+");
                Execute_result es = Execute_command.execute_command_list_no_wait(List.of(pieces), new File("."), logger);
                if ( es.status())
                {
                    ta.setText(es.output());
                }
                else
                {
                    ta.setText("command failed, check logs");
                }
            });

        }


        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.show();

    }

}
