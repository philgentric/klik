// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.path_lists;

import javafx.stage.Window;
import klikr.browser_core.virtual_landscape.Image_found;
import klikr.util.cache.Cache_folder;
import klikr.util.execute.actor.Aborter;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.files_and_paths.Moving_files;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.perf.Perf;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//**********************************************************
public class Path_list_provider_for_file_system implements Path_list_provider
//**********************************************************
{
    private final static boolean cache_dbg = false;
    private final Path folder_path;
    private final String key;
    private final Logger logger;
    private final Window owner;
    private final Change_broadcaster change_broadcaster;
    long timestamp = -1;
    private Files_and_folders cached;
    private volatile long cache_creation_time;

    //**********************************************************
    public Path_list_provider_for_file_system(Path folder_path, Window owner, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        change_broadcaster = new Change_broadcaster(logger);
        this.owner = owner;
        this.folder_path = folder_path;
        if( folder_path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌❌❌ Panic : null folder_path"));
            this.key = null;
            return;
        }
        String home =  System.getProperty("user.home");
        String p = folder_path.toAbsolutePath().normalize().toString();
        if (p.equals(home))
        {
            this.key = p;
            return;
        }
        if( p.startsWith(home) ) {
            this.key = p.substring(home.length());
        }
        else {
            this.key = p;
        }
    }

    //**********************************************************
    @Override
    public void set_cache_creation_time(long cache_creation_time)     //**********************************************************
    //**********************************************************
    {
        this.cache_creation_time = cache_creation_time;
    }

    //**********************************************************
    @Override
    public Path get_cache_save_path()
    //**********************************************************
    {
        Path folder_cache_dir = Cache_folder.get_cache_dir( Cache_folder.folder_cache,owner,logger);
        if ( cache_dbg)
            logger.log("folder_cache_dir="+folder_cache_dir);
        String local = folder_path.toAbsolutePath().toString();
        local = local.replace(":","_");
        local = local.replace(File.separator,"_");
        //logger.log("local="+local);
        return Path.of(folder_cache_dir.toAbsolutePath().toString(),local+".cache");
    }


    //**********************************************************
    @Override
    public boolean is_rescan_needed()
    //**********************************************************
    {
        Optional<Path> path = get_folder_path();
        if (  path.isEmpty()) return  false;
        if ( timestamp < 0) return true;
        try {
            long as_of_now = Files.getLastModifiedTime( path.get()).toMillis();
            boolean returned = false;
            if ( as_of_now > timestamp) returned = true;
            timestamp = as_of_now;
            return  returned;
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return  true;
        }
    }

    //**********************************************************
    @Override
    public Optional<Path> get_folder_path()
    //**********************************************************
    {
        return Optional.of(folder_path);
    }


    //**********************************************************
    @Override
    public int how_many_files_and_folders(boolean force_rescan, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {
        Files_and_folders faf = get_faf(force_rescan,aborter);

        int returned = 0;
        for (Path file : faf.files())
        {
            if ( aborter.should_abort()) return 0;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file,logger)) continue;
            }
            returned++;
        }
        for (Path folder : faf.files())
        {
            if ( aborter.should_abort()) return 0;
            if (! consider_also_hidden_folders)
            {
                if ( Guess_file_type.should_ignore(folder,logger)) continue;
            }
            returned++;
        }
        return returned;
    }


    //**********************************************************
    @Override
    public Files_and_folders files_and_folders(boolean force, Image_found imgfnd, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {
        Files_and_folders faf = get_faf(force, aborter);

        // let us perform a real disk scan
        List<Path> file_paths = new ArrayList<>();
        List<Path> folder_paths = new ArrayList<>();
        for (Path file : faf.files())
        {
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file,logger)) continue;
                if ( Guess_file_type.is_this_path_extension_an_image(file,owner,logger))
                {
                    if ( imgfnd!=null) imgfnd.image_found();
                }
            }
            file_paths.add(file);
            if ( aborter.should_abort()) break;
        }
        for (Path folder : faf.folders())
        {
            if ( ! consider_also_hidden_folders)
            {
                if ( Guess_file_type.should_ignore(folder,logger)) continue;
            }
            folder_paths.add(folder);
            if ( aborter.should_abort()) break;
        }
        Files_and_folders returned = new Files_and_folders(file_paths,folder_paths);
        save_cache_to_disk(returned,aborter,logger);
        return returned;
    }

    //**********************************************************
    private Files_and_folders get_faf(boolean force, Aborter aborter)
    //**********************************************************
    {
        try (Perf p = new Perf("get Files_and_folders")) {
            if (cached == null) {
                cached = load_cache_from_disk(aborter, logger);
                // cached may be null
            }
            Files_and_folders faf;
            // is cache stale ?
            try {
                if (cache_creation_time > Files.getLastModifiedTime(folder_path).toMillis()) {
                    // cache was created AFTER the last change, so OK
                    faf = cached;
                } else {
                    // cache is stale !
                    faf = null;
                }
            } catch (IOException e) {
                logger.log(Stack_trace_getter.get_stack_trace("" + e));
                return new Files_and_folders(new ArrayList<>(), new ArrayList<>());
            }

            boolean rescan = false;
            if (force) rescan = true;
            if (faf == null)
            {
                rescan = true;
            }
            else
            {
                if ( faf.folders().isEmpty() && faf.files().isEmpty() )
                {
                    rescan = true;
                }
            }
            if (rescan) {
                faf = scan(aborter);
            }
            cached = faf;
            return faf;
        }
    }

    //**********************************************************
    private Files_and_folders scan(Aborter aborter)
    //**********************************************************
    {
        List<Path> file_paths = new ArrayList<>();
        List<Path> folder_paths = new ArrayList<>();
        File dir = folder_path.toFile();
        File[] files = dir.listFiles();
        if ( files == null) return new Files_and_folders(file_paths,folder_paths);
        for (File file : files)
        {
            if ( aborter.should_abort()) break;
            if ( file.isDirectory() )
            {
                folder_paths.add(file.toPath());
            }
            else
            {
                file_paths.add(file.toPath());
            }
        }
        Files_and_folders returned = new Files_and_folders(file_paths,folder_paths);
        save_cache_to_disk(returned,aborter,logger);
        return returned;
    }


    //**********************************************************
    @Override
    public List<Path> only_folder_paths(boolean force_rescan, boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {
        Files_and_folders faf = get_faf(force_rescan,aborter);
        List<Path> returned = new ArrayList<>();
        for (Path folder : faf.folders())
        {
            if (! consider_also_hidden_folders)
            {
                if ( Guess_file_type.should_ignore(folder,logger)) continue;
            }
            returned.add(folder);
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<Path> only_file_paths(boolean force_rescan, boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        Files_and_folders faf = get_faf(force_rescan, aborter);
        List<Path> returned = new ArrayList<>();
        for (Path file : faf.files())
        {
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file,logger)) continue;
            }
            returned.add(file);
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<Path> only_image_paths(boolean force_rescan, boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        Files_and_folders faf = get_faf(force_rescan, aborter);
        List<Path> returned = new ArrayList<>();
        for (Path file : faf.files())
        {
            if ( !Guess_file_type.is_this_path_extension_an_image(file,owner,logger)) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file,logger)) continue;
            }
            returned.add(file);
        }
        return returned;
    }


    //**********************************************************
    @Override
    public List<Path> only_song_paths(boolean force_rescan, boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        Files_and_folders faf = get_faf(force_rescan,aborter);
        List<Path> returned = new ArrayList<>();
        for (Path file : faf.files())
        {
            if ( !Guess_file_type.is_this_path_extension_a_music(file,logger)) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file,logger)) continue;
            }
            returned.add(file);
        }
        return returned;
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "Path_list_provider_for_file_system+ "+folder_path;
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
    public void reload(String origin, Aborter aborter)
    //**********************************************************
    {
        logger.log("Path_list_provider_for_file_system.reload(), reason ="+origin);

        cached = get_faf(true,aborter);
        // notify listeners
        change_broadcaster.call_all_change_subscribers();
    }

    //**********************************************************
    @Override
    public Path resolve(String string)
    //**********************************************************
    {
        if (folder_path == null) return  null;
        return folder_path.resolve(string);
    }

    //**********************************************************
    @Override
    public Change_broadcaster get_change_broadcaster()
    //**********************************************************
    {
        return change_broadcaster;
    }


    //**********************************************************
    @Override
    public Move_provider get_move_in_provider()
    //**********************************************************
    {
        return Moving_files::safe_move_files_or_dirs;
    }


    //**********************************************************
    @Override
    public void delete(Path path, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Static_files_and_paths_utilities.move_to_trash(path,owner,x,y, null, aborter, logger);
    }

    //**********************************************************
    @Override
    public void delete_multiple(List<Path> paths, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Static_files_and_paths_utilities.move_to_trash_multiple(paths,owner,x,y, null, aborter, logger);
    }


    // returns null if the cache does not exist or is stale
    //**********************************************************
    Files_and_folders load_cache_from_disk(Aborter aborter, Logger logger)
    //**********************************************************
    {
        Optional<Path> op = get_folder_path();
        if (  op.isEmpty())
        {
            logger.log(Stack_trace_getter.get_stack_trace("PANIC "));
            return null;
        }
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(get_cache_save_path());
        }
        catch (IOException e)
        {
            // happens the first time
            // logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return  null;
        }
        try {
            MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(bytes);
            String folder_path2 = unpacker.unpackString();
            if ( !folder_path.toAbsolutePath().toString().equals(folder_path2))
            {
                logger.log(Stack_trace_getter.get_stack_trace("PANIC different folder paths ?"));
                unpacker.close();
                return null;
            }
            long cache_creation_time = unpacker.unpackLong();

            long folder_modification_time = Files.getLastModifiedTime(folder_path).toMillis();
            if ( folder_modification_time > cache_creation_time)
            {
                logger.log("stale folder cache for "+get_folder_path());
                unpacker.close();
                return null;
            }
            set_cache_creation_time(cache_creation_time);
            List<Path> files;
            {
                int files_count = unpacker.unpackArrayHeader();
                files = new ArrayList<>(files_count);
                for (int i = 0; i < files_count; i++)
                {
                    if ( aborter.should_abort())
                    {
                        unpacker.close();
                        return null;
                    }
                    String local_path = unpacker.unpackString();
                    files.add(Path.of(local_path));
                }
            }
            List<Path> folders;
            {
                int folders_count = unpacker.unpackArrayHeader();
                folders = new ArrayList<>(folders_count);
                for (int i = 0; i < folders_count; i++)
                {
                    if ( aborter.should_abort())
                    {
                        unpacker.close();
                        return null;
                    }
                    String local_path = unpacker.unpackString();
                    folders.add(Path.of(local_path));
                }
            }
            unpacker.close();
            return new Files_and_folders(files,folders);
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return  null;
        }
    }

    //**********************************************************
    boolean save_cache_to_disk(Files_and_folders faf, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Optional<Path> folder_path = get_folder_path();
        if (  folder_path.isEmpty())
        {
            logger.log(Stack_trace_getter.get_stack_trace("PANIC "));
            return false;
        }
        try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
            packer.packString(folder_path.get().toAbsolutePath().toString());
            long now = System.currentTimeMillis();
            set_cache_creation_time(now);
            packer.packLong(now);

            {
                List<Path> files = faf.files();
                packer.packArrayHeader(files.size());
                for (Path entry : files)
                {
                    if ( aborter.should_abort())
                    {
                        packer.close();
                        return false;
                    }
                    packer.packString(entry.toAbsolutePath().toString());
                }
            }
            {
                List<Path> folders = faf.folders();
                packer.packArrayHeader(folders.size());
                for (Path entry : folders)
                {
                    if ( aborter.should_abort())
                    {
                        packer.close();
                        return false;
                    }
                    packer.packString(entry.toAbsolutePath().toString());
                }
            }
            Path cache_save_path = get_cache_save_path();
            if ( cache_dbg) logger.log("get_cache_save_path()="+cache_save_path);
            Files.write(cache_save_path, packer.toByteArray());
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return false;
        }
        return true;
    }

}
