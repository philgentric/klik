package klik.experimental.image_playlist;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.browser.Move_provider;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.change.Change_gang;
import klik.util.files_and_paths.Command_old_and_new_Path;
import klik.util.files_and_paths.Old_and_new_Path;
import klik.util.files_and_paths.Status_old_and_new_Path;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Playlist_path_list_provider implements Path_list_provider
//**********************************************************
{
    public static final String KLIK_IMAGE_PLAYLIST_EXTENSION = "klik_image_playlist";

    public final Path the_playlist_file_path;
    public final List<String> paths = new ArrayList<>();
    public final Logger logger;

    //**********************************************************
    public Playlist_path_list_provider(Path path, Logger logger)
    //**********************************************************
    {
        this.logger = logger;

        the_playlist_file_path = path;
        reload();

    }

    //**********************************************************
    @Override
    public Path get_path()
    //**********************************************************
    {
        return the_playlist_file_path;
    }
    //**********************************************************
    @Override
    public String get_name2()
    //**********************************************************
    {
        return the_playlist_file_path.toAbsolutePath().toString();
    }

    //**********************************************************
    @Override
    public List<Path> get_path_list()
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( String s : paths)
        {
            returned.add(Path.of(s));
        }
        return returned;
    }

    //**********************************************************
    @Override
    public Path resolve(String string)
    //**********************************************************
    {
        return null;
    }

    //**********************************************************
    @Override
    public List<File> only_files()
    //**********************************************************
    {
        List<File> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory()) continue;
            returned.add(new File(s));
        }
        return returned;
    }

    //**********************************************************
    private void save()
    //**********************************************************
    {
        try {

            Files.write(the_playlist_file_path,paths,java.nio.charset.StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        }
        catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
    }


    //**********************************************************
    @Override
    public Move_provider get_move_provider()
    //**********************************************************
    {
        return (owner, x, y, destination_dir, destination_is_trash, the_list, safeMoveFilesOrDirs, logger) ->
        {
            for ( File f : the_list)
            {
                String s = f.getAbsolutePath();
                if ( paths.contains(s)) continue;
                paths.add(s);
            }
            save();
            report_change();
        };
    }


    //**********************************************************
    private void report_change()
    //**********************************************************
    {
        List<Old_and_new_Path> l = new ArrayList<>();
        Old_and_new_Path oanp = new Old_and_new_Path(
                the_playlist_file_path,
                the_playlist_file_path,
                Command_old_and_new_Path.command_edit,
                Status_old_and_new_Path.edition_done,
                false);
        l.add(oanp);
        Change_gang.report_changes(l);
    }

    //**********************************************************
    @Override
    public void delete(Path path, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        paths.remove(path.toAbsolutePath().toString());
        save();
        report_change();
    }

    //**********************************************************
    @Override
    public void delete_multiple(List<Path> paths, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        for ( Path p : paths)
        {
            paths.remove(p.toAbsolutePath().toString());
        }
        save();
        report_change();
    }

    //**********************************************************
    @Override
    public void reload()
    //**********************************************************
    {
        try {
            List<String> ss = Files.readAllLines(the_playlist_file_path);
            for ( String s : ss)
            {
                paths.add(s);
            }
        }
        catch (NoSuchFileException e)
        {
            logger.log("No such file: "+ the_playlist_file_path);
        }
        catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
    }
}
