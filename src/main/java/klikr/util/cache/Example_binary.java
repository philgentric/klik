package klikr.util.cache;

import javafx.stage.Window;
import klikr.path_lists.Path_list_provider;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Job;
import klikr.util.execute.actor.Job_termination_reporter;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiPredicate;
import java.util.function.Function;


public class Example_binary
{
    public static void do_it(Window owner, Logger logger)
    {

        BiPredicate<Path, DataOutputStream> key_serializer= new BiPredicate<Path, DataOutputStream>() {
            @Override
            public boolean test(Path path, DataOutputStream dos)
            {
                String full_path = path.toAbsolutePath().normalize().toString();
                try {
                    dos.writeUTF(full_path);
                    return true;
                } catch (IOException e) {
                    logger.log(""+e);
                }
                return false;
            }
        };

        Function<DataInputStream, Path> key_deserializer = new Function<DataInputStream, Path>() {
            @Override
            public Path apply(DataInputStream dis)
            {
                try {
                    String full_path = dis.readUTF();
                    return Path.of(full_path);
                } catch (IOException e) {
                    logger.log(""+e);
                }

                return null;
            }
        };

        BiPredicate<My_my, DataOutputStream> value_serializer = new BiPredicate<My_my, DataOutputStream>() {
            @Override
            public boolean test(My_my data, DataOutputStream dos) {
                try {
                    dos.writeUTF(data.s);
                    dos.writeInt(data.length);
                    return true;
                } catch (IOException e) {
                    logger.log(""+e);
                }
                return false;
            }
        };
        Function<DataInputStream, My_my> value_deserializer = new Function<DataInputStream, My_my>() {
            @Override
            public My_my apply(DataInputStream dis) {
                try {
                    String s = dis.readUTF();
                    int l = dis.readInt();
                    if ( l != s.length())
                    {
                        logger.log(Stack_trace_getter.get_stack_trace("panic"));
                    }
                    return new My_my(s,l);
                } catch (IOException e) {
                    logger.log(""+e);
                }
                return null;
            }
        };




        Function<Path,My_my> value_extractor = new Function<Path,My_my>() {
            @Override
            public My_my apply(Path p)
            {
                logger.log("maker "+p);
                return My_my.make(p);
            }
        };
        Function<Path,String> string_key_maker = new Function<Path, String>() {
            @Override
            public String apply(Path path) {
                return path.toAbsolutePath().normalize().toString();
            }
        };
        Function<String,Path> object_key_maker = new Function<String, Path>() {
            @Override
            public Path apply(String s) {
                return Path.of(s);
            }
        };

        Aborter aborter = new Aborter("dummy",logger);

        Path_list_provider plp = new Path_list_provider_for_file_system(Path.of("."),owner,logger);

        RAM_cache<Path,My_my> cache = new RAM_cache<Path,My_my>(
                plp,"example2",
                key_serializer, key_deserializer,
                value_serializer, value_deserializer,
                value_extractor,
                string_key_maker, object_key_maker,
                aborter, owner,logger);

        File[] files = (new File(".")).listFiles();
        for ( File f : files)
        {
            cache.prefill_cache(f.toPath(), true, aborter,owner);
        }
        for ( File f : files) {
            My_my mm = cache.get(f.toPath(), aborter, null, owner);
            logger.log("ok for " + mm.to_string());
        }
        cache.save_whole_cache_to_disk();

        cache.reload_cache_from_disk();
        for ( File f : files) {
            My_my mm = cache.get(f.toPath(), aborter, null, owner);
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
            cache.get(f.toPath(), aborter, tr, owner);
        }

        try
        {
            cdl.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        cache.save_whole_cache_to_disk();
        logger.log("Done!");

        RAM_cache<Path,My_my> cache2 = new RAM_cache<Path,My_my>(plp, cache.name,
                key_serializer, key_deserializer,
                value_serializer, value_deserializer,
                value_extractor,
                string_key_maker, object_key_maker,
                aborter, owner,logger);

        cache2.reload_cache_from_disk();
        for ( File f : files)
        {
            My_my mm = cache2.get(f.toPath(), aborter, null, owner);
            logger.log("from disk: "+mm.to_string());
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
