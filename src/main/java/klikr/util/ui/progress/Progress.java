// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.ui.progress;

import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import klikr.look.Look_and_feel_manager;
import klikr.look.Look_and_feel_style;
import klikr.util.log.Logger;


//**********************************************************
public record Progress(Pane pane, ImageView iv, Progress_spinner spinner, Window owner, Logger logger)
//**********************************************************
{
    //**********************************************************
    public static Progress start(Pane pane, Window owner, Logger logger)
    //**********************************************************
    {
        if ( Look_and_feel_manager.get_instance(owner,logger).get_look_and_feel_style() == Look_and_feel_style.materiol)
        {
            logger.log("Progress: starting a spinner");
            Progress_spinner spinner = new Progress_spinner();
            Pane pane2 = spinner.start();
            pane.getChildren().add(pane2);
            return new Progress(pane,null,spinner,owner,logger);
        }
        else
        {
            logger.log("Progress: starting a 'film'");
            ImageView iv = new ImageView(Look_and_feel_manager.get_running_film_icon(owner, logger));
            iv.setFitHeight(100);
            iv.setPreserveRatio(true);
            pane.getChildren().add(iv);
            return new Progress(pane, iv,null,owner,logger);
        }
    }

    //**********************************************************
    public void stop()
    //**********************************************************
    {
        if ( iv() != null) iv().setImage(Look_and_feel_manager.get_the_end_icon(owner(), logger()));
        if ( spinner() != null) spinner().stop();
    }

    //**********************************************************
    public void remove()
    //**********************************************************
    {
        if ( iv() != null)
        {
            pane().getChildren().remove(iv());
        }
        if ( spinner() != null)
        {
            pane().getChildren().remove(spinner().stack_pane());
        }
    }
}
