package klik.browser.locator;

import javafx.application.Platform;
import klik.actor.Aborter;
import klik.actor.Actor_engine;
import klik.browser.Browser;
import klik.browser.Browser_creation_context;
import klik.files_and_paths.*;
import klik.util.Logger;
import klik.util.Threads;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

//**********************************************************
public class Locator
//**********************************************************
{
    private static boolean dbg = false;
    private static Locator instance = null;
    private final Path top;
    private final int minimum_count;
    private final int min_bytes;
    private final Browser browser;
    private final Logger logger;
    private final  ConcurrentHashMap<String,Integer> contanimated_directories = new ConcurrentHashMap<>();
    AtomicInteger folders = new AtomicInteger(0);
    private final  List<String> final_choice = new ArrayList<>();
    private static final int MAX_WINDOWS = 10;

    private final Aborter private_aborter = new Aborter();
    private Monitor monitor = null;

    //**********************************************************
    public static void locate(Path top, int minimum_count, int min_bytes, Browser browser, Logger logger)
    //**********************************************************
    {
        instance = new Locator(top,minimum_count,min_bytes, browser,logger);
        instance.search();
    }


    //**********************************************************
    private Locator(Path top, int minimum_count, int min_bytes, Browser browser, Logger logger)
    //**********************************************************
    {
        this.top = top;
        this.minimum_count = minimum_count;
        this.min_bytes = min_bytes;
        this.browser = browser;
        this.logger = logger;

    }

    //**********************************************************
    private void search()
    //**********************************************************
    {
        Locator locator = this;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                explore(top.toFile());


                // wait for exploration to end
                long start = System.currentTimeMillis();
                for(;;)
                {
                    int count = folders.get();
                    if (  count==0)
                    {
                        break;
                    }
                    if ( dbg) logger.log(" remaining sub-folders="+count);
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        logger.log(""+e);
                    }
                    if ( (browser.aborter.should_abort())||(private_aborter.should_abort()))
                    {
                        logger.log("Locator thread aborting");
                        if ( monitor!=null) monitor.close();
                        return;
                    }
                    long now = System.currentTimeMillis();
                    if ( now-start > 3000)
                    {
                        String msg = "...still exploring, found "+contanimated_directories.size()+" folders with images";
                        logger.log(msg);
                        if ( monitor == null)
                        {
                            monitor = new Monitor(top,locator,logger);
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    monitor.realize();
                                }
                            };
                            Platform.runLater(r);
                        }
                        else
                        {
                            monitor.show(msg);
                        }
                        start = now;
                    }

                }
                if ( dbg) print_all_contaminated();

                {
                    String x = "exploration ended, going to analyze " + contanimated_directories.size() + " folders";
                    if ( monitor!=null) monitor.show(x);
                    logger.log(x);
                }
                bottom_to_top();
                if ( dbg) print_all_contaminated();
                if ( dbg) logger.log("wrapup ended, going to prune "+contanimated_directories.size());
                prune();
                if ( dbg) logger.log("prune ended:"+contanimated_directories.size()+" going to show ");
                show();
            }
        };
        Actor_engine.execute(r,logger);
    }

    //**********************************************************
    private void explore(File dir)
    //**********************************************************
    {
        if (!dir.isDirectory()) return;
        if (Files.isSymbolicLink(dir.toPath())) return;

        //logger.log("Locator looking at folder: "+dir);
        if (contanimated_directories.get(file_to_key(dir)) != null) {
            if ( dbg) logger.log("explore folder: " + dir + " already done");
            return;
        }

        File[] all_files = dir.listFiles();
        if (all_files == null) {
            return;
        }
        int count_images = 0;
        for (File f : all_files) {
            if ((browser.aborter.should_abort())||(private_aborter.should_abort()))
            {
                if ( dbg) logger.log("Locator thread aborting");
                if ( monitor!=null) monitor.close();
                return;
            }

            if (!f.isFile()) continue;
            if (Guess_file_type.is_file_an_image(f))
            {
                if ( f.length() >= min_bytes)
                {
                    count_images++;
                    if (count_images > minimum_count) {
                        contanimated_directories.put(file_to_key(dir), minimum_count);

                        if ( dbg) logger.log("images found in: " + dir);
                        break;
                    }
                }
            }
        }
        if (count_images > minimum_count)
        {
            // no use in scanning sub folders if this one is already contaminated
            return;
        }

        if ((browser.aborter.should_abort())||(private_aborter.should_abort()))
        {
            logger.log("Locator thread aborting");
            if ( monitor!=null) monitor.close();
            return;
        }

        // launch one thread per sub_folder
        for (File f : all_files) {
            if (f.isDirectory())
            {
                if ( Files.isSymbolicLink(f.toPath())) continue;
                folders.incrementAndGet();

                //logger.log("count after inc="+folders.get());
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        explore(f);
                        folders.decrementAndGet();
                        //logger.log("count after dec="+folders.get()+" size="+contanimated_directories.size());
                    }
                };
                if ( Threads.use_fibers)
                {
                    Actor_engine.execute(r,logger);
                }
                else
                {
                    // dont use a thread
                    r.run();
                }
            }
            if ((browser.aborter.should_abort())||(private_aborter.should_abort()))
            {
                logger.log("Locator thread aborting");
                if ( monitor!=null) monitor.close();
                return;
            }
        }


    }

    //**********************************************************
    private static String file_to_key(File f)
    //**********************************************************
    {
        return f.toPath().toAbsolutePath().toString();
    }


    //**********************************************************
    private static Path key_to_path(String s)
    //**********************************************************
    {
        return Path.of(s);
    }

    //**********************************************************
    private void bottom_to_top()
    //**********************************************************
    {
        for(;;)
        {
            int before = contanimated_directories.size();
            pass();
            int after = contanimated_directories.size();
            if ( before == after) break;
            if ( dbg) logger.log("contaminated= "+contanimated_directories.size());
        }
    }

    //**********************************************************
    private void pass()
    //**********************************************************
    {

        for (String key : contanimated_directories.keySet())
        {
            Path dir = key_to_path(key);
            Path parent = dir.getParent();
            if (is_contaminated(parent))
            {
                contanimated_directories.put(file_to_key(parent.toFile()), minimum_count);
            }
            if ( (browser.aborter.should_abort())||(private_aborter.should_abort()))
            {
                logger.log("Locator thread aborting");
                if ( monitor!=null) monitor.close();
                return;
            }
        }
    }

    //**********************************************************
    private boolean is_contaminated(Path folder)
    //**********************************************************
    {
        if ( dbg) logger.log("is folder contaminated? "+folder);
        if ( contanimated_directories.get(file_to_key(folder.toFile())) != null)
        {
            return true;
        }

        File[] all_files = folder.toFile().listFiles();
        if (all_files == null) {
            return false;
        }

        int count = 0;
        for (File f : all_files)
        {
            if (f.isDirectory())
            {
                if ( Files.isSymbolicLink(f.toPath())) continue;

                if ( contanimated_directories.get(file_to_key(f)) != null)
                {
                    if ( dbg) logger.log("subfolder is contaminated:"+f);
                    count++;
                    if ( count >= 2)
                    {
                        if ( dbg) logger.log("parent becomes contaminated:"+folder);
                        return true;
                    }
                }
            }
            if ( (browser.aborter.should_abort())||(private_aborter.should_abort()))
            {
                logger.log("Locator thread aborting");
                if ( monitor!=null) monitor.close();
                return false;
            }
        }
        if ( dbg) logger.log("parent NOT contaminated:"+folder);

        return false;
    }

    //**********************************************************
    private void print_all_contaminated()
    //**********************************************************
    {
        logger.log("\n\n\n==================================");
        logger.log(""+contanimated_directories.size());

        for ( String s : contanimated_directories.keySet())
        {
            logger.log(s);
        }
        logger.log("==================================\n\n\n");

    }

    //**********************************************************
    private void prune()
    //**********************************************************
    {
        // if your parent is not contaminated, let us keep you
        for ( String folder : contanimated_directories.keySet())
        {
            File parent = key_to_path(folder).getParent().toFile();
            String p = file_to_key(parent);
            if (!contanimated_directories.containsKey(p) )
            {
                if ( dbg) logger.log("parent is not contaminated, we keep "+folder);
                if ( !final_choice.contains(folder))
                {
                    final_choice.add(folder);
                }
            }
            else {
                if ( dbg) logger.log("parent IS contaminated, we DONT keep "+folder);
            }
            if ( (browser.aborter.should_abort())||(private_aborter.should_abort()))
            {
                logger.log("Locator thread aborting");
                if ( monitor!=null) monitor.close();
                return;
            }
        }
    }

    //**********************************************************
    private void show()
    //**********************************************************
    {
        int count = 0;
        for ( String s : final_choice)
        {
            //if ( dbg)
            logger.log("final choice: "+s);

            if ( count < MAX_WINDOWS)
            {
                String final_S = s;
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        Browser_creation_context.additional_no_past(key_to_path(final_S), logger);
                    }
                };
                Platform.runLater(r);
                count++;
            }
            else {
                String x = "Too many windows! ... otherwise would also show: "+s;
                logger.log(x);
                if ( monitor!=null) monitor.show(x);
            }

             if ( (browser.aborter.should_abort())||(private_aborter.should_abort()))
            {
                logger.log("Locator thread aborting");
                if ( monitor!=null) monitor.close();
                return;
            }
        }
    }

    public void cancel() {
        private_aborter.abort();
    }
}
