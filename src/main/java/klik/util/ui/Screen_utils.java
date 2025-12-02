// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import klik.util.log.Stack_trace_getter;


//**********************************************************
public class Screen_utils
//**********************************************************
{

    //**********************************************************
    public static Point2D verify(double x, double y, double w, double h)
    //**********************************************************
    {
        ObservableList<Screen> sss = Screen.getScreensForRectangle(x, y, w, h);
        if ( sss.isEmpty())
        {
            return new Point2D(0,0);
        }
        if ( sss.size() == 1)
        {
            Screen the_screen = sss.get(0);
            Rectangle2D bounds = the_screen.getBounds();
            if (bounds.contains(x,y,w,h)) return new Point2D(x,y);
            return new Point2D(bounds.getMinX(),bounds.getMinY());
        }
        // we have 2 screens, get the current one
        ObservableList<Screen> screens = Screen.getScreensForRectangle(x, y, 1, 1);
        Screen screen = screens.get(0);
        Rectangle2D bounds = screen.getBounds();
        return new Point2D(bounds.getMinX(),bounds.getMinY());
    }
}
