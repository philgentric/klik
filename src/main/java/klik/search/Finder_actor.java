package klik.search;

import klik.actor.Actor;
import klik.actor.Message;
import klik.util.files_and_paths.Ding;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

//**********************************************************
public class Finder_actor implements Actor
//**********************************************************
{

    static final boolean dbg = false;
    private static final boolean ultra_dbg = false;
    private int visited_folders;
    private int visited_files;
    Map<String,Integer> matched_keyword_counts = new HashMap<>();
    Logger logger;
    long start;

    //**********************************************************
    public Finder_actor(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
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
        logger.log("Finder::search()");
        print_keywords(fm.search_config.keywords(),fm.extension);

        fm.callback.has_ended(find_similar_files(fm));
        return "search done";
    }



    //**********************************************************
    private Search_status find_similar_files(Finder_message fm)
    //**********************************************************
    {
        //logger.log("find_similar_files()");
        Path dir = fm.search_config.path();
        if ( !Files.isDirectory(fm.search_config.path()))
        {
            dir = fm.search_config.path().getParent();
        }
        start = System.currentTimeMillis();
        return extract_dir( dir, fm);

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

        if ( dbg) logger.log("Now looking into dir:"+dir.toAbsolutePath());
        visited_folders++;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))
        {
            for (Path path : stream)
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
                        if ( dbg) logger.log("NOT following symbolic link:"+path);
                    }
                    else
                    {
                        if ( dbg) logger.log("going down? trying folder:"+path);
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
                    if (!fm.search_config.search_files())
                    {
                        // we are nio interested in files
                        continue;
                    }

                    visited_files++;
                    if ( fm.search_config.look_only_for_images())
                    {
                        if (Guess_file_type.is_this_path_an_image(path))
                        {
                            check_if_name_matches_keywords(path, fm);
                        }
                    }
                    else
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

            }
        } catch (IOException e)
        {
            if (dbg) logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
            else logger.log("Finder_actor:extract_dir "+e.toString());
            if ( fm.callback != null)
            {
                fm.callback.on_the_fly_stats(null,new Search_statistics(visited_folders, visited_files,matched_keyword_counts));
            }
        }
        return Search_status.done;
    }

    //**********************************************************
    private void check_if_name_matches_keywords(Path target_path, Finder_message fm)
    //**********************************************************
    {
        if ( fm.search_config.keywords().isEmpty())
        {
            search_with_extension(target_path, fm);
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
                if (!Guess_file_type.is_this_path_an_image(target_path))
                {
                    return;
                }
            }
            if (fm.extension != null)
            {
                if (!fm.extension.isBlank())
                {
                    String ext = Static_files_and_paths_utilities.get_extension(target_path.getFileName().toString()).toLowerCase();
                    if (ext.equals(fm.extension))
                    {
                        count_keyword(ext);
                        all_matched_keywords.add(ext);
                    }
                    else
                    {
                        if (ultra_dbg) logger.log("extensions dont match" + ext + " vs " + fm.extension);
                        return;
                    }
                }
            }
            name = Static_files_and_paths_utilities.get_base_name(target_path.getFileName().toString());
            if ( !fm.search_config.check_case())
            {
                name = name.toLowerCase();
            }
        }


        if ( ultra_dbg) logger.log(target_path.toAbsolutePath()+" checking if all keywords are present for: "+name);
        // look for ALL of them
        for ( String keyword : fm.search_config.keywords())
        {
            String kk = keyword;
            if (!fm.search_config.check_case()) kk = keyword.toLowerCase();
            if ( ultra_dbg) logger.log(target_path.toAbsolutePath()+" checking if all keywords are present for: "+name+" keyword="+kk);
            if ( !name.contains(kk) )
            {
                // if one keyword is missing we give up
                break;
            }
            count_keyword(keyword);
            all_matched_keywords.add(keyword);
        }

        if ( fm.aborter.should_abort()) return;

        if ( all_matched_keywords.isEmpty())
        {
            // second chance: trying matching only some keywords
            if (ultra_dbg) logger.log("checking if a few keywords are present for: " + name);
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
            }
            if ( !shorter_keyword_list.isEmpty())
            {
                record_found(target_path, shorter_keyword_list, fm);
            }
        }
        else
        {
            if ( dbg) logger.log("all keywords "+all_matched_keywords+" found for "+target_path.getFileName());
            record_found(target_path, all_matched_keywords, fm);
        }
    }

    //**********************************************************
    private void search_with_extension(Path target_path, Finder_message fm)
    //**********************************************************
    {
        if ( fm.search_config.extension()!=null)
        {
            // no keywords but an extension
            String ext = Static_files_and_paths_utilities.get_extension(target_path.getFileName().toString()).toLowerCase();
            //logger.log("ext="+ext+" vs "+fm.search_config.extension());
            if(ext.equals(fm.search_config.extension()))
            {
                List<String> empty_keyword_list = new ArrayList<>();
                empty_keyword_list.add(ext);
                record_found(target_path, empty_keyword_list, fm);
            }
        }
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
    private void record_found(Path path, List<String> matched_keywords, Finder_message fm)
    //**********************************************************
    {
        if ( ultra_dbg) logger.log("Matching item found: "+path.toAbsolutePath());
        if ( fm.callback != null)
        {
            fm.callback.on_the_fly_stats(new Search_result(path,matched_keywords),new Search_statistics(visited_folders, visited_files,matched_keyword_counts));
        }
    }


    //**********************************************************
    private void print_keywords(List<String> keywords, String extension)
    //**********************************************************
    {
        logger.log("---Finder keywords------");
        logger.log("Extension="+extension);
        for( String s: keywords)
        {
            logger.log("->"+s+"<-");
        }
        logger.log("------------------------");
    }

/*
    //**********************************************************
    public void find_files(Path target_path, Browser browser)
    //**********************************************************
    {
        List<String> exclusion_list = load_keyword_exclusion_list(logger);
        Keyword_extractor ke = new Keyword_extractor(logger, exclusion_list);
        Set<String> keywords_set = ke.extract_keywords_from_file_and_dir_names(target_path);
        if (keywords_set == null) {
            logger.log("FATAL null keywords ??? ");
            return;
        }
        if (keywords_set.isEmpty()) {
            logger.log("FATAL no keywords ??? ");
            return;
        }
        List<String> keywords = new ArrayList<>();
        for (String k : keywords_set) {
            keywords.add(k.toLowerCase());
        }

        logger.log("---- looking at keywords -------");
        for (String s : keywords) {
            logger.log("->" + s + "<-");
        }
        logger.log("--------------------------------");

        find_files_from_keywords( target_path,  browser,  keywords);

    }


    //**********************************************************
    private static List<String> load_keyword_exclusion_list(Logger logger)
    //**********************************************************
    {

        List<String> returned = new ArrayList<>();
        int max = Static_application_properties.get_excluded_keyword_list_max_size(logger);
        for (int i = 0; i < max; i++) {
            String key = Static_application_properties.EXCLUDED_KEYWORD_PREFIX + i;
            String kw = Static_application_properties.get_properties_manager(logger).get(key);
            if (kw != null) {
                String lower = kw.toLowerCase();
                returned.add(lower);
                logger.log("excluded key word: ->" + lower + "<-");
            }
        }
        return returned;
    }

    //**********************************************************
    private void process_name_selected(Path path, List<String> matched_keywords,Finder_message fm)
    //**********************************************************
    {
        if ( Files.isDirectory(path))
        {
            if ( dbg) logger.log("keywords  ->"+matched_keywords+"<- matched for DIR "+path.toAbsolutePath());
            record_found(path, matched_keywords,fm);
        }
        else
        {
            if(fm.look_only_for_images)
            {
                if ( !Guess_file_type.is_this_path_an_image(path))
                {
                    if ( dbg) logger.log("is not an image: "+path.toAbsolutePath());
                    return;
                }
            }
            if ( dbg) logger.log("keywords ->"+matched_keywords+"<- matched for file: "+path.toAbsolutePath());
            record_found(path, matched_keywords, fm);
        }
    }
*/

}
