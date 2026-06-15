// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.search;

import javafx.stage.Window;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.path_lists.Path_list_provider_for_playlist;
import klikr.path_lists.Path_list_provider_for_search_results;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Message;
import klikr.util.files_and_paths.Ding;
import klikr.util.files_and_paths.Extensions;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.log.Logger;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class Finder_actor implements Actor
//**********************************************************
{

    static final boolean dbg = true;
    private static final boolean ultra_dbg = true;
    private int visited_folders;
    private int visited_files;
    Map<String,Integer> matched_keyword_counts = new HashMap<>();
    private final Logger logger;
    private final Window owner;
    long start;

    //**********************************************************
    public Finder_actor(Window owner, Logger logger)
    //**********************************************************
    {
        this.owner = owner;
        this.logger = logger;
    }



    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Finder_actor";
    }


    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Finder_message fm = (Finder_message) m;
        if ( fm.search_config.keywords().isEmpty())
        {
            if (fm.search_config.extension() == null)
            {
                logger.log("no keywords ? aborting search");
                fm.callback.has_ended(Search_status.no_keywords);
                Ding.play("Aborting file search: no keywords",logger);
                return "no keywords ? aborting search";
            }
        }
        visited_files = 0;
        visited_folders =0;
        logger.log("Finder::search() in folder: "+fm.search_config.path_list_provider().get_key());
        print_keywords(fm.search_config.keywords());

        if( fm.search_config.path_list_provider() instanceof Path_list_provider_for_file_system)
        {
            fm.callback.has_ended(find_similar_files(fm));
        }
        else if( fm.search_config.path_list_provider() instanceof Path_list_provider_for_playlist)
        {
            fm.callback.has_ended(find_similar_songs(fm));
        }
        else if( fm.search_config.path_list_provider() instanceof Path_list_provider_for_search_results)
        {
            fm.callback.has_ended(find_similar_search_results(fm));
        }

        return "search done";
    }



    //**********************************************************
    private Search_status find_similar_files(Finder_message fm)
    //**********************************************************
    {
        //logger.log("find_similar_files()");
        Optional<Path> p = fm.search_config.path_list_provider().get_folder_path();
        if ( p.isEmpty())
        {
            logger.log("no folder path found");
            return Search_status.invalid;
        }
        Path dir = p.get();
        if ( !Files.isDirectory(dir))
        {
            dir = dir.getParent();
        }
        start = System.currentTimeMillis();
        return extract_dir( dir, fm);

    }

    //**********************************************************
    private Search_status find_similar_search_results(Finder_message fm)
    //**********************************************************
    {
        //logger.log("find_similar_files()");
        List<Path> all_paths = fm.search_config.path_list_provider().only_file_paths(true, Feature_cache.get(Feature.Show_hidden_files),fm.aborter);
        if ( all_paths.isEmpty())
        {
            logger.log("no files ?");
            return Search_status.invalid;
        }

        start = System.currentTimeMillis();

        Search_status search_status = Search_status.searching;
        for( Path p : all_paths)
        {
            search_status = scan_path(fm, p);
            if ( search_status == Search_status.interrupted) break;
        }
        return search_status;
    }



    //**********************************************************
    private Search_status find_similar_songs(Finder_message fm)
    //**********************************************************
    {
        start = System.currentTimeMillis();
        if ( ultra_dbg) logger.log("finder find_similar_strings");
        if ( fm.aborter.should_abort() )
        {
            if ( ultra_dbg) logger.log("finder abort");
            return Search_status.interrupted;
        }

        List<Path> paths = fm.search_config.path_list_provider().only_song_paths(true,false,fm.aborter);
        for ( Path path : paths)
        {
            //logger.log("looking at:"+f.getAbsolutePath());
            if ( fm.aborter.should_abort() )
            {
                if ( ultra_dbg) logger.log("finder abort");
                return Search_status.interrupted;
            }
            check_if_name_matches_keywords(path, fm);
            long now = System.currentTimeMillis();
            if ( (now-start) > 300)
            {
                if ( fm.callback != null)
                {
                    fm.callback.on_the_fly_stats(null,new Search_statistics(visited_folders, visited_files,matched_keyword_counts));
                    start = now;
                }
            }
        }

        return Search_status.done;
    }



    //**********************************************************
    private Search_status extract_dir(Path dir, Finder_message fm)
    //**********************************************************
    {
        if ( ultra_dbg) logger.log("finder extract_dir");
        if ( fm.aborter.should_abort() )
        {
            if ( ultra_dbg) logger.log("finder abort");
            return Search_status.interrupted;
        }
        if ( !Files.isDirectory(dir) )
        {
            return Search_status.invalid;
        }
        if ( fm.search_config.ignore_hidden())
        {
            if( Guess_file_type.should_ignore(dir,logger))
            {
                //if ( dbg)
                    logger.log("ignoring hidden folder:"+dir.toAbsolutePath());
                return Search_status.done;
            }

        }

        if ( dbg) logger.log("Now looking into dir:"+dir.toAbsolutePath());
        visited_folders++;
        {
            File files[] = dir.toFile().listFiles();
            if ( files != null)
            {
                for ( File f : files)
                {
                    //logger.log("looking at:"+f.getAbsolutePath());
                    Path path = f.toPath();
                    Search_status interrupted = scan_path(fm, path);
                    if (interrupted != null) return interrupted;

                }
            }
        }

        return Search_status.done;
    }

    //**********************************************************
    private @Nullable Search_status scan_path(Finder_message fm, Path path)
    //**********************************************************
    {
        if ( fm.aborter.should_abort() )
        {
            if ( ultra_dbg) logger.log("finder abort");
            return Search_status.interrupted;
        }
        if ( Files.isDirectory(path))
        {
            visited_folders++;
            if (Files.isSymbolicLink(path))
            {
                if ( dbg) logger.log("NOT following symbolic link:"+ path);
            }
            else
            {
                if ( dbg) logger.log("going down? trying folder:"+ path);
                switch(extract_dir(path, fm))
                {
                    case interrupted:
                        return Search_status.interrupted;
                    case invalid:
                        break;
                    case done:
                        break;
                    case searching:
                        break;
                    case ready:
                        break;
                    case undefined:
                        break;
                }
            }
            if (fm.search_config.search_folders())
            {
                check_if_name_matches_keywords(path, fm);
            }
        }
        else
        {
            visited_files++;
            //logger.log("looking at file:"+path.toAbsolutePath());
            boolean do_this_file = true;
            if ( fm.search_config.ignore_hidden())
            {
                if( Guess_file_type.should_ignore(path,logger))
                {
                    if ( dbg) logger.log("ignoring hidden file:"+ path.toAbsolutePath());
                    do_this_file = false;
                }

            }
            if (!fm.search_config.search_files())
            {
                if ( dbg) logger.log("ignoring files");

                // we are not interested in files
                do_this_file = false;
            }
            if (fm.search_config.look_only_for_images())
            {
                if (!Guess_file_type.is_this_path_extension_an_image(path, owner, logger))
                {
                    do_this_file = false;
                }
            }
            if ( do_this_file)
            {
                check_if_name_matches_keywords(path, fm);
            }
        }
        long now = System.currentTimeMillis();
        if ( (now-start) > 300)
        {
            if ( fm.callback != null)
            {
                fm.callback.on_the_fly_stats(null,new Search_statistics(visited_folders, visited_files,matched_keyword_counts));
                start = now;
            }
        }
        return null;
    }

    //**********************************************************
    private void check_if_name_matches_keywords(Path target_path, Finder_message fm)
    //**********************************************************
    {
        //logger.log("checking "+target_path.toAbsolutePath());
        if ( fm.search_config.keywords().isEmpty())
        {
            search_with_extension_only(target_path, fm);
            return;
        }

        List<String> all_matched_keywords = new ArrayList<>();
        String name;
        if ( Files.isDirectory(target_path))
        {
            name = target_path.getFileName().toString();
            if ( !fm.search_config.check_case())
            {
                name = name.toLowerCase();
            }
        }
        else
        {
            // is a file
            if ( fm.search_config.look_only_for_images())
            {
                if (!Guess_file_type.is_this_path_extension_an_image(target_path,owner,logger))
                {
                    return;
                }
            }
            name = Extensions.get_base_name(target_path.getFileName().toString());
            if ( !fm.search_config.check_case())
            {
                name = name.toLowerCase();
            }
        }


        if ( ultra_dbg)
            logger.log(target_path.toAbsolutePath()+" checking if all keywords are present for: "+name);
        // look for ALL of them
        for ( String keyword : fm.search_config.keywords())
        {
            String kk = keyword;
            if (!fm.search_config.check_case()) kk = keyword.toLowerCase();
            if ( !name.contains(kk) )
            {
                // if one keyword is missing we give up
                if ( ultra_dbg)
                logger.log(target_path.toAbsolutePath()+" checking if all keywords are present for: "+name+" keyword="+kk+ " not found");
                break;
            }

            count_keyword(keyword);
            all_matched_keywords.add(keyword);
        }

        if ( fm.aborter.should_abort()) return;

        if ( all_matched_keywords.isEmpty())
        {
            // second chance: trying matching only some keywords
            if (ultra_dbg)
                logger.log("checking if a few keywords are present for: " + name);
            List<String> shorter_keyword_list = new ArrayList<>();
            for (String keyword : fm.search_config.keywords())
            {
                String k = keyword;
                if (!fm.search_config.check_case()) k = keyword.toLowerCase();
                if (name.contains(k))
                {
                    count_keyword(keyword);
                    shorter_keyword_list.add(keyword);
                }
                else
                {
                    //logger.log(k+ " not found in " + name);
                }
            }
            if ( !shorter_keyword_list.isEmpty())
            {
                record_found(target_path, shorter_keyword_list, fm);
            }
        }
        else
        {
            if ( dbg) logger.log("MATCHED keywords ->"+all_matched_keywords+"<- for ->"+target_path.getFileName()+"<-");
            record_found(target_path, all_matched_keywords, fm);
        }
    }

    //**********************************************************
    private void search_with_extension_only(Path target_path, Finder_message fm)
    //**********************************************************
    {
        if ( !extension_matches(target_path,fm)) return;

        List<String> keyword_list = new ArrayList<>();
        keyword_list.add(fm.search_config.extension());
        record_found(target_path, keyword_list, fm);
    }
    //**********************************************************
    private boolean extension_matches(Path target_path, Finder_message fm)
    //**********************************************************
    {
        if ( fm.search_config.extension()==null) return false;

        String ext = Extensions.get_extension(target_path.getFileName().toString()).toLowerCase();
        //logger.log("ext="+ext+" vs "+fm.search_config.extension());

        return ext.equals(fm.search_config.extension());
    }

    //**********************************************************
    private void count_keyword(String keyword)
    //**********************************************************
    {
        Integer previous = matched_keyword_counts.get(keyword);
        if ( previous == null) previous = Integer.valueOf(0);
        matched_keyword_counts.put(keyword,previous+1);
    }

    //**********************************************************
    private void record_found(Path target_path, List<String> matched_keywords, Finder_message fm)
    //**********************************************************
    {
        // check if extension ALSO matches, if required
        if (fm.extension != null)
        {
            if (!fm.extension.isBlank())
            {
                String ext = Extensions.get_extension(target_path.getFileName().toString()).toLowerCase();
                if (ext.equals(fm.extension))
                {
                    count_keyword(ext);
                    matched_keywords.add(ext);
                }
                else
                {
                    if (ultra_dbg) logger.log("extensions dont match" + ext + " vs " + fm.extension);
                    return;
                }
            }
        }


        if ( ultra_dbg)
        {
            logger.log("Matching item found: "+target_path.toAbsolutePath());
            print_keywords(matched_keywords);
        }
        if ( fm.callback != null)
        {
            fm.callback.on_the_fly_stats(new Search_result(target_path,matched_keywords),new Search_statistics(visited_folders, visited_files,matched_keyword_counts));
        }
    }


    //**********************************************************
    private void print_keywords(List<String> keywords)
    //**********************************************************
    {
        logger.log("--- keywords found------");
        for( String s: keywords)
        {
            Integer count = matched_keyword_counts.get(s);
            if (count == null) count = Integer.valueOf(0);
            logger.log("->"+s+"<-"+count+" times");
        }
        logger.log("------------------------");
    }


}
