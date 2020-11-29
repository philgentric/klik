package klik.browser;

import java.nio.file.Path;

// signal that it was not possible to make an icon for this file
// so it show be displayed as non-image (with a button instead of an ImageView)
public interface Exception_recorder {
    public void record(Path path);
}
