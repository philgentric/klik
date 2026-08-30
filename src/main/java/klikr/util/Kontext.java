package klikr.util;

import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

public record Kontext(Window owner, Aborter aborter, Logger logger) {
}
