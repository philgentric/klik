package klik.image_ml.image_similarity;

import klik.actor.Aborter;
import klik.actor.Job_termination_reporter;
import klik.browser.Browser;
import klik.image_ml.Feature_vector;
import klik.images.Image_window;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;
import klik.util.ui.Hourglass;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Show_running_man_frame_with_abort_button;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;

//**********************************************************
public class Image_similarity
//**********************************************************
{
    public static final double W = 300;
    public static final double H = 300;


    Map<Path,Map<Path,Double>> similarities = new HashMap<>();
    public final Browser browser;
    public final Logger logger;
    //**********************************************************
    public Image_similarity(Browser browser, Logger logger)
    //**********************************************************
    {
        this.browser = browser;
        this.logger = logger;
    }

    //**********************************************************
    public void find_and_show_similars(Path image_path, int N)
    //**********************************************************
    {
        Hourglass x = Show_running_man_frame_with_abort_button.show_running_man("wait",20000, logger);

        Result result = preload_all_feature_vector_in_cache(image_path.getParent(),browser.aborter,logger);
        if (result == null)
        {
            return;
        }


        Feature_vector fv2 = result.image_feature_vector_ram_cache().get_from_cache(image_path, null, true);
        result.targets.remove(image_path);
        List<Most_similar> most_similars = find_most_similars(N,
                                                              result.targets(),
                                                              result.image_feature_vector_ram_cache(), fv2, image_path);

        Runnable rr = new Runnable() {
            @Override
            public void run() {
                double x = 10;
                double y = 10;
                show_one_at(new Most_similar(image_path,0.0),x,y);
                y += H;
                for ( Most_similar ms : most_similars)
                {
                    show_one_at(ms,x,y);
                    x += W;
                }
            }
        };
        Jfx_batch_injector.inject(rr, logger);
        x.close();
    }

    //**********************************************************
    public static Result preload_all_feature_vector_in_cache(Path folder_path, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Image_feature_vector_RAM_cache image_feature_vector_ram_cache = new Image_feature_vector_RAM_cache(folder_path,"image_feature_vectors", aborter, logger);

        image_feature_vector_ram_cache.reload_cache_from_disk(aborter);

        File[] files = folder_path.toFile().listFiles();
        List<Path> targets = new ArrayList<>();
        if ( files == null) return null;
        for (File f : files)
        {
            if ( f.isDirectory()) continue;

            if ( f.getName().startsWith("._"))
            {
                continue;
            }

            if ( !Guess_file_type.is_file_an_image(f)) continue;
            targets.add(f.toPath());
        }
        if ( targets.isEmpty()) return null;
        CountDownLatch cdl = new CountDownLatch(targets.size());
        Job_termination_reporter tr = (message, job) -> {
            cdl.countDown();
            if ( cdl.getCount() % 100 == 0) logger.log("preloading FVs into cache: "+cdl.getCount());
        };
        // start the cache warming on many threads
        for ( int i = 0 ; i < targets.size(); i++)
        {
            Path p1 = targets.get(i);
            image_feature_vector_ram_cache.get_from_cache(p1,tr,false);
        }
        try {
            cdl.await();
        } catch (InterruptedException e) {
            logger.log(""+e);
            return null;
        }
        image_feature_vector_ram_cache.save_whole_cache_to_disk();
        Result result = new Result(image_feature_vector_ram_cache, targets);
        return result;
    }

    public record Result(Image_feature_vector_RAM_cache image_feature_vector_ram_cache, List<Path> targets)
    {
    }


    //**********************************************************
    private void show_one_at(Most_similar ms,double x, double y)
    //**********************************************************
    {
        String s = String.format("%.4f",ms.similarity());
        Image_window returned = new Image_window(browser, ms.path(), x, y, W, H, s, false,logger);
        returned.the_Stage.setX(x);
        returned.the_Stage.setY(y);
        logger.log("x="+returned.the_Stage.getX());
        logger.log("y="+returned.the_Stage.getY());

    }
    //**********************************************************
    private List<Most_similar> find_most_similars(int N, List<Path> targets, Image_feature_vector_RAM_cache image_feature_vector_ram_cache, Feature_vector fv2, Path path)
    //**********************************************************
    {
        List<Most_similar> returned =  new ArrayList<>();
        double min = Double.MAX_VALUE;
        for(Path p : targets)
        {
            Double similarity = read_similarity(path,p);
            if ( similarity == null)
            {
                Feature_vector fv1 = image_feature_vector_ram_cache.get_from_cache(p, null,true);
                if (fv1 == null) continue; // server failure
                similarity = fv1.cosine_similarity(fv2);
                store_similarity(similarity, path, p);
            }
            Most_similar ms = new Most_similar(p,similarity);
            min = crounch(N,returned,ms, min);
        }
        return returned;
    }

    //**********************************************************
    Comparator<? super Most_similar> comp = new Comparator<Most_similar>()
    //**********************************************************
    {
        @Override
        public int compare(Most_similar o1, Most_similar o2) {
            return o1.similarity().compareTo(o2.similarity());
        }
    };

    //**********************************************************
    private double crounch(int N, List<Most_similar> local, Most_similar ms, double min)
    //**********************************************************
    {
        local.add(ms);
        local.sort(comp);
        if ( local.size() > N) local.removeLast();
        return local.get(0).similarity();
    }

    //**********************************************************
    private List<Most_similar> find_most_similars_old(int N, List<Path> targets, Image_feature_vector_RAM_cache image_feature_vector_ram_cache, Feature_vector fv2, Path path)
    //**********************************************************
    {
        List<Most_similar> returned =  new ArrayList<>();
        for ( int i = 0 ; i < N ; i ++)
        {
            Most_similar ms = find_min(targets, image_feature_vector_ram_cache, fv2, path);
            if ( ms.path == null) break;
            targets.remove(ms.path());
            returned.add(ms);
        }
        return returned;
    }


    record Most_similar(Path path,Double similarity){};

    //**********************************************************
    private Most_similar find_min(List<Path> targets, Image_feature_vector_RAM_cache image_feature_vector_ram_cache, Feature_vector fv2, Path path)
    //**********************************************************
    {
        Path min_p1 = null;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < targets.size(); i++)
        {
            Path p1 = targets.get(i);
            Double similarity = read_similarity(path,p1);
            if ( similarity == null)
            {
                Feature_vector fv1 = image_feature_vector_ram_cache.get_from_cache(p1, null,true);
                if (fv1 == null) continue; // server failure
                similarity = fv1.cosine_similarity(fv2);
                store_similarity(similarity, path, p1);
            }
            if ( similarity < min)
            {
                min = similarity;
                min_p1 = p1;
            }
        }
        return new Most_similar(min_p1,min);
    }


    //**********************************************************
    private void store_similarity(Double similarity, Path p1, Path p2)
    //**********************************************************
    {
        Map<Path, Double> m1 = similarities.get(p1);
        if (m1 != null)
        {
            m1.put(p2,similarity);
            return;
        }
        Map<Path, Double> m2 = similarities.get(p2);
        if (m2 != null)
        {
            m2.put(p1, similarity);
            return;
        }

        m1 = new HashMap<>();
        similarities.put(p1,m1);
        m1.put(p2, similarity);
    }

    //**********************************************************
    private Double read_similarity(Path p1, Path p2)
    //**********************************************************
    {
        Map<Path, Double> m1 = similarities.get(p1);
        if (m1 != null)
        {
            Double similarity = m1.get(p2);
            if ( similarity != null) return similarity;
        }
        Map<Path, Double> m2 = similarities.get(p2);
        if (m2 != null)
        {
            Double similarity = m2.get(p1);
            if ( similarity != null) return similarity;
        }
        return null;
    }
    //**********************************************************
    private Double get_similarity_from_RAM_cache(Path p1, Path p2)
    //**********************************************************
    {
        {
            Map<Path, Double> m = similarities.get(p1);
            if ( m != null)
            {
                Double s = m.get(p2);
                if ( s != null) return s;
                else return null;
            }
        }
        {
            Map<Path, Double> m = similarities.get(p2);
            if ( m != null)
            {
                Double s = m.get(p1);
                return s;
            }
            else
            {
                return null;
            }
        }
    }

}
