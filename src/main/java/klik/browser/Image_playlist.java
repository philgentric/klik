package klik.browser;

import klik.browser.virtual_landscape.Path_list_provider;
import klik.util.files_and_paths.Old_and_new_Path;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.List;

public class Image_playlist extends Abstract_browser{

    public final List_path_list_provider path_list_provider;

    public Image_playlist(Browser_creation_context context, Logger logger)
    {
        super(context, logger);
        path_list_provider = new List_path_list_provider(context.folder_path);
    }
    @Override
    protected String get_name() {
        return path_list_provider.get_name();
    }

    @Override
    public Path_list_provider get_Path_list_provider() {
        return path_list_provider;
    }

    @Override
    public String signature() {
        return "";
    }

    @Override
    void monitor() {

    }

    @Override
    public void set_title() {

    }

    @Override
    public void go_full_screen() {

    }

    @Override
    public void stop_full_screen() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Logger logger2) {

    }

    @Override
    public String get_string() {
        return "";
    }
}
