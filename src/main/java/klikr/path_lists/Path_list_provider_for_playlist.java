// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.path_lists;

import javafx.stage.Window;
import klikr.browser_core.virtual_landscape.Image_found;
import klikr.settings.String_constants;
import klikr.settings.boolean_features.Booleans;
import klikr.util.External_application;
import klikr.util.execute.Execute_command;
import klikr.util.execute.Execute_result;
import klikr.util.execute.actor.Aborter;
import klikr.change.Change_gang;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.files_and_paths.Extensions;
import klikr.util.files_and_paths.Filename_sanitizer;
import klikr.util.files_and_paths.Moving_files;
import klikr.change.old_and_new.Command;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.change.old_and_new.Status;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    public static final boolean dbg = false;
    public static final String KLIKR_IMAGE_PLAYLIST_EXTENSION = "klikr_image_playlist";

    public final Path the_playlist_file_path;

    public final Logger logger;
    private final Window owner;
    private final Change_broadcaster change_broadcaster;

    // cached:
    private final String key;
    // we need a list to keep the same order when we rename
    public final List<String> paths = new ArrayList<>();

    //**********************************************************
    public Path_list_provider_for_playlist(
            Path the_playlist_file_path,
            Window owner,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        change_broadcaster = new Change_broadcaster(logger);
        this.owner = owner;
        this.the_playlist_file_path = the_playlist_file_path;
        this.key = the_playlist_file_path.toAbsolutePath().normalize().toString();
        reload("constructor",aborter);
    }


    @Override
    public void set_cache_creation_time(long cache_creation_time) {

    }

    @Override
    public Path get_cache_save_path() {
        return null;
    }

    @Override
    public boolean is_rescan_needed() {
        return true;
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "Path_list_provider_for_playlist "+the_playlist_file_path;
    }

    //**********************************************************
    @Override
    public Path get_folder_path()
    //**********************************************************
    {
        // does not have a meaning for a playlist
        return null;
    }


    //**********************************************************
    @Override
    public String get_key()
    //**********************************************************
    {
        return key;
    }

    //**********************************************************
    @Override
    public int how_many_files_and_folders(boolean force_rescan, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {
        int returned = 0;
        for ( String s : paths)
        {
            if ( aborter.should_abort()) return 0;
            if ( (new File(s)).isDirectory())
            {
                if (! consider_also_hidden_folders)
                {
                    if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
                    returned++;
                    continue;
                }
            }
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
            }
            returned++;
        }
        return returned;

    }


    //**********************************************************
    @Override
    public List<Path> only_file_paths(boolean force_rescan, boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory()) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
            }
            returned.add(Path.of(s));
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<Path> only_song_paths(boolean force_rescan, boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory()) continue;
            if( !Guess_file_type.is_this_path_extension_a_music(Path.of(s),logger)) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
            }
            returned.add(Path.of(s));
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<Path> only_image_paths(boolean force_rescan, boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory()) continue;
            if( !Guess_file_type.is_this_path_extension_an_image(Path.of(s),owner,logger)) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
            }
            returned.add(Path.of(s));
        }
        return returned;
    }



    //**********************************************************
    @Override
    public List<Path> only_folder_paths(boolean force_rescan, boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if ( ! (new File(s)).isDirectory()) continue;
            if (! consider_also_hidden_folders)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
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
            List<String> lines = Files.readAllLines(the_playlist_file_path, StandardCharsets.UTF_8);
            if ( dbg)
            {
                logger.log("####### Playlist AFTER SAVE:");
                for (String s : lines) {
                    logger.log(s);
                }
                logger.log("##########################");
            }
        }
        catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }

    }

    //**********************************************************
    public Move_provider get_move_provider_for_file_system()
    //**********************************************************
    {
        // not a file system thing
        return null;
    }

    //**********************************************************
    public Move_provider get_move_in_provider()
    //**********************************************************
    {
        Move_provider move_provider = new Move_provider() {
            @Override
            public void move(Path destination, boolean destination_is_trash, List<File> the_list, Window owner, double x, double y, Aborter aborter, Logger logger) {

                logger.log("Entering move() for Path_list_provider_for_playlist "+the_list.size());
                List<String> the_list2 = new ArrayList<>();
                for (File f : the_list) {
                    the_list2.add(f.getAbsolutePath());
                }
                user_wants_to_add_items(the_list2, aborter);

                save();
                report_change(owner);

            }
        };

        return move_provider;
    }

    //**********************************************************
    public void user_wants_to_add_items(
            List<String> the_list_of_new_items,
            Aborter aborter)
    //**********************************************************
    {
        long start = System.currentTimeMillis();
        Runnable r = () ->
        {
            List<Old_and_new_Path> to_be_renamed_first = new ArrayList<>();
            List<String> oks = new ArrayList<>();
            for (String path_s : the_list_of_new_items)
            {
                logger.log(" looking at "+path_s);
                if ( aborter.should_abort())
                {
                    logger.log(" ABORTING "+aborter.reason());
                    return;
                }
                File f = new File(path_s);
                if (f.isDirectory())
                {
                    logger.log(f+" is a directory");
                    load_folder(f, oks,to_be_renamed_first, aborter);
                }
                else
                {
                    sanitize(path_s,  oks,to_be_renamed_first,logger);
                }
            }
            Moving_files.actual_safe_moves(to_be_renamed_first, true,  owner.getX()+100, owner.getY()+ 100, owner, new Aborter("dummy",logger), logger);
            logger.log(to_be_renamed_first.size()+ " files RENAMED to be accepted as possible songs");

            String last = null;
            List<String> final_dest = new ArrayList<>();
            for ( Old_and_new_Path o : to_be_renamed_first)
            {
                if ( !paths.contains(o.new_Path.toAbsolutePath().toString()))
                {
                    final_dest.add(o.new_Path.toAbsolutePath().toString());
                }
                else
                {
                    logger.log(o.new_Path.toAbsolutePath().toString()+" not added = already there!");
                }
                last = o.new_Path.toAbsolutePath().toString();
            }
            for ( String f : oks)
            {
                if ( !paths.contains(f))
                {
                    final_dest.add(f);
                    last = f;
                }
            }
            logger.log(final_dest.size()+ " files accepted as possible songs");
            paths.addAll(final_dest);
            if ( last != null)
            {
                save();
                //change_song(last, start,true);
            }
            //update_playlist_size_info();
        };
        Actor_engine.execute(r, "Adding multiple songs to playlist",logger);

    }

    //**********************************************************
    private void load_folder(File folder, List<String> oks, List<Old_and_new_Path> out, Aborter aborter)
    //**********************************************************
    {
        logger.log("Entering load_folder() for Path_list_provider_for_playlist");
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File ff : files)
        {
            if ( aborter.should_abort()) return;

            if (ff.isDirectory())
            {
                load_folder(ff, oks, out, aborter);
            }
            else
            {
                sanitize(ff.getAbsolutePath(), oks, out,logger);
            }
        }
    }

    //**********************************************************
    static void sanitize(String song, List<String> oks, List<Old_and_new_Path> out, Logger logger)
    //**********************************************************
    {
        if (!Guess_file_type.is_this_extension_an_audio(Extensions.get_extension((new File(song)).getName())))
        {
            if ( dbg) logger.log("❗ Rejected as a possible song due to extension: "+(new File(song)).getName());
            return;
        }
        String parent = (new File(song)).getParent();
        String file_name = (new File(song)).getName();
        String new_name = Extensions.get_base_name(file_name);

        new_name = Filename_sanitizer.sanitize(new_name,logger);

        new_name = Extensions.add(new_name,Extensions.get_extension(file_name));

        if (new_name.equals(file_name))
        {
            oks.add(song);
            return;
        }

        out.add(new Old_and_new_Path(Path.of(song), Path.of(parent, new_name), Command.command_rename, Status.before_command,false));

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
        //dump("paths before delete");
        paths.remove(path.toAbsolutePath().toString());
        //dump("paths after delete");
        save();
        //dump("paths after save");
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
    public void reload(String origin, Aborter aborter)
    //**********************************************************
    {
        logger.log("Path_list_provider_for_playlist.reload(), reason ="+origin);
        if ( the_playlist_file_path == null)
        {
            logger.log("❌ FATAL ERROR: the_playlist_file_path is null!");
            return;
        }
        try {
            List<String> ss = Files.readAllLines(the_playlist_file_path,StandardCharsets.UTF_8);
            for ( String s : ss)
            {
                if ( aborter.should_abort()) return;
                if ( !paths.contains(s))
                {
                    //logger.log("Path_list_provider_for_playlist.reload(): adding "+s);
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
        change_broadcaster.call_all_change_subscribers();
    }

    @Override
    public Change_broadcaster get_change_broadcaster() {
        return change_broadcaster;
    }

    //**********************************************************
    @Override
    public Files_and_folders files_and_folders(boolean force_rescan, Image_found imgfnd, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {

        List<Path> files = new ArrayList<>();
        List<Path> folders = new ArrayList<>();
        Files_and_folders returned = new Files_and_folders(files,folders);
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory())
            {
                if (! consider_also_hidden_folders)
                {
                    if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
                }
                folders.add(Path.of(s));
            }
            else
            {
                if (! consider_also_hidden_files)
                {
                    if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
                }
                files.add(Path.of(s));
            }
        }
        return returned;
    }

    // **********************************************************
    public void import_from_youtube(String youtube_url)
    // **********************************************************
    {
        // yt-dlp -x --audio-format aac --audio-quality 0 https://youtu.be/3DB-uJ0TxKQ

        logger.log("going to extract audio tracks from URl:" + youtube_url);

        List<String> command_line_for_ytdlp = new ArrayList<>();
        command_line_for_ytdlp.add(External_application.Ytdlp.get_command(owner,logger));
        command_line_for_ytdlp.add("-4");
        command_line_for_ytdlp.add("-x");
        command_line_for_ytdlp.add("--audio-format");
        command_line_for_ytdlp.add("aac");
        command_line_for_ytdlp.add("--audio-quality");
        command_line_for_ytdlp.add("0");
        command_line_for_ytdlp.add(youtube_url);

        StringBuilder sb = new StringBuilder();
        String home = System.getProperty(String_constants.USER_HOME);
        Execute_result res = Execute_command.execute_command_list(command_line_for_ytdlp, new File(home), 20 * 1000, sb,
                logger);
        if (!res.status()) {
            List<String> verif = new ArrayList<>();
            verif.add(External_application.Ytdlp.get_command(owner,logger));
            verif.add("--version");
            Execute_result res2 = Execute_command.execute_command_list(verif, new File(home), 20 * 1000, null, logger);
            if (!res2.status()) {
                Booleans.manage_show_ytdlp_install_warning(owner, logger);
            }
            return;
        }
        logger.log(sb.toString());

        List<String> returned = new ArrayList<>();
        String detector = "[ExtractAudio] Destination:";
        for (String l : sb.toString().split("\n"))
        {
            if (l.startsWith(detector)) {
                String path = (home + File.separator + l.substring(detector.length()).trim());
                paths.add(path);
            }
        }
        save();
    }

    // **********************************************************
    public void swap(String old_path, String new_path)
    // **********************************************************
    {
        logger.log("Path_list_provider_for_playlist swapping old path: " + old_path + " new path: " + new_path);
        int i = paths.indexOf(old_path);
        if ( i == -1 )
        {
            paths.add(new_path);
        }
        else
        {
            paths.set(i, new_path);
        }
        save();
    }
}
