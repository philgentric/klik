// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.path_lists;

import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.browser.Move_provider;
import klik.change.Change_gang;
import klik.util.files_and_paths.old_and_new.Command;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.files_and_paths.old_and_new.Status;
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
public class Path_list_provider_for_playlist implements Path_list_provider
//**********************************************************
{
    public static final String KLIK_IMAGE_PLAYLIST_EXTENSION = "klik_image_playlist";

    public final Path the_playlist_file_path;
    public final List<String> paths = new ArrayList<>();
    public final Logger logger;
    private final Window owner;

    Change change = new Change();

    //**********************************************************
    public Path_list_provider_for_playlist(Path the_playlist_file_path, Window owner, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.owner = owner;
        this.the_playlist_file_path = the_playlist_file_path;
        if ( the_playlist_file_path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌ FATAL ERROR: the_playlist_file_path is null!"));
            return;
        }
        reload();
    }

    //**********************************************************
    @Override
    public Path get_folder_path()
    //**********************************************************
    {
        return the_playlist_file_path;
    }


    //**********************************************************
    @Override
    public String get_name()
    //**********************************************************
    {
        return the_playlist_file_path.toAbsolutePath().toString();
    }
/*
    //**********************************************************
    @Override
    public List<Path> get_all()
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( String s : paths)
        {
            returned.add(Path.of(s));
        }
        return returned;
    }
*/
    //**********************************************************
    @Override
    public List<File> only_files(boolean consider_also_hidden_files)
    //**********************************************************
    {
        List<File> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory()) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),owner,logger)) continue;
            }
            returned.add(new File(s));
        }
        return returned;
    }
    //**********************************************************
    @Override
    public int how_many_files_and_folders(boolean consider_also_hidden_files, boolean consider_also_hidden_folders)
    //**********************************************************
    {
        int returned = 0;
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory())
            {
                if (! consider_also_hidden_folders)
                {
                    if ( Guess_file_type.should_ignore(Path.of(s),owner,logger)) continue;
                    returned++;
                    continue;
                }
            }
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),owner,logger)) continue;
            }
            returned++;
        }
        return returned;

    }


    //**********************************************************
    @Override
    public List<Path> only_file_paths(boolean consider_also_hidden_files)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory()) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),owner,logger)) continue;
            }
            returned.add(Path.of(s));
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<Path> only_song_paths(boolean consider_also_hidden_files)
    //**********************************************************
    {
        return new ArrayList<>();
    }

    //**********************************************************
    @Override
    public List<Path> only_image_paths(boolean consider_also_hidden_files)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory()) continue;
            if( !Guess_file_type.is_this_path_an_image(Path.of(s),owner,logger)) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),owner,logger)) continue;
            }
            returned.add(Path.of(s));
        }
        return returned;    }


    //**********************************************************
    @Override
    public List<File> only_folders(boolean consider_also_hidden_folders)
    //**********************************************************
    {
        List<File> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if (! (new File(s)).isDirectory()) continue;
            if (! consider_also_hidden_folders)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),owner,logger)) continue;
            }
            returned.add(new File(s));
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<Path> only_folder_paths(boolean consider_also_hidden_folders)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if ( ! (new File(s)).isDirectory()) continue;
            if (! consider_also_hidden_folders)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),owner,logger)) continue;
            }
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
    private void save()
    //**********************************************************
    {
        try {
            Files.delete(the_playlist_file_path);
            Files.write(the_playlist_file_path,paths,java.nio.charset.StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            List<String> lines = Files.readAllLines(the_playlist_file_path);
            for ( String s : lines)
            {
                logger.log("AFTER SAVE FILE IS: Path_list_provider_for_playlist.save(): " +s);
            }
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
        return (destination_dir, destination_is_trash, the_list,owner, x, y,  aborter, logger) ->
        {
            for ( File f : the_list)
            {
                String s = f.getAbsolutePath();
                if ( paths.contains(s)) continue;
                logger.log("Path_list_provider_for_playlist.get_move_provider(): adding "+s);
                paths.add(s);
            }
            save();
            report_change(owner);
        };
    }


    //**********************************************************
    private void report_change(Window owner)
    //**********************************************************
    {
        List<Old_and_new_Path> l = new ArrayList<>();
        Old_and_new_Path oanp = new Old_and_new_Path(
                the_playlist_file_path,
                the_playlist_file_path,
                Command.command_edit,
                Status.edition_done,
                false);
        l.add(oanp);
        Change_gang.report_changes(l,owner);
    }

    //**********************************************************
    @Override
    public void delete(Path path, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        logger.log("Path_list_provider_for_playlist.delete(): "+path.toAbsolutePath().toString());
        dump("paths before delete");
        paths.remove(path.toAbsolutePath().toString());
        dump("paths after delete");
        save();
        dump("paths after save");
        report_change(owner);
    }

    //**********************************************************
    private void dump(String msg)
    //**********************************************************
    {
        logger.log("===== Path_list_provider_for_playlist.paths: "+msg+" =====");
        for ( String s : paths)
        {
            logger.log("   "+s);
        }
        logger.log("=========================================");
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
        report_change(owner);
    }

    //**********************************************************
    @Override
    public void reload()
    //**********************************************************
    {
        if ( the_playlist_file_path == null)
        {
            logger.log("❌ FATAL ERROR: the_playlist_file_path is null!");
            return;
        }
        try {
            List<String> ss = Files.readAllLines(the_playlist_file_path);
            for ( String s : ss)
            {
                if ( !paths.contains(s))
                {
                    logger.log("Path_list_provider_for_playlist.reload(): adding "+s);
                    paths.add(s);
                }
            }
        }
        catch (NoSuchFileException e)
        {
            logger.log("No such file: "+ the_playlist_file_path);
        }
        catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        change.call_change_listeners();
    }

    @Override
    public Change get_Change() {
        return change;
    }

    @Override
    public Files_and_folders files_and_folders(boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter) {

        List<Path> files = new ArrayList<>();
        List<Path> folders = new ArrayList<>();
        Files_and_folders returned = new Files_and_folders(files,folders);
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory())
            {
                if (! consider_also_hidden_folders)
                {
                    if ( Guess_file_type.should_ignore(Path.of(s),owner,logger)) continue;
                }
                folders.add(Path.of(s));
            }
            else
            {
                if (! consider_also_hidden_files)
                {
                    if ( Guess_file_type.should_ignore(Path.of(s),owner,logger)) continue;
                }
                files.add(Path.of(s));
            }
        }
        return returned;
    }
}
