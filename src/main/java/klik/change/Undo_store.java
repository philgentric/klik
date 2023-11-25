package klik.change;

import klik.files_and_paths.Command_old_and_new_Path;
import klik.files_and_paths.Old_and_new_Path;
import klik.files_and_paths.Status_old_and_new_Path;
import klik.properties.Properties_manager;
import klik.properties.Static_application_properties;
import klik.util.Logger;
import klik.util.info_stage.Info_stage;
import klik.util.info_stage.Line_for_info_stage;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;

//**********************************************************
public class Undo_store
//**********************************************************
{
    public static int max = 30;
    static final boolean dbg = true;
    private static final String key_base = "undo_item_"; // name of items about this in properties file
    public static final String HOW_MANY = "_how_many";
    private List<Undo_item> cache = new ArrayList<>();
    private final Properties_manager pm;
    private final Logger logger;

    //**********************************************************
    public Undo_store(Logger logger_)
    //**********************************************************
    {
        logger = logger_;
        pm = Static_application_properties.get_properties_manager(logger);
        read_undo_items(pm,key_base);
        if (dbg) logger.log("undo store "+cache.size()+" items loaded");
    }

    //**********************************************************
    private static String generate_key_for_old_path(UUID index, int j)
    //**********************************************************
    {
        return key_base+index+"_old_"+j;
    }
    //**********************************************************
    private static String generate_key_for_new_path(UUID index, int j)
    //**********************************************************
    {
        return key_base+index+"_new_"+j;
    }
    //**********************************************************
    private static String generate_key_for_datetime(UUID index)
    //**********************************************************
    {
        return key_base+index+"_datetime";
    }
    //**********************************************************
    private static String generate_key_for_how_many_oans(UUID index)
    //**********************************************************
    {
        return key_base+index+ HOW_MANY;
    }
    //**********************************************************
    private static UUID extract_index(String s, Logger logger)
    //**********************************************************
    {
        UUID returned;
        if ( dbg) logger.log("extract_index from:->"+s+"<-");
        String ii = s.substring(s.indexOf(key_base)+key_base.length());
        if ( dbg) logger.log("extract_index from:"+ii);
        ii = ii.substring(0,ii.indexOf(HOW_MANY));
        if ( dbg) logger.log("extract_index from:"+ii);
        returned = UUID.fromString(ii);
        if ( dbg) logger.log("extract_index :"+returned);
        return returned;
    }

    //**********************************************************
    public static void remove_all_indo_items_from_property_file(Logger logger)
    //**********************************************************
    {
        Properties_manager local = Static_application_properties.get_properties_manager(logger);

        Set<String> set = local.get_all_keys();
        for ( String k : set)
        {
            if ( !k.startsWith(key_base)) continue;
            if (k.endsWith(HOW_MANY))
            {
                UUID index = extract_index(k,logger);
                int number_of_oan = Integer.parseInt(local.get(k));
                if ( dbg) logger.log("      undo item, index = "+index+" has "+number_of_oan+ " oans");
                {
                    String key = generate_key_for_datetime(index);
                    String new_path_string = local.get(key);
                    if ( new_path_string != null) local.remove(key);
                }
                {
                    String key = generate_key_for_how_many_oans(index);
                    String new_path_string = local.get(key);
                    if ( new_path_string != null) local.remove(key);
                }
                for (int j = 0 ;j < number_of_oan; j++)
                {
                    {
                        String key = generate_key_for_old_path(index, j);
                        String old_path_string = local.get(key);
                        if (old_path_string != null) local.remove(key);
                    }
                    {
                        String key = generate_key_for_new_path(index, j);
                        String new_path_string = local.get(key);
                        if ( new_path_string != null) local.remove(key);
                    }
                }
            }
        }
    }

    public static void show_all_events(Logger logger)
    {
        Properties_manager local = Static_application_properties.get_properties_manager(logger);

        List<Line_for_info_stage> l = new ArrayList<>();
        l.add(new Line_for_info_stage(true,"Items that can be undone:"));
        Set<String> set = local.get_all_keys();
        for ( String k : set)
        {
            if ( !k.startsWith(key_base)) continue;

            if (k.endsWith(HOW_MANY))
            {
                StringBuilder sb = new StringBuilder();
                UUID index = extract_index(k,logger);

                int number_of_oan = Integer.parseInt(local.get(k));
                for (int j = 0 ;j < number_of_oan; j++)
                {

                    String key = generate_key_for_old_path(index, j);
                    String old_path_string = local.get(key);
                    sb.append(old_path_string);
                    sb.append(" ==> ");
                    key = generate_key_for_new_path(index, j);
                    String new_path_string = local.get(key);
                    sb.append(new_path_string);
                    sb.append(" ");
                }
                String key = generate_key_for_datetime(index);
                String datetime_string = local.get(key);
                sb.append(datetime_string);
                sb.append(" / ");
                key = generate_key_for_how_many_oans(index);
                String how_many_string = local.get(key);
                sb.append(how_many_string);
                sb.append(" / ");
                l.add(new Line_for_info_stage(false,sb.toString()));
            }
        }

        Info_stage.show_info_stage("About Klik",l, null);
    }

    //**********************************************************
    public List<Undo_item> read_undo_items(Properties_manager pm, String key_base)
    //**********************************************************
    {
        if ( dbg) logger.log("Undo_store READ");
        Command_old_and_new_Path cmd = Command_old_and_new_Path.command_move;
        Status_old_and_new_Path stt = Status_old_and_new_Path.move_done;

        List<Undo_item> returned = new ArrayList<>();
        Set<String> set = pm.get_all_keys();
        for ( String k : set)
        {
            if ( !k.startsWith(key_base)) continue;
            if (k.endsWith(HOW_MANY))
            {
                UUID index = extract_index(k,logger);
                int number_of_oan = Integer.parseInt(pm.get(k));
                if ( dbg) logger.log("      undo item, index = "+index+" has "+number_of_oan+ " oans");
                String datetime_string = pm.get(generate_key_for_datetime(index));
                if ( datetime_string == null)
                {
                    logger.log("WEIRD: datetime_string=null for: "+k);
                    continue;
                }
                List<Old_and_new_Path> l = new ArrayList<>();
                for (int j = 0 ;j < number_of_oan; j++)
                {
                    String old_path_string = pm.get(generate_key_for_old_path(index,j));
                    if ( old_path_string == null)
                    {
                        logger.log("WEIRD: old_path_string=null with "+j);
                        continue;
                    }
                    String new_path_string = pm.get(generate_key_for_new_path(index,j));
                    if ( new_path_string == null)
                    {
                        l.add(new Old_and_new_Path(Path.of(old_path_string),null,cmd,stt));
                    }
                    else
                    {
                        l.add(new Old_and_new_Path(Path.of(old_path_string), Path.of(new_path_string), cmd, stt));
                    }
                }
                Undo_item undo_item = new Undo_item(l,LocalDateTime.parse(datetime_string),index);
                if ( dbg) logger.log("  ![](../../../../../../../../Desktop/help/misc/Ultim/exhib/lacyspicets072111_p04_106.jpg)    undo item:"+undo_item.to_string());
                returned.add(undo_item);
            }
        }
        cache = returned;
        cache.sort(Undo_item.comparator_by_date);
        if ( dbg) show_cache();
        return returned;
    }

    //**********************************************************
    private void show_cache()
    //**********************************************************
    {
        logger.log("Undo_store cache:");
        for ( Undo_item ui : cache)
        {
            logger.log("        "+ui.to_string());
        }
    }


    //**********************************************************
    public static Undo_store get_Undo_store_instance(Logger logger)
    //**********************************************************
    {
        return new Undo_store(logger);
    }




    //**********************************************************
    public void add(Undo_item ui)
    //**********************************************************
    {
        if ( dbg) logger.log("Undo_store add:"+ui.to_string());

        cache.add(ui);
        cache.sort(Undo_item.comparator_by_date);

        if ( cache.size() > max)
        {
            Undo_item removed = cache.remove(cache.size()-1);
            pm.remove(generate_key_for_datetime(removed.index));
            pm.remove(generate_key_for_how_many_oans(removed.index));
            int j = 0;
            for (Old_and_new_Path oan : removed.oans)
            {
                pm.remove(generate_key_for_old_path(removed.index, j));
                pm.remove(generate_key_for_new_path(removed.index, j));
                j++;
            }
        }
        write_undo_items();
    }

    //**********************************************************
    private void write_undo_items()
    //**********************************************************
    {
        if ( dbg) logger.log("Undo_store WRITE:");

        for ( Undo_item undo_item : cache)
        {
            {
                String k = generate_key_for_how_many_oans(undo_item.index);
                String v = String.valueOf(undo_item.oans.size());
                pm.raw_put(k, v);
                if ( dbg) logger.log("       "+k+"="+v);
            }
            {
                String k = generate_key_for_datetime(undo_item.index);
                String v = undo_item.time_stamp.toString();
                pm.raw_put(k, v);
                if ( dbg)  logger.log("       "+k+"="+v);
            }
            int j = 0;
            for (Old_and_new_Path oan : undo_item.oans)
            {
                {
                    String key_for_old_path = generate_key_for_old_path(undo_item.index, j);
                    String string_for_old_path = oan.old_Path.toAbsolutePath().toString();
                    pm.raw_put(key_for_old_path, string_for_old_path);
                    if ( dbg) logger.log("       "+key_for_old_path+"="+string_for_old_path);
                }
                if ( oan.new_Path != null)
                {
                    String key_for_new_path = generate_key_for_new_path(undo_item.index, j);
                    String string_for_new_path = oan.new_Path.toAbsolutePath().toString();
                    pm.raw_put(key_for_new_path, string_for_new_path);
                    if ( dbg) logger.log("       "+key_for_new_path+"="+string_for_new_path);
                }
                j++;
            }
        }
        pm.store_properties();
    }

    //**********************************************************
    public void remove_after_undo_done(Undo_item undo_item)
    //**********************************************************
    {
        if ( dbg) logger.log("Undo_store REMOVE:"+undo_item.to_string());
        cache.remove(undo_item);
        UUID index = undo_item.index;
        remove_file_stored_undo_item(undo_item, index);
    }

    //**********************************************************
    private void remove_file_stored_undo_item(Undo_item undo_item, UUID index)
    //**********************************************************
    {
        {
            String key = generate_key_for_how_many_oans(index);
            Object found = pm.remove(key);
            if ( found == null)
            {
                logger.log("WEIRD error tried to remove "+key+" from properties but it was not there?");
            }
            else
            {
                if ( dbg) logger.log("OK removed "+key+" from properties");
            }
        }
        {
            String key = generate_key_for_datetime(index);
            Object found = pm.remove(key);
            if ( found == null)
            {
                logger.log("WEIRD error tried to remove "+key+" from properties but it was not there?");
            }
            else
            {
                if ( dbg) logger.log("OK removed "+key+" from properties");
            }
        }
        int j = 0;
        for (Old_and_new_Path oan : undo_item.oans)
        {
            {
                String key_for_old_path = generate_key_for_old_path(index, j);
                Object found = pm.remove(key_for_old_path);
                if ( found == null)
                {
                    logger.log("WEIRD error tried to remove "+key_for_old_path+" from properties but it was not there?");
                }
                else
                {
                    if ( dbg) logger.log("OK removed "+key_for_old_path+" from properties");
                }

            }
            {
                String key_for_new_path = generate_key_for_new_path(index, j);
                Object found = pm.remove(key_for_new_path);
                if ( found == null)
                {
                    logger.log("WEIRD error tried to remove "+key_for_new_path+" from properties but it was not there?");
                }
                else
                {
                    if ( dbg) logger.log("OK removed "+key_for_new_path+" from properties");
                }
            }
            j++;
        }
        pm.store_properties();
    }

    //**********************************************************
    public Undo_item get_most_recent()
    //**********************************************************
    {
        if ( cache.isEmpty()) return  null;
        cache.sort(Undo_item.comparator_by_date);
        return cache.get(0);
    }
}
