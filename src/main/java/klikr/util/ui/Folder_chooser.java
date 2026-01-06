// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.ui;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import klikr.look.Look_and_feel;
import klikr.look.Look_and_feel_manager;
import klikr.properties.Non_booleans_properties;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.log.Logger;

import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

//**********************************************************
public class Folder_chooser
//**********************************************************
{

    //**********************************************************
    public static Path show_dialog_for_folder_selection(String title, Path initial_directory, Window owner, Logger logger)
    //**********************************************************
    {
        Stage dialog = new Stage(StageStyle.UTILITY);
        dialog.initModality(Modality.WINDOW_MODAL);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(title);

        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(8));
        Look_and_feel_manager.set_region_look(pane,owner,logger);


        Button up_button = new Button("Up");
        double font_size = Non_booleans_properties.get_font_size(owner,logger);
        double height = Look_and_feel.MAGIC_HEIGHT_FACTOR * font_size;
        Image icon = Look_and_feel_manager.get_up_icon(height,owner,logger);
        if (icon == null)
        {
            logger.log("WARNING: could not load " + Look_and_feel_manager.get_instance(owner,logger).get_up_icon_path());
            Look_and_feel_manager.set_button_look(up_button, true,owner, logger);
        }
        else
        {
            Look_and_feel_manager.set_button_and_image_look(up_button, icon, height, null, true, owner, logger);
        }
        CheckBox show_hidden_checkbox = new CheckBox("Show hidden folders");
        Look_and_feel_manager.set_CheckBox_look(show_hidden_checkbox,dialog,logger);
        {
            Look_and_feel look_and_feel = Look_and_feel_manager.get_instance(dialog, logger);
            double w = look_and_feel.estimate_text_width("Show hidden folders");
            show_hidden_checkbox.setMinWidth(2*w);
            show_hidden_checkbox.setPrefWidth(2*w);
            show_hidden_checkbox.setMaxWidth(2*w);
        }

        VBox top = new VBox();
        HBox top1 = new HBox(8, up_button, show_hidden_checkbox);
        HBox.setMargin(up_button, new Insets(0, 4, 0, 0));
        top1.setAlignment(Pos.CENTER_LEFT);
        top.getChildren().add(top1);

        TextField path_textfield = new TextField();
        Look_and_feel_manager.set_TextField_look(path_textfield,false,dialog,logger);
        path_textfield.setPromptText("Path");
        path_textfield.setMinWidth(1000);
        top.getChildren().add(path_textfield);

        pane.setTop(top);

        ListView<Path> list = new ListView<>();
        //list.setPlaceholder(new Label("No subfolders"));
        list.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    Path name = item.getFileName();
                    setText(name != null ? name.toString() : item.toString());
                }
            }
        });
        pane.setCenter(list);

        Button choose_that = new Button("Choose folder");
        Look_and_feel_manager.set_button_look(choose_that, true,owner, logger);
        //choose_that.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());

        Button cancel_button = new Button("Cancel");
        Look_and_feel_manager.set_button_look(cancel_button, true,owner, logger);
        choose_that.setDefaultButton(true);
        cancel_button.setCancelButton(true);
        HBox bottom = new HBox(8, choose_that, cancel_button);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(8, 0, 0, 0));
        pane.setBottom(bottom);

        Scene scene = new Scene(pane);
        dialog.setScene(scene);
        dialog.sizeToScene();

        // State
        Path[] answer = new Path[1];
        answer[0] = start_folder(initial_directory);
        path_textfield.setText(answer[0].toString());

        Runnable refresher = () -> {
            list.getItems().setAll(list_paths(answer[0], show_hidden_checkbox.isSelected(),owner,logger));
            list.getSelectionModel().clearSelection();
            up_button.setDisable(answer[0].getParent() == null);
            path_textfield.setText(answer[0].toString());
            choose_that.setText("Choose folder: " + answer[0].getFileName());
        };

        up_button.setOnAction(e -> {
            Path parent = answer[0].getParent();
            if (parent != null) {
                answer[0] = parent;
                refresher.run();
            }
        });

        show_hidden_checkbox.selectedProperty().addListener((obs, a, b) -> refresher.run());

        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Path sel = list.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    answer[0] = sel;
                    refresher.run();
                }
            }
        });

        list.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                Path sel = list.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    answer[0] = sel;
                    refresher.run();
                } else {
                    dialog.close();
                }
            } else if (e.getCode() == KeyCode.BACK_SPACE) {
                up_button.fire();
            }
        });

        path_textfield.setOnAction(e -> {
            Path p = Paths.get(path_textfield.getText().trim());
            if (Files.isDirectory(p)) {
                answer[0] = p.toAbsolutePath().normalize();
                refresher.run();
            } else {
                path_textfield.setText(answer[0].toString());
            }
        });

        final Path[] result = new Path[1];

        choose_that.setOnAction(e -> {
            Path sel = list.getSelectionModel().getSelectedItem();
            result[0] = (sel != null ? sel : answer[0]);
            dialog.close();
        });



        cancel_button.setOnAction(e -> {
            result[0] = null;
            dialog.close();
        });

        refresher.run();
        dialog.showAndWait();
        return result[0];
    }

    //**********************************************************
    private static Path start_folder(Path init)
    //**********************************************************
    {
        try {
            if (init != null && Files.isDirectory(init)) {
                return init.toAbsolutePath().normalize();
            }
        } catch (Exception ignored) {}

        try {
            Path home = Paths.get(System.getProperty("user.home"));
            if (Files.isDirectory(home)) return home.toAbsolutePath().normalize();
        } catch (Exception ignored) {}

        File[] roots = File.listRoots();
        if (roots != null && roots.length > 0) return roots[0].toPath().toAbsolutePath().normalize();
        return Paths.get("/").toAbsolutePath().normalize();
    }

    //**********************************************************
    private static List<Path> list_paths(Path folder, boolean also_hidden,Window owner,Logger logger)
    //**********************************************************
    {
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder)) {
            for (Path p : ds) {
                try {
                    if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
                        if (also_hidden || !Guess_file_type.should_ignore(p,owner,logger))
                        {
                            out.add(p);
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        out.sort(Comparator.comparing(
                p -> {
                    Path name = p.getFileName();
                    String s = name != null ? name.toString() : p.toString();
                    return s.toLowerCase(Locale.ROOT);
                }
        ));
        return out;
    }

}
