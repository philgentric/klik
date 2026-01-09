package klikr.util.cache;

import javafx.stage.Window;
import klikr.path_lists.Path_list_provider;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Job;
import klikr.util.execute.actor.Job_termination_reporter;
import klikr.util.log.Logger;
import klikr.util.log.Simple_logger;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;


public class Example
{
    public static void main(String[] args)
    {
        //do_1(null,new Simple_logger());

        Example_binary.do_it(null,new Simple_logger());
    }
    public static void do_1(Window owner, Logger logger)
    {

        Function<My_my, String> serializer = new Function<My_my,String>() {
            @Override
            public String apply(My_my mm) {
                logger.log("serializer "+mm.s);
                return mm.to_string();
            }
        };
        Function<String, My_my> deserializer = new Function<String, My_my>() {
            @Override
            public My_my apply(String s) {
                logger.log("deserializer "+s);
                return new My_my(s,s.length());
            }
        };


        Function<String,My_my> value_extractor = new Function<String, My_my>() {
            @Override
            public My_my apply(String s) {
                return new My_my(s,s.length());
            }
        };
        Function<String,String> string_key_maker = new Function<String, String>() {
            @Override
            public String apply(String p) {
                return p;
            }
        };


        Function<Path, My_my> maker = new Function<Path, My_my>() {
            @Override
            public My_my apply(Path p) {
                logger.log("maker "+p);
                return My_my.make(p);
            }
        };
        Aborter aborter = new Aborter("dummy",logger);

        Path_list_provider plp = new Path_list_provider_for_file_system(Path.of("."),owner,logger);

        RAM_cache<String, My_my> cache = new RAM_cache<String, My_my>(plp,"example",
                serializer,deserializer,
                value_extractor, string_key_maker,
                aborter, owner,logger);

        File[] files = (new File(".")).listFiles();
        for ( File f : files)
        {
            cache.prefill_cache(f.getAbsolutePath(), true, aborter,owner);
        }
        for ( File f : files) {
            My_my mm = cache.get(f.getAbsolutePath(), aborter, null, owner);
            logger.log("ok for " + mm.to_string());
        }
        cache.save_whole_cache_to_disk();

        cache.reload_cache_from_disk();
        for ( File f : files) {
            My_my mm = cache.get(f.getAbsolutePath(), aborter, null, owner);
            logger.log("ok for " + mm.to_string());
        }

        cache.clear_RAM();
        cache.clear_DISK(aborter,owner);

        CountDownLatch cdl = new CountDownLatch(files.length);
        Job_termination_reporter tr = new Job_termination_reporter() {
            @Override
            public void has_ended(String message, Job job) {
                cdl.countDown();
            }
        };
        for ( File f : files)
        {
            cache.get(f.getAbsolutePath(), aborter, tr, owner);
        }

        try
        {
            cdl.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        cache.save_whole_cache_to_disk();
        logger.log("Done!");

        RAM_cache<String,My_my> cache2 = new RAM_cache<String,My_my>(plp, cache.name,
                serializer,deserializer,value_extractor,string_key_maker,aborter, owner,logger);

        cache2.reload_cache_from_disk();
        for ( File f : files)
        {
            My_my mm = cache2.get(f.getAbsolutePath(), aborter, null, owner);
            logger.log("from disk: "+mm.to_string());
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
