package klik.browser.comparators;

import klik.actor.Actor;
import klik.actor.Message;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//**********************************************************
public class Most_similar_actor implements Actor
//**********************************************************
{
    public final ConcurrentHashMap<Path_pair_int, Double> similarities;
    public final Logger logger;
    private final Map<Integer, String> dummy_names;
    public final List<Path> int_to_path;
    public final Map<Path,Integer> path_to_int;

    //**********************************************************
    public Most_similar_actor(List<Path> int_to_path, Map<Path,Integer> path_to_int, ConcurrentHashMap<Path_pair_int, Double> similarities, Map<Integer, String> dummy_names, Logger logger)
    //**********************************************************
    {
        this.similarities = similarities;
        this.dummy_names = dummy_names;
        this.logger = logger;
        this.int_to_path = int_to_path;
        this.path_to_int = path_to_int;
    }
    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Most_similar_message mm = (Most_similar_message)m;
        double min = Double.MAX_VALUE;
        int closest = -1;
        Path closest_path = null;
        logger.log("looking for closest to "+mm.p1);
        for( Path p2 : mm.images)
        {
            if ( mm.aborter.should_abort()) return "aborted";
            if ( dummy_names.containsKey(path_to_int.get(p2))) continue;
            int j = path_to_int.get(p2);
            logger.log("Most similar actor processing "+mm.p1+" vs "+p2);
            int i = path_to_int.get(mm.p1);
            Path_pair_int pair = Path_pair_int.get(i,j);
            Double diff = similarities.get(pair);
            if ( diff == null)
            {
                logger.log("WTF diff == null for "+mm.p1+" vs "+p2);
                continue;
            }
            if ( diff < min)
            {
                closest_path = p2;
                min = diff;
                closest = j;
            }
        }
        if ( closest_path != null)
        {
            logger.log(closest_path.getFileName().toString()+"=> dummy_name "+mm.p1.getFileName().toString()+" distance:"+min+" index:"+closest);
            dummy_names.put(closest,mm.p1.getFileName().toString()+min+closest);
            return closest_path.getFileName().toString();
        }
        return null;
    }

    @Override
    public String name() {
        return "Most_similar_actor";
    }
}
