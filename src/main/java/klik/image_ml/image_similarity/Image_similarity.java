package klik.image_ml.image_similarity;

//SOURCES ../Feature_vector_mask.java
//SOURCES ./Vector_window.java

import klik.actor.Aborter;
import klik.browser.Clearable_RAM_cache;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.browser.icons.image_properties_cache.Image_properties;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.image_ml.Feature_vector;
import klik.image_ml.Feature_vector_mask;
import klik.images.Image_window;
import klik.util.log.Logger;
import klik.util.ui.Hourglass;
import klik.util.ui.Jfx_batch_injector;
import klik.util.ui.Show_running_film_frame_with_abort_button;

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
    public final Path_list_provider path_list_provider;
    public final Logger logger;
    public final Aborter aborter;
    //**********************************************************
    public Image_similarity(
            Path_list_provider path_list_provider,
            double x, double y,
            Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.path_list_provider = path_list_provider;
        this.logger = logger;
        this.aborter = aborter;
        this.images_and_feature_vectors = Image_feature_vector_cache.preload_all_feature_vector_in_cache(path_list_provider,x,y,aborter,logger);
    }

    //**********************************************************
    public List<Most_similar> find_similars(
            boolean quasi_same,
            Path image_path,
            List<Path> already_done,//maybe null
            int N,
            boolean and_show,
            double threshold,
            Image_properties_RAM_cache image_properties_cache,
            boolean use_mask,
            double x, double y,
            AtomicLong count_pairs_examined)
    //**********************************************************
    {
        Hourglass hourglass = null;
        if ( and_show) hourglass = Show_running_film_frame_with_abort_button.show_running_film("wait",20000, x,y, logger);

        if (images_and_feature_vectors == null)
        {
            return new ArrayList<>();
        }

        Feature_vector fv0 = images_and_feature_vectors.image_feature_vector_ram_cache().get_from_cache(image_path, null, true);
        if ( fv0 ==null)
        {
            return new ArrayList<>();
        }
        List<Path> to_be_compared = new ArrayList<>(images_and_feature_vectors.images());
        to_be_compared.remove(image_path);
        if ( already_done!= null) to_be_compared.removeAll(already_done);
        List<Most_similar> most_similars = find_similars_of(image_path, fv0,
                quasi_same,use_mask,
                N, threshold,
                image_properties_cache,
                to_be_compared,
                count_pairs_examined);

        if ( already_done!= null) already_done.add(image_path);
        if ( !and_show)
        {
            return most_similars;
        }
        Runnable rr = new Runnable() {
            @Override
            public void run() {
                double x = 10;
                double y = 10;
                show_one_at(new Most_similar(image_path,fv0,fv0,0.0),false,x,y);
                y += H;
                for ( Most_similar ms : most_similars)
                {
                    show_one_at(ms,true,x,y);
                    x += W;
                }
            }
        };
        Jfx_batch_injector.inject(rr, logger);
        hourglass.close();
        return most_similars;
    }


    //**********************************************************
    private void show_one_at(Most_similar ms,boolean not_same,double x, double y)
    //**********************************************************
    {
        String s = String.format("%.4f",ms.similarity());
        Image_window returned = new Image_window(Optional.empty(), ms.path(), x, y, W, H, s, false,path_list_provider,aborter,logger);
        returned.the_Stage.setX(x);
        returned.the_Stage.setY(y);
        logger.log("x="+returned.the_Stage.getX());
        logger.log("y="+returned.the_Stage.getY());

        //show_vector_differences(ms,not_same, x,y+H);

    }

    private void show_vector_differences(Most_similar ms, boolean not_same, double x, double y)
    {
        Vector_window vw = new Vector_window("Distance: "+ms.similarity,x,y,ms.fv1,ms.fv2,not_same,true,logger);

    }

    //**********************************************************
    public List<Most_similar> find_similars_of(
            Path path0,
            Feature_vector fv0,
            boolean quasi_same,
            boolean use_mask,
            int N,
            double threshold,
            Image_properties_RAM_cache image_properties_cache,
            List<Path> targets,
            AtomicLong count_pairs_examined)
    //**********************************************************
    {
        List<Most_similar> returned =  new ArrayList<>();
        double min = Double.MAX_VALUE;
        Image_properties ip0 = null;
        if ( quasi_same)
        {
            ip0 = image_properties_cache.get_from_cache(path0,null);
        }
        Feature_vector_mask mask = null;
        for(Path path1 : targets)
        {
            if ( count_pairs_examined!= null) count_pairs_examined.incrementAndGet();
            if ( quasi_same)
            {
                Image_properties ip1 = image_properties_cache.get_from_cache(path1,null);
                if ( ip0.w() != ip1.w()) continue;
                if ( ip0.h() != ip1.h()) continue;
            }

            Feature_vector fv1 = images_and_feature_vectors.image_feature_vector_ram_cache().get_from_cache(path1, null,true);
            if (fv1 == null) continue; // server failure

            Double similarity = null;
            if ( mask!= null)
            {
                similarity = mask.similarity_with_mask(fv0,fv1);
            }
            else
            {
                similarity = read_similarity_from_cache(path0, path1);
                if (similarity == null) {
                    similarity = fv0.cosine_similarity(fv1);
                    save_similarity_in_cache(similarity, path0, path1);
                }
            }
            if (use_mask)
            {
                if ( mask == null)
                {
                    mask = new Feature_vector_mask(fv0, fv1,true,logger);
                }
            }
            if ( quasi_same)
            {
                if ( similarity > 0) // i.e. not zero, for a double
                {
                    continue;
                }
            }
            if ( similarity > threshold) // ignore if too far
            {
                continue;
            }

            Most_similar ms = new Most_similar(path1,fv0,fv1,similarity);
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




    public record Most_similar(Path path, Feature_vector fv1, Feature_vector fv2, Double similarity){};


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
