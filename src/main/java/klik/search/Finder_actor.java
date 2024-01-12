package klik.search;

import klik.actor.Actor;
import klik.actor.Message;
import klik.browser.Browser;
import klik.files_and_paths.Files_and_Paths;
import klik.files_and_paths.Guess_file_type;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.Stack_trace_getter;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//**********************************************************
public class Finder_actor implements Actor
//**********************************************************
{

    public static final int MAX_SEARCH_TIME = 30_000;
    static final boolean dbg = false;
    private static final boolean ultra_dbg = false;
    private int visited_folders;
    private int visited_files;
    Logger logger;

    //**********************************************************
    public Finder_actor(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
    }

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
    public void find_files_from_keywords(Path target_path, Browser browser, List<String> keywords)
    //**********************************************************
    {
        Finder_frame popup = new Finder_frame(
                target_path,
                keywords,
                800,
                600,
                browser,
                this,
                logger);
        popup.start_search(target_path,browser,this);
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Finder_message fm = (Finder_message) m;
        if ( fm.keywords.isEmpty())
        {
            logger.log("no keywords ? aborting search");
            return "no keywords ? aborting search";
        }
        visited_files = 0;
        visited_folders =0;
        logger.log("Finder::search()");
        print_keywords(fm.keywords);

        String reason_to_stop = find_similar_files(fm);

        fm.callback.update_display_in_FX_thread(fm.the_browser, reason_to_stop);
        fm.callback.has_ended();
        return "search done";
    }



    //**********************************************************
    private String find_similar_files(Finder_message fm)
    //**********************************************************
    {
        logger.log("find_similar_files()");
        // first look into the directory of the current file
        Path dir = fm.path.getParent();
        extract_dir( dir, fm);


        boolean go_up = true;
        if (!go_up)
        {
            logger.log("not going up");
            return "hard coded limit = only current folder as search scope";
        }
        Path top = Paths.get(System.getProperty("user.home"));//(new File (System.getProperty("user.home"))).toPath();
        if (Files_and_Paths.is_same_path(dir,top,logger) )
        {
            return "oh oh ? aborted because this would search your whole user.home ???";
        }

        // then look directories up the hierarchy
        Path up = dir;
        long start = System.currentTimeMillis();
        for(;;)
        {
            up = up.getParent();
            if ( Files_and_Paths.is_same_path(up,top,logger) )
            {
                if ( dbg) logger.log("giving up at top");
                return "search reached top level = "+top.toAbsolutePath();
            }
            extract_dir( up, fm);
            if ( fm.aborter.should_abort() )
            {
                logger.log("finder shutdown");
                return "search was interrupted by user";
            }
            long now = System.currentTimeMillis();
            if ( now-start > MAX_SEARCH_TIME)
            {
                logger.log("finder shutdown after 30 seconds");
                return "search was interrupted because it reached the max search time: "+MAX_SEARCH_TIME;
            }
        }
    }


    //**********************************************************
    private void extract_dir(Path dir, Finder_message fm)
    //**********************************************************
    {
        if ( fm.aborter.should_abort() )
        {
            logger.log("finder shutdown");
            return;
        }
        if ( !Files.isDirectory(dir) ) return;

        if ( dbg)
            logger.log("Now looking into dir:"+dir.toAbsolutePath());

        visited_folders++;
        // we also look for directories
        check_if_file_name_matches_keywords(dir,fm);


        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))
        {
            for (Path entry : stream)
            {
                if ( fm.aborter.should_abort() )
                {
                    logger.log("finder cancelled");
                    return;
                }
                if ( Files.isDirectory(entry))
                {
                    if ( dbg) logger.log("diving DOWN for ALL keywords");
                    extract_dir(entry, fm);
                    continue;
                }
                check_if_file_name_matches_keywords(entry, fm);
            }
        } catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
    }

    //**********************************************************
    private void check_if_file_name_matches_keywords(Path target_file_path, Finder_message fm)
    //**********************************************************
    {
        visited_files++;
        String base_name = FilenameUtils.getBaseName(target_file_path.getFileName().toString());
        boolean is_a_string_of_only_numbers = false;
        for (char c : base_name.toCharArray()) {
            is_a_string_of_only_numbers = Character.isDigit(c);
            if (! is_a_string_of_only_numbers) break;
        }
        if ( is_a_string_of_only_numbers) return;

        if ( ultra_dbg) logger.log("check_if_file_name_matches_keywords() for "+base_name);
        if ( fm.keywords.contains("&"))
        {
            StringBuilder cumul = new StringBuilder();
            for ( String s : fm.keywords)
            {
                if ( s.equals("&")) continue;
                if ( !base_name.toLowerCase().contains(s.toLowerCase()))
                {
                    return;
                }
                if (cumul.length() == 0) cumul = new StringBuilder(s);
                else cumul.append(" & ").append(s);
            }
            process_name_selected(target_file_path, cumul.toString(), fm);
        }
        else
        {
            if ( ultra_dbg) logger.log("TRYING any keywords for "+base_name);
            for ( String s : fm.keywords)
            {
                if ( base_name.toLowerCase().contains(s.toLowerCase()))
                {
                    process_name_selected(target_file_path, s, fm);
                }
            }
            if (fm.keywords.size() <= 1) return;
            if ( ultra_dbg) logger.log("TRYING all keywords for "+base_name);
            // look for ALL of them
            StringBuilder all_s = new StringBuilder();
            for ( String s : fm.keywords)
            {
                if ( !base_name.toLowerCase().contains(s.toLowerCase()) ) return;
                all_s.append(s).append(" ");
            }
            // all selected !!
            if ( dbg) logger.log("all keywords "+all_s+" found for "+target_file_path.getFileName());
            process_name_selected(target_file_path, all_s.toString(), fm);
        }

    }

    //**********************************************************
    private void process_name_selected(Path path, String keyword,Finder_message fm)
    //**********************************************************
    {
        if ( Files.isDirectory(path))
        {
            if ( dbg)
                logger.log("keyword is ->"+keyword+"<- matched for DIR "+path.toAbsolutePath());
            record_found_dir(path, keyword,fm);
        }
        else
        {
            if(fm.look_only_for_images)
            {
                if ( !Guess_file_type.is_this_path_an_image (path))
                {
                    if ( dbg) logger.log("is not an image: "+path.toAbsolutePath());
                    return;
                }
            }
            if ( dbg) logger.log("keyword is ->"+keyword+"<- matched for IMAGE "+path.toAbsolutePath());
            record_found_file(path, keyword, fm);
        }
    }

    //**********************************************************
    private void record_found_dir(Path dir, String keyword, Finder_message fm)
    //**********************************************************
    {
        if ( fm.callback != null)
        {
            fm.callback.add_one_Search_result(new Search_result(null, null), new Search_statistics(visited_folders, visited_files));
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))
        {
            for (Path path : stream)
            {
                if ( fm.aborter.should_abort() )
                {
                    return;
                }
                if ( Files.isDirectory(path))
                {
                    record_found_dir(path, keyword,fm);
                    continue;
                }
                record_found_file(path, keyword, fm);
            }
        } catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
    }

    //**********************************************************
    private void record_found_file(Path path, String keyword, Finder_message fm)
    //**********************************************************
    {

        if ( ultra_dbg) logger.log("Matching file found: "+path.toAbsolutePath());
        if ( fm.callback != null)
        {
            fm.callback.add_one_Search_result(new Search_result(path.toAbsolutePath().toString(),keyword), new Search_statistics(visited_folders,visited_files));
        }
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
    private void print_keywords(List<String> keywords)
    //**********************************************************
    {
        //if ( dbg == false) return;

        logger.log("---Finder keywords------");
        for( String s: keywords)
        {
            logger.log("->"+s+"<-");
        }
        logger.log("------------------------");

    }



}
