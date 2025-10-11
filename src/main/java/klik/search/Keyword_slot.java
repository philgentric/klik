package klik.search;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import klik.look.Look_and_feel_manager;
import klik.look.my_i18n.My_I18n;
import klik.util.log.Logger;

//**********************************************************
public class Keyword_slot
//**********************************************************
{

    private final String[] keyword_holder = new String[1];
    private final Label result_keyword_label;
    private final Label result_count_label;
    private boolean is_red = false;
    public final HBox hbox1;
    public final HBox hbox2;

    //**********************************************************
    public Keyword_slot(String keyword_, Finder_frame finder_frame, boolean is_extension, Logger logger)
    //**********************************************************
    {
        keyword_holder[0] = keyword_;
        if (is_extension) keyword_holder[0] = "*."+keyword_holder[0];

        hbox1 = new HBox();
        hbox2 = new HBox();
        TextField the_keyword_textfield;
        result_keyword_label = new Label(keyword_holder[0]);
        Look_and_feel_manager.set_region_look(result_keyword_label,finder_frame.stage,logger);
        //result_keyword_label.setTextFill(Color.BLUE);
        result_keyword_label.setStyle(result_keyword_label.getStyle()+"-fx-text-fill: blue;");

        if (!is_extension)
        {
            finder_frame.top_keyword_vbox.getChildren().add(hbox1);
            the_keyword_textfield = new TextField(keyword_holder[0]);
            Look_and_feel_manager.set_TextField_look(the_keyword_textfield,finder_frame.stage,logger);
            the_keyword_textfield.setStyle("-fx-text-inner-color: blue;");

            hbox1.getChildren().add(the_keyword_textfield);
            if (is_extension) the_keyword_textfield.setEditable(false);
            the_keyword_textfield.setOnAction(actionEvent -> {
                if (is_red)
                {
                    the_keyword_textfield.setStyle("-fx-text-inner-color: blue;");
                    is_red = false;
                }
                String new_keyword = the_keyword_textfield.getText().trim();
                if (new_keyword.isBlank()) return;
                if (finder_frame.get_keyword_to_slot().containsKey(new_keyword)) return;
                finder_frame.session.stop_search();
                // CHANGE THE KEYWORD IN THE MAP
                finder_frame.get_keyword_to_slot().remove(keyword_holder[0]);
                keyword_holder[0] = new_keyword;
                finder_frame.get_keyword_to_slot().put(keyword_holder[0],this);
                result_keyword_label.setText(keyword_holder[0]);

                finder_frame.start_search();
            });

            the_keyword_textfield.textProperty().addListener((observableValue, old_val, new_val) -> {
                //logger.log("the_keyword_textfield  old_val:"+old_val+" new_val:"+new_val);
                if (!is_red)
                {
                    the_keyword_textfield.setStyle("-fx-text-inner-color: red;");
                    is_red = true;
                }
            });

            hbox1.getChildren().add(Finder_frame.horizontal_spacer(finder_frame.stage,logger));

            Button t4 = new Button(My_I18n.get_I18n_string("Remove_This_Keyword", finder_frame.stage,logger));
            Look_and_feel_manager.set_button_look(t4, true,finder_frame.stage,logger);
            t4.setOnAction(new EventHandler<>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    finder_frame.session.stop_search();
                    finder_frame.top_keyword_vbox.getChildren().remove(hbox1);
                    finder_frame.bottom_keyword_vbox.getChildren().remove(hbox2);
                    finder_frame.get_keyword_to_slot().remove(keyword_holder[0]);
                    finder_frame.start_search();
                }
            });
            hbox1.getChildren().add(t4);
        }

        // second part is in the result section
        {


            hbox2.getChildren().add(result_keyword_label);
            hbox2.getChildren().add(Finder_frame.horizontal_spacer(finder_frame.stage,logger));

            Label t2 = new Label(My_I18n.get_I18n_string("Was_Found_In", finder_frame.stage,logger));
            Look_and_feel_manager.set_region_look(t2,finder_frame.stage,logger);
            hbox2.getChildren().add(t2);
            hbox2.getChildren().add(Finder_frame.horizontal_spacer(finder_frame.stage,logger));

            result_count_label= new Label(""); // this is the label that will be updated during search with the match count
            Look_and_feel_manager.set_region_look(result_count_label,finder_frame.stage,logger);
            hbox2.getChildren().add(result_count_label);
            hbox2.getChildren().add(Finder_frame.horizontal_spacer(finder_frame.stage,logger));

            Label t4 = new Label( My_I18n.get_I18n_string("File_Names",finder_frame.stage,logger));
            Look_and_feel_manager.set_region_look(t4,finder_frame.stage,logger);
            hbox2.getChildren().add(t4);
            finder_frame.bottom_keyword_vbox.getChildren().add(hbox2);
        }
    }

    public Label get_result_label() {
        return result_count_label;
    }
}
