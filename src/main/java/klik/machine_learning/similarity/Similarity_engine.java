package klik.machine_learning.similarity;

//SOURCES ./Vector_window.java

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.browser.Clearable_RAM_cache;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.browser.virtual_landscape.Path_list_provider;
import klik.browser.icons.image_properties_cache.Image_properties;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.machine_learning.feature_vector.Feature_vector;
import klik.machine_learning.feature_vector.Feature_vector_cache;
import klik.machine_learning.feature_vector.Feature_vector_double;
import klik.images.Image_window;
import klik.properties.boolean_features.Feature;
import klik.properties.boolean_features.Booleans;
import klik.util.files_and_paths.File_with_a_few_bytes;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.ui.*;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

//**********************************************************
public class Similarity_engine implements Clearable_RAM_cache
//**********************************************************
{
    private final static boolean dbg = false;


    public static final double W = 300;
    public static final double H = 300;

    final List<Path> paths;
    Map<Path,Map<Path,Double>> similarities = new HashMap<>();
    public final Path_list_provider path_list_provider;
    public final Path_comparator_source path_comparator_source;
    public final Logger logger;
    public final Aborter aborter;

    private boolean show_vector_differences;

    //**********************************************************
    public Similarity_engine(
            List<Path> paths,
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            Window owner,
            Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.path_list_provider = path_list_provider;
        this.path_comparator_source = path_comparator_source;
        this.logger = logger;
        this.aborter = aborter;
        this.paths = paths;
        show_vector_differences = Booleans.get_boolean(Feature.Display_image_distances.name(),owner);
    }

    //**********************************************************
    @Override
    public void clear_RAM_cache()
    //**********************************************************
    {
        similarities.clear();
    }

    //**********************************************************
    public List<Most_similar> find_similars(
            Path reference_item_path,
            List<Path> already_done,//maybe null
            int N,
            boolean show_images,
            double threshold,
            Supplier<Feature_vector_cache> fv_cache_supplier,
            Window owner,
            double x, double y,
            AtomicLong count_pairs_examined,
            Aborter browser_aborter)
    //**********************************************************
    {
        if (paths.isEmpty())
        {
            return new ArrayList<>();
        }
        Feature_vector_cache fv_cache = fv_cache_supplier.get();
        if ( fv_cache == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("FATAL: fv_cache is null"));
            return new ArrayList<>();
        }

        Hourglass hourglass = Progress_window.show(
                false,
                "wait, looking for similar items",
                20000,
                x,
                y,
                owner,
                logger);

        Feature_vector fv0 = fv_cache.get_from_cache_or_make(reference_item_path, null, true, owner, browser_aborter);
        if ( fv0 ==null)
        {
            hourglass.close();
            logger.log(Stack_trace_getter.get_stack_trace("FATAL: fv cannot be acquired"));
            return new ArrayList<>();
        }


        List<Path> to_be_compared = new ArrayList<>(paths);
        to_be_compared.remove(reference_item_path);
        if ( already_done!= null) to_be_compared.removeAll(already_done);
        List<Most_similar> most_similars = find_similars_of(
                reference_item_path,
                fv0,
                N,
                threshold,
                fv_cache_supplier,
                to_be_compared,
                count_pairs_examined,
                owner,
                browser_aborter);

        if ( already_done!= null) already_done.add(reference_item_path);
        if ( !show_images)
        {
            hourglass.close();
            return most_similars;
        }
        final double xx = x;
        final double yy = y;
        Runnable rr = new Runnable() {
            @Override
            public void run() {
                double xxx = xx;
                double yyy = yy;


                Image_window local = show_one_at(new Most_similar(reference_item_path,fv0,fv0,0.0),owner,xxx,yyy);
                Image_window.stage_group.add(local);
                yyy += H;
                for ( Most_similar ms : most_similars)
                {
                    local = show_one_at(ms,owner,xxx,yyy);
                    Image_window.stage_group.add(local);
                    xxx += W;
                }

                if ( Booleans.get_boolean_defaults_to_true(Feature.Show_can_use_ESC_to_close_windows.name(),owner))
                {
                    if (Popups.info_popup("After selecting one, you can use ESC to close these small windows one by one",owner,logger))
                    {
                        Booleans.set_boolean(Feature.Show_can_use_ESC_to_close_windows.name(), false,owner);
                    }
                }
            }
        };
        Jfx_batch_injector.inject(rr, logger);
        hourglass.close();
        return most_similars;
    }


    //**********************************************************
    public List<Most_similar> find_similars2(
            boolean quasi_same,
            Path reference_item_path,
            List<Path> already_done,//maybe null
            int N,
            boolean show_images,
            double threshold,
            Image_properties_RAM_cache image_properties_cache,
            Supplier<Feature_vector_cache> fv_cache_supplier,
            Window owner,
            double x, double y,
            AtomicLong count_pairs_examined,
            Aborter browser_aborter)
    //**********************************************************
    {
        Hourglass hourglass = Progress_window.show(
                false,
                "wait, looking for similar items",
                20000,
                x,
                y,
                owner,
                logger);

        if (paths.isEmpty())
        {
            hourglass.close();
            return new ArrayList<>();
        }

        Feature_vector_cache fv_cache = fv_cache_supplier.get();
        if ( fv_cache == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("FATAL: fv_cache is null"));
            hourglass.close();
            return new ArrayList<>();
        }
        Feature_vector fv0 = fv_cache.get_from_cache_or_make(reference_item_path, null, true, owner, browser_aborter);
        if ( fv0 ==null)
        {
            hourglass.close();
            return new ArrayList<>();
        }
        List<Path> to_be_compared = new ArrayList<>(paths);
        to_be_compared.remove(reference_item_path);
        if ( already_done!= null) to_be_compared.removeAll(already_done);
        List<Most_similar> most_similars = find_similars_of2(
                reference_item_path,
                fv0,
                quasi_same,
                N, threshold,
                image_properties_cache,
                fv_cache_supplier,
                to_be_compared,
                count_pairs_examined,
                owner,
                browser_aborter);

        if ( already_done!= null) already_done.add(reference_item_path);
        if ( !show_images)
        {
            hourglass.close();
            return most_similars;
        }
        final double xx = x;
        final double yy = y;
        Runnable rr = new Runnable() {
            @Override
            public void run() {
                double xxx = xx;
                double yyy = yy;

                Image_window local = show_one_at(new Most_similar(reference_item_path,fv0,fv0,0.0),owner,xxx,yyy);
                Image_window.stage_group.add(local);
                yyy += H;
                for ( Most_similar ms : most_similars)
                {
                    local = show_one_at(ms,owner,xxx,yyy);
                    Image_window.stage_group.add(local);
                    xxx += W;
                }

                if ( Booleans.get_boolean_defaults_to_true(Feature.Show_can_use_ESC_to_close_windows.name(),owner))
                {
                    if (Popups.info_popup("After selecting one, you can use ESC to close these small windows one by one",owner,logger))
                    {
                        Booleans.set_boolean(Feature.Show_can_use_ESC_to_close_windows.name(), false,owner);
                    }
                }
            }
        };
        Jfx_batch_injector.inject(rr, logger);
        hourglass.close();
        return most_similars;
    }


    //**********************************************************
    private Image_window show_one_at(Most_similar ms, Window owner, double x, double y)
    //**********************************************************
    {
        String s = String.format("%.4f",ms.similarity());
        Image_window returned = new Image_window(
                ms.path(), owner, x, y, W, H, s, false,path_list_provider,
                Optional.of(path_comparator_source.get_path_comparator()),
                new Aborter("dummy34",logger),logger);
        returned.stage.setX(x);
        returned.stage.setY(y);
        //logger.log("show_one_at path"+ms.path()+" x="+returned.the_Stage.getX()+" y="+returned.the_Stage.getY());

        if (show_vector_differences)
        {
            if ( ms.fv1() instanceof Feature_vector_double fvd1)
            {
                if (ms.fv2() instanceof Feature_vector_double fvd2)
                {
                    Vector_window vw = new Vector_window("Distance: " + ms.similarity(), owner, x, y, fvd1, fvd2, false, true, logger);
                }
            }
        }
        return returned;
    }


    //**********************************************************
    public List<Most_similar> find_similars_of(
            Path path0,
            Feature_vector fv0,
            int N,
            double threshold,
            Supplier<Feature_vector_cache> fv_cache_supplier,
            List<Path> targets,
            AtomicLong count_pairs_examined,
            Window owner,
            Aborter browser_aborter)
    //**********************************************************
    {
        List<Most_similar> returned =  new ArrayList<>();
        double min = Double.MAX_VALUE;

        for(Path path1 : targets)
        {
            if ( count_pairs_examined!= null) count_pairs_examined.incrementAndGet();

            Feature_vector fv1 = fv_cache_supplier.get().get_from_cache_or_make(path1, null,true, owner, browser_aborter);
            if (fv1 == null) continue; // server failure

            Double distance  = read_similarity_from_cache(path0, path1);
            if (distance == null)
            {
                    distance = fv0.distance(fv1);
                    logger.log("Distance " + distance + " between " + path0 + " and " + path1);
                    save_similarity_in_cache(distance, path0, path1);
            }

            if ( distance > threshold) // ignore if too far
            {
                logger.log("IGNORING, as Distance " + distance + " larger than max " + threshold + " for " + path1);

                continue;
            }

            Most_similar ms = new Most_similar(path1,fv0,fv1,distance);
            min = keep_N_closest(N,returned,ms, min);
            //count++;
            //if ( count % 100 == 0) logger.log("image compared count="+count);
        }
        return returned;
    }


    //**********************************************************
    public List<Most_similar> find_similars_of2(
            Path path0,
            Feature_vector fv0,
            boolean quasi_same,
            int N,
            double threshold,
            Image_properties_RAM_cache image_properties_cache,
            Supplier<Feature_vector_cache> fv_cache_supplier,
            List<Path> targets,
            AtomicLong count_pairs_examined,
            Window owner,
            Aborter browser_aborter)
    //**********************************************************
    {
        List<Most_similar> returned =  new ArrayList<>();
        double min = Double.MAX_VALUE;
        Image_properties ip0 = null;
        if ( quasi_same)
        {
            ip0 = image_properties_cache.get_from_cache(path0,null);
        }
        for(Path path1 : targets)
        {
            if ( count_pairs_examined!= null) count_pairs_examined.incrementAndGet();
            if ( quasi_same)
            {
                Image_properties ip1 = image_properties_cache.get_from_cache(path1,null);
                if ( ip0.w() != ip1.w()) continue;
                if ( ip0.h() != ip1.h()) continue;
            }

            Feature_vector fv1 = fv_cache_supplier.get().get_from_cache_or_make(path1, null,true, owner, browser_aborter);
            if (fv1 == null) continue; // server failure

            Double distance = null;
            distance = read_similarity_from_cache(path0, path1);
            if (distance == null) {
                distance = fv0.distance(fv1);
                if (dbg) logger.log("distance " + distance + " between " + path0 + " and " + path1);
                save_similarity_in_cache(distance, path0, path1);
            }
            if ( quasi_same)
            {
                if ( distance > 0) // i.e. not zero, for a double
                {
                    continue;
                }
            }
            if ( distance > threshold) // ignore if too far
            {
                continue;
            }

            Most_similar ms = new Most_similar(path1,fv0,fv1,distance);
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
        if ( local.size() > N) local.remove(local.size()-1);
        return local.get(0).similarity();
    }






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
