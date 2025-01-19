package klik.image_ml.image_similarity;

import klik.browser.Browser;
import klik.browser.Clearable_RAM_cache;
import klik.browser.icons.image_properties_cache.Image_properties;
import klik.image_ml.Feature_vector;
import klik.images.Image_window;
import klik.util.log.Logger;
import klik.util.ui.Hourglass;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Show_running_man_frame_with_abort_button;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

//**********************************************************
public class Image_similarity implements Clearable_RAM_cache
//**********************************************************
{

    //**********************************************************
    @Override
    public void clear_RAM_cache()
    //**********************************************************
    {
        similarities.clear();
        images_and_feature_vectors.image_feature_vector_ram_cache().clear_feature_vector_RAM_cache();
    }



    public static final double W = 300;
    public static final double H = 300;

    Image_feature_vector_cache.Images_and_feature_vectors images_and_feature_vectors;

    Map<Path,Map<Path,Double>> similarities = new HashMap<>();
    public final Browser browser;
    public final Logger logger;
    //**********************************************************
    public Image_similarity(Browser browser, Logger logger)
    //**********************************************************
    {
        this.browser = browser;
        this.logger = logger;
        this.images_and_feature_vectors = Image_feature_vector_cache.preload_all_feature_vector_in_cache(browser.displayed_folder_path,browser.aborter,logger);
    }

    //**********************************************************
    public List<Most_similar> find_similars(boolean quasi_same, Path image_path, int N, boolean and_show, double threshold, AtomicLong count_pairs_examined)
    //**********************************************************
    {
        Hourglass x = null;
        if ( and_show) x = Show_running_man_frame_with_abort_button.show_running_man("wait",20000, logger);

        if (images_and_feature_vectors == null)
        {
            return null;
        }

        Feature_vector fv2 = images_and_feature_vectors.image_feature_vector_ram_cache().get_from_cache(image_path, null, true);

        List<Path> images_copy = new ArrayList<>(images_and_feature_vectors.images());
        images_copy.remove(image_path);
        List<Most_similar> most_similars = find_most_similars(quasi_same,N, threshold,
                images_copy,
                images_and_feature_vectors.image_feature_vector_ram_cache(), fv2, image_path, count_pairs_examined);

        if ( !and_show)
        {
            return most_similars;
        }
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
        return most_similars;
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
    private List<Most_similar> find_most_similars(boolean quasi_same, int N, double threshold, List<Path> targets, Image_feature_vector_cache image_feature_vector_ram_cache, Feature_vector fv2, Path path, AtomicLong count_pairs_examined)
    //**********************************************************
    {
        List<Most_similar> returned =  new ArrayList<>();
        double min = Double.MAX_VALUE;
        //int count = 0;
        Image_properties ip1 = null;
        if ( quasi_same)
        {
            ip1 = browser.image_properties_cache.get_from_cache(path,null);
        }
        //int discarded = 0;
        for(Path p : targets)
        {
            if ( count_pairs_examined!= null) count_pairs_examined.incrementAndGet();
            if ( quasi_same)
            {
                Image_properties ip2 = browser.image_properties_cache.get_from_cache(p,null);
                if ( ip1.w() != ip2.w()) continue;
                if ( ip1.h() != ip2.h()) continue;
            }

            Double similarity = read_similarity_from_cache(path,p);
            if ( similarity == null)
            {
                Feature_vector fv1 = image_feature_vector_ram_cache.get_from_cache(p, null,true);
                if (fv1 == null) continue; // server failure
                similarity = fv1.cosine_similarity(fv2);
                save_similarity_in_cache(similarity, path, p);
            }
            if ( quasi_same)
            {
                if ( similarity > 0)
                {
                    //discarded++;
                    //if ( discarded%1000 == 0) logger.log("images discarded, too far: "+discarded);
                    continue;
                }
            }
            if ( similarity > threshold) continue;
            Most_similar ms = new Most_similar(p,similarity);
            min = keep_N_closest(N,returned,ms, min);
            //count++;
            //if ( count % 100 == 0) logger.log("image compared count="+count);
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
    private double keep_N_closest(int N, List<Most_similar> local, Most_similar ms, double min)
    //**********************************************************
    {
        local.add(ms);
        local.sort(comp);
        if ( local.size() > N) local.removeLast();
        return local.getFirst().similarity();
    }




    record Most_similar(Path path,Double similarity){};


    //**********************************************************
    private void save_similarity_in_cache(Double similarity, Path p1, Path p2)
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
    private Double read_similarity_from_cache(Path p1, Path p2)
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


}
