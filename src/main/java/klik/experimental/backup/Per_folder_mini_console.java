package klik.experimental.backup;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import klik.util.execute.actor.Aborter;
import klik.util.execute.actor.Actor_engine;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.ui.Jfx_batch_injector;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.Strings;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

//**********************************************************
public class Per_folder_mini_console
//**********************************************************
{
    final Logger logger;
    final Aborter aborter;
    Directory_backup_job_request request;
    private String renamed_files_names="";
    private String last_news="";
    private final AtomicInteger processed_file_count = new AtomicInteger(0);
    private final AtomicLong copied_bytes =  new AtomicLong(0);
    private final AtomicLong bytes_read =  new AtomicLong(0);
    private final AtomicInteger copied_files = new AtomicInteger(0);
    private final AtomicInteger renamed_files = new AtomicInteger(0);
    private final AtomicInteger skipped_files = new AtomicInteger(0);
    private long start_time;
    private long end_time;
    private long target_file_count;
    //private static final boolean multi_threaded = true;

    public static final int Mini_console_width = 500;
    public static final int Mini_console_max_x = 2*Mini_console_width;
    public static final int Mini_console_max_y = 1000;
    public static final int Mini_console_height = 200;
    public static final int XXX = 0;
    private static int window_x = XXX;
    public static final int YYY = 0;
    private static int window_y = YYY;

    private Stage the_stage;
    private Scene the_scene;
    private TextArea the_text_area;
    //**********************************************************
    public Per_folder_mini_console(Aborter aborter, Logger logger_)
    //**********************************************************
    {
        this.aborter = aborter;
        logger = logger_;
    }



    //**********************************************************
    void create()
    //**********************************************************
    {
        int H = Mini_console_height;
        int W = Mini_console_width;
        Jfx_batch_injector.inject(() -> {
            the_stage = new Stage();
            the_stage.setTitle("Backup console ");
            the_stage.setMinWidth(W);
            the_stage.setMinHeight(H);
            the_text_area = new TextArea();
            the_scene = new Scene(the_text_area);
            the_stage.setScene(the_scene);

            the_stage.setX(window_x);
            the_stage.setY(window_y);
            window_y += H;
            if (window_y > Mini_console_max_y) {
                window_y = YYY;
                window_x += W;
                if (window_x > Mini_console_max_x) {
                    window_x = XXX;
                }
            }
            the_stage.show();
        },logger);
    }
    //**********************************************************
    public void init(Directory_backup_job_request request_)
    //**********************************************************
    {
        start_time = System.currentTimeMillis();
        request = request_;
        Jfx_batch_injector.inject(() -> {
                    the_stage.setTitle("Backing up : " + request.source_dir.getName());
                    the_text_area.setText("Source:"+request.source_dir);
                },logger);
        last_news = "";
        renamed_files_names = "";
        estimate(request.source_dir);

        //target_file_count = Static_files_and_paths_utilities.get_how_many_files_down_the_tree(request.source_dir.toPath(),logger);
    }

    //**********************************************************
    private void estimate(File target_dir)
    //**********************************************************
    {
        // estimates the content of "target_dir" in number of files to check

        target_file_count = 0;

        File[] all_files = target_dir.listFiles();
        if (all_files == null)
        {
            logger.log("NO files in: " + target_dir);
            return;
        }

        for (File f : all_files)
        {
            if (!f.isDirectory())
            {
                if (!Guess_file_type.should_ignore(f.toPath()))
                {
                    target_file_count++;
                }
            }
        }

        last_news = "Looking at :\n" + target_dir + "\nfile count here is : " + target_file_count+"\n";
        logger.log("Looking at :" + target_dir + " file count here is : " + target_file_count);
        show_progress();
    }


    //**********************************************************
    String make_final_report()
    //**********************************************************
    {
        // last news will be good news
        end_time = System.currentTimeMillis();
        last_news = request.source_dir + "\ntotal processing time: " + Strings.create_nice_remaining_time_string(end_time - start_time)+"\n";

        if (copied_bytes.get() > 1000000) {
            double x = (double) copied_bytes.get() / 1000000.0;
            x = Math.floor(x * 100) / 100;
            last_news += x+" MegaBytes copied\n";
        } else if (copied_bytes.get() > 1000) {
            double x = (double) copied_bytes.get() / 1000.0;
            x = Math.floor(x * 100) / 100;
            last_news += x+" KiloBytes copied\n";
        } else {
            last_news += copied_bytes.get() + " Bytes copied\n";
        }

        double x = (double) processed_file_count.get() / (double) (end_time - start_time) * 1000.0;
        x = Math.floor(x * 100) / 100;
        if ( end_time-start_time == 0) x =0;
        if (x > 10) {
            last_news += "average file rate: " + (int) x + " files per second\n";
        } else {
            last_news += "average file rate: " + x + " files per second\n";
        }

        x = (double) copied_bytes.get() / (double) (end_time - start_time) * 1000.0;
        x = Math.floor(x * 100) / 100;
        if ( end_time-start_time == 0) x =0;
        if (x > 1000000.0) {
            x = x / 1000000.0;
            x = Math.floor(x * 100) / 100;
            if (x > 10) {
                last_news += "average copy data rate: " + (int) x + " MegaBytes per second\n";
            } else {
                last_news += "average copy data rate: " + x + " MegaBytes per second\n";
            }
        } else if (x > 1000.0) {
            x = x / 1000.0;
            x = Math.floor(x * 100) / 100;
            if (x > 10) {
                last_news += "average copy data rate: " + (int) x + " KiloBytes per second\n";
            } else {
                last_news += "average copy data rate: " + x + " KiloBytes per second\n";
            }

        } else {
            last_news += "average copy data rate: " + x + " Bytes per second\n";
        }

        return last_news;
    }


    //**********************************************************
    synchronized void show_progress()
    //**********************************************************
    {

        String report = make_current_report();
        Jfx_batch_injector.inject(() -> the_text_area.setText(report),logger);

        if ( processed_file_count.get() == target_file_count)
        {
            Runnable r = () -> {

                        Jfx_batch_injector.inject(() -> {
                            the_text_area.setStyle("-fx-control-inner-background: green;");

                            //the_text_area.setStyle("-fx-base: white; "+ "-fx-font-weight: bold; ");
                            //"text-area-background: green;");
                            //the_text_area.setBackground(new Background(new BackgroundFill(Color.PINK, CornerRadii.EMPTY, Insets.EMPTY)));
                        },logger);


                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
                    return;
                }

                close();
            };
            Actor_engine.execute(r, "Monitor backup",logger);
        }

    }


    //**********************************************************
    public String make_current_report()
    //**********************************************************
    {
        long elapsed_time_since_start = (System.currentTimeMillis() - start_time);

        String remaining_time = "?";
        if (processed_file_count.get() > 1)
        {
            long estimated_tot_time = (long) ((double) elapsed_time_since_start * (double) target_file_count / (double) processed_file_count.get());

            long remaining_time_in_milliseconds = estimated_tot_time - elapsed_time_since_start;

            remaining_time = Strings.create_nice_remaining_time_string(remaining_time_in_milliseconds);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Source: ");
        sb.append(request.source_dir);
        sb.append("\ncurrent progress is: ");
        sb.append(processed_file_count);
        sb.append("/");
        sb.append(target_file_count);
        sb.append(" ");
        sb.append((int) ((double) processed_file_count.get() / (double) target_file_count * 100.0));
        sb.append("% files in this folder (excluding subfolders)");

        sb.append("\n");
        sb.append("Remaining time : ");
        sb.append(remaining_time);
        sb.append("\n");

        sb.append("Copied files: ");
        sb.append(copied_files.get());
        sb.append("\n");
        sb.append("Skipped files: ");
        sb.append(skipped_files.get());
        sb.append("\n");
        sb.append("Renamed files: ");
        sb.append(renamed_files.get());
        sb.append("\n");
        sb.append("Copied bytes: ");
        sb.append(copied_bytes.get());
        sb.append("\n");
        if (renamed_files.get() > 0)
        {
            sb.append("names of renamed files:" );
            sb.append(renamed_files_names);
            sb.append("\n");
        }

        return sb.toString();
    }



    //**********************************************************
    public void close()
    //**********************************************************
    {

        Jfx_batch_injector.inject(() -> {
            if ( the_stage != null) the_stage.close();
        },logger);

    }

    public void add_to_last_news(String s) {
        last_news += s;
    }

    public void add_to_copied_bytes(long l) {
        copied_bytes.addAndGet(l);
    }

    public void increment_file_count() {
        processed_file_count.incrementAndGet();
    }
    public void increment_copied_files() {
    copied_files.incrementAndGet();
    }

    public void increment_skipped_files() {
        skipped_files.incrementAndGet();
    }

    public void increment_renamed_files() {
        renamed_files.incrementAndGet();
    }

    public void add_to_renamed_files_names(String s) {
        renamed_files_names+=s+"\n";
    }

    public void add_to_bytes_read(long length) {
        bytes_read.addAndGet(length);
    }
}
