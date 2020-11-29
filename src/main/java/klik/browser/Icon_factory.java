package klik.browser;

import javafx.scene.image.Image;
import klik.images.From_disk;
import klik.util.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

//**********************************************************
public class Icon_factory
//**********************************************************
{
    private static final boolean dbg = false;
    Logger logger;
    LinkedBlockingDeque<Icon_factory_request> input_queue_single = new LinkedBlockingDeque<Icon_factory_request>();
    Icon_writer_actor writer;
    Path icon_cache_dir;
    private volatile boolean die = false;
    Path tmp_dir;

    //**********************************************************
    public Icon_factory(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        if (dbg) logger.log("Icon_factory created");

        icon_cache_dir = Tool_box.get_icon_cache_dir(logger);
        //tmp_dir = Tool_box.get_gif_icons_for_videos_tmp_dir(logger);
        tmp_dir = icon_cache_dir;

        int n_threads = Runtime.getRuntime().availableProcessors();
        //executor = new ThreadPoolExecutor(n_threads,n_threads,1, TimeUnit.SECONDS,new LinkedBlockingQueue<>());
        writer = Icon_writer_actor.launch_icon_writer(icon_cache_dir, logger);
        // start n_threads worker threads
        for (int i = 0; i < n_threads; i++) {
            start_one_LIFO_factory_thread_single();
        }


    }

    //**********************************************************
    public void die()
    //**********************************************************
    {
        logger.log("icon factory die received");

        die = true;
        writer.die();
    }

    //**********************************************************
    private void start_one_LIFO_factory_thread_single()
    //**********************************************************
    {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (dbg) logger.log("one Icon_factory_thread starting");
                for (; ; ) {
                    try {
                        //if (dbg) logger.log(" Icon_factory_thread going to wait");

                        Icon_factory_request ifr = input_queue_single.pollFirst(1, TimeUnit.SECONDS);
                        if (ifr == null) {
                            // opportunity here to exit the thread
                            if (die) {
                                logger.log("icon factory thread exiting");
                                return;
                            }
                            continue;
                        }

                        //Icon_factory_request ifr = input_queue_single.take();
                        process(ifr);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        try {
            Tool_box.execute(r, logger);
        } catch (Exception e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }

    }


    //**********************************************************
    public void make_icon(Icon_factory_request l)
    //**********************************************************
    {
        if (dbg) logger.log("emergency icon request made ");
        if (l.destination.icon_status == Icon_status.true_icon_in_the_making) return;
        if (l.destination.icon_status == Icon_status.true_icon) return;
        input_queue_single.addFirst(l);
        l.destination.icon_status = Icon_status.true_icon_in_the_making;
        //input_queue_single.add(l);
    }

    //**********************************************************
    private void process(Icon_factory_request icon_factory_request) throws InterruptedException
    //**********************************************************
    {
        if (dbg) logger.log("icon request processing starts ");

        Item_image item_image = icon_factory_request.destination;
        if (item_image == null) {
            logger.log("icon factory : cancel!");
            input_queue_single.clear();
            return;
        }

        Image image = null;
        if (Guess_file_type_from_extension.is_this_path_a_video(item_image.get_Path()))
        {
            image = process_video(icon_factory_request,item_image);
        }
        else
            {
                image = process_image(icon_factory_request,item_image);
        }
        if (dbg) logger.log("Icon_factory icon ready");

        if ( image == null)
        {
            // must treat as non_image
            logger.log("RECORDING");
            icon_factory_request.exception_recorder.record(item_image.get_Path());
            return;
        }

        item_image.set_Image(image, true);


    }

    //**********************************************************
    private Image process_image(Icon_factory_request icon_factory_request, Item_image item_image)
    //**********************************************************
    {
        Image image = From_disk.load_icon_from_disk_cache_fx(item_image.get_Path(), icon_cache_dir, icon_factory_request.icon_size, logger);


        if (image == null) {
            if (dbg)
                logger.log("Icon_factory thread:  load from cache FAILED for " + item_image.get_Path().getFileName());

            image = From_disk.load_icon_fx_from_disk(item_image.get_Path(), icon_factory_request.icon_size, logger);
            if (image == null) {
                logger.log("Icon_factory thread: load from file FAILED for " + item_image.get_Path().getFileName());
                return null;
            }

            // dont try to disk-cache for gifs, they are either small or animated

            if (Guess_file_type_from_extension.is_gif_extension(item_image.get_Path()) == false) {
                if (dbg)
                    logger.log("Icon_factory thread: sending icon write to file in cache dir for " + item_image.get_Path().getFileName());
                Icon_write_message iwm = new Icon_write_message(image, icon_factory_request.icon_size, item_image.get_Path());
                writer.push(iwm);
            }
        } else {
            if (dbg) logger.log("Icon_factory thread: found in cache: " + item_image.get_Path().getFileName());
        }
        return image;
    }

    //**********************************************************
    private Image process_video(Icon_factory_request icon_factory_request, Item item_image)
    //**********************************************************
    {
        logger.log("Icon_factory thread:  process_video " + item_image.get_Path().toAbsolutePath());

        // we are going to create the gif using ffmpeg!
        // ... unless it is already in the video-specific GIF cache !?

        String resulting_gif_name = tmp_dir.toString() +File.separator+ Tool_box.CLEAN_NAME(item_image.get_Path().toAbsolutePath().toString()) + ".gif";
        Path resulting_gif_path = Paths.get(resulting_gif_name);
        Image image = From_disk.load_icon_from_disk_cache_fx(resulting_gif_path, tmp_dir, icon_factory_request.icon_size, logger);

        if (image == null) {
            //if (dbg)
                logger.log("Icon_factory thread:  load from GIF tmp FAILED for " + resulting_gif_path.getFileName());

            // ffmpeg -i movie.mp4 -r 10  -t 00:00:2.000 output.gif

            List<String> list = new ArrayList<>();
            list.add("ffmpeg");
            list.add("-y"); // force overwrite of output without asking
            list.add("-i");
            list.add(item_image.get_Path().getFileName().toString());
            list.add("-r");
            list.add("10");
            list.add("-t");
            list.add("00:00:2.000");
            list.add(resulting_gif_name);
            File wd = item_image.get_Path().getParent().toFile();
            Execute_command.execute_command_list(list, wd, 2000, logger);


            image = From_disk.load_icon_fx_from_disk(resulting_gif_path, icon_factory_request.icon_size, logger);
            if (image == null) {
                logger.log("Icon_factory thread: load from file FAILED for " + item_image.get_Path().getFileName());
                return null;
            }

        } else {
            if (dbg) logger.log("Icon_factory thread: found in cache: " + resulting_gif_path.getFileName());
        }

        return image;
    }

    /*
    private void create_item_video(Item_video item_video)
    {
        logger.log("VIDEO icon request processing starts ");
        Media media = null;
        try {
            Path ppp = item_video.get_Path().toAbsolutePath();
            URI uri = ppp.toUri();
            URL url = uri.toURL();
            media = new Media(url.toString());
        } catch (MalformedURLException e) {
            logger.log("Icon_factory thread: URL FAILED for " + item_video.get_Path().getFileName());
            return;
        }
        catch (Exception e) {
            logger.log(Stack_trace_getter.get_stack_trace("Icon_factory thread: " + e));
            return;
        }
        final Media media2=  media;

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                MediaPlayer mediaPlayer = null;
                try {
                    mediaPlayer = new MediaPlayer(media2);
                }
                catch (Exception e) {
                    logger.log(Stack_trace_getter.get_stack_trace("Icon_factory thread: " + e));
                    return;
                }
                mediaPlayer.seek(new Duration(2000));
                int width = 300;//mediaPlayer.getMedia().getWidth();
                int height =300;// mediaPlayer.getMedia().getHeight();
                //WritableImage wim = new WritableImage(width, height);
                item_video.media_view = new MediaView();
                item_video.media_view.setFitWidth(width);
                item_video.media_view.setFitHeight(height);
                item_video.media_view.setMediaPlayer(mediaPlayer);

                if ( item_video.visible_in_scene == false)
                {
                    item_video.set_imageview_null();
                    item_video.icon_status = Icon_status.no_icon;
                    return;
                }
                //mv.snapshot(null, wim);

                item_video.media_view.setVisible(true);


                item_video.set_imageview_null();

                item_video.icon_status = Icon_status.true_icon;

                item_video.height = null;
                logger.log("VIDEO icon request processing ends ");
            }
        });

    }
    */

}
