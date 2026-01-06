// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.change.undo;

import javafx.stage.Window;
import klikr.Shared_services;
import klikr.change.active_list_stage.Datetime_to_signature_source;
import klikr.look.my_i18n.My_I18n;
import klikr.properties.Non_booleans_properties;
import klikr.properties.Properties_manager;
import klikr.util.files_and_paths.old_and_new.Command;
import klikr.util.files_and_paths.old_and_new.Old_and_new_Path;
import klikr.util.files_and_paths.old_and_new.Status;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

//**********************************************************
public class Undo_core implements Datetime_to_signature_source
//**********************************************************
{
    private final static boolean dbg = false;
    private final Logger logger;
    static final boolean ultra_dbg = false;
    private static final String key_base = "undo_item_"; // name of items about this in properties file
    public static final String HOW_MANY = "_how_many";
    private static Properties_manager properties_manager;

    
    //**********************************************************
    public Undo_core(String undo_filename, Window owner, Logger logger_)
    //**********************************************************
    {
        logger  = logger_;
        String home = System.getProperty(Non_booleans_properties.USER_HOME);
        Path p = Paths.get(home, Non_booleans_properties.CONF_DIR, undo_filename);
        if( properties_manager == null)
        {
            properties_manager = new Properties_manager(p, "Undo DB", owner, Shared_services.aborter(), logger);
        }
        List<Undo_item> l = read_all_undo_items_from_disk();
        if (dbg) logger.log("undo store "+l.size()+" items loaded from "+undo_filename);
    }


    //**********************************************************
    boolean check_validity_internal(Undo_item undo_item)
    //**********************************************************
    {
        int valid = 0;
        for (Old_and_new_Path e : undo_item.oans)
        {
            Old_and_new_Path r = e.reverse_for_restore();
            if ( r.old_Path == null)
            {
                logger.log("nope, cannot undo this : "+r.to_string());
                continue;
            }
            if ( Files.exists(r.old_Path))
            {
                valid++;
            }
            else
            {
                logger.log("\n\n\nIGNORED: this undo item is now invalid, as the source file is not where mentioned in the record... it was probably moved since?\n\n\n");
            }
        }
        if ( valid == 0) return false;
        return true;
    }


    //**********************************************************
    @Override
    public Map<LocalDateTime, String> get_map_of_date_to_signature()
    //**********************************************************
    {
        List<Undo_item> ll = read_all_undo_items_from_disk();
        Map<LocalDateTime, String> returned = new HashMap<>();
        for ( Undo_item ui: ll)
        {
            returned.put(ui.time_stamp,ui.signature());
        }
        return returned;
    }



    //**********************************************************
    public Map<String, Undo_item> get_signature_to_undo_item_map()
    //**********************************************************
    {
        logger.log("reading undo items from disk");
        Map<String, Undo_item> returned = new HashMap<>();
        List<Undo_item> ll = read_all_undo_items_from_disk();
        for ( Undo_item ui: ll)
        {
            returned.put(ui.signature(),ui);
        }
        return returned;
    }




    //**********************************************************
    public void add(Undo_item ui)
    //**********************************************************
    {
        if ( dbg) logger.log("Undo_core add:"+ui.to_string());
        write_one_undo_item_to_disk(ui);
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
        if ( ultra_dbg) logger.log("extract_index from:->"+s+"<-");
        String ii = s.substring(s.indexOf(key_base)+key_base.length());
        if ( ultra_dbg) logger.log("extract_index from:"+ii);
        ii = ii.substring(0,ii.indexOf(HOW_MANY));
        if ( ultra_dbg) logger.log("extract_index from:"+ii);
        returned = UUID.fromString(ii);
        if ( ultra_dbg) logger.log("extract_index :"+returned);
        return returned;
    }

    //**********************************************************
    public void remove_all_undo_items_from_property_file(Window owner)
    //**********************************************************
    {
        String s1 = My_I18n.get_I18n_string("Warning_delete_undo", owner,logger);
        if (!Popups.popup_ask_for_confirmation("❗"+ s1, "", owner,logger)) return;

        Set<String> set = properties_manager.get_all_keys();
        for ( String k : set)
        {
            if ( !k.startsWith(key_base)) continue;
            if (!k.endsWith(HOW_MANY)) continue;

            UUID index = extract_index(k,logger);
            int number_of_oan = Integer.parseInt(properties_manager.get(k));
            if ( dbg) logger.log("\nremove_all_undo_items_from_property_file index = "+index+" has "+number_of_oan+ " oans");
            {
                String key = generate_key_for_datetime(index);
                String new_path_string = properties_manager.get(key);
                if ( new_path_string != null)
                {
                    if ( dbg) logger.log("removed: "+key);
                    properties_manager.remove(key);
                }
            }
            {
                String key = generate_key_for_how_many_oans(index);
                String new_path_string = properties_manager.get(key);
                if ( new_path_string != null)
                {
                    if ( dbg) logger.log("removed: "+key);
                    properties_manager.remove(key);
                }
            }
            for (int j = 0 ;j < number_of_oan; j++)
            {
                {
                    String key = generate_key_for_old_path(index, j);
                    String old_path_string = properties_manager.get(key);
                    if (old_path_string != null)
                    {
                        if ( dbg) logger.log("removed: "+key);
                        properties_manager.remove(key);
                    }
                }
                {
                    String key = generate_key_for_new_path(index, j);
                    String new_path_string = properties_manager.get(key);
                    if ( new_path_string != null)
                    {
                        if ( dbg) logger.log("removed: "+key);
                        properties_manager.remove(key);
                    }
                }
            }
        }
        properties_manager.store_properties(false);
    }

    /*
    //**********************************************************
    public static void show_all_events(Aborter aborter, Logger logger)
    //**********************************************************
    {
        IProperties local = Shared_services.main_properties();

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

        Info_stage.show_info_stage("Undos", l, null);
    }
*/
    //**********************************************************
    public List<Undo_item> read_all_undo_items_from_disk()
    //**********************************************************
    {
        if ( dbg) logger.log(("Undo_core READ"));
        Command cmd = Command.command_move;
        Status stt = Status.move_done;

        List<Undo_item> returned = new ArrayList<>();
        Set<String> set = properties_manager.get_all_keys();
        for ( String k : set)
        {
            if ( !k.startsWith(key_base)) continue;
            if (k.endsWith(HOW_MANY))
            {
                UUID index = extract_index(k,logger);
                int number_of_oan = Integer.parseInt(properties_manager.get(k));
                if ( dbg) logger.log("      undo item, index = "+index+" has "+number_of_oan+ " oans");
                String datetime_string = properties_manager.get(generate_key_for_datetime(index));
                if ( datetime_string == null)
                {
                    logger.log("WEIRD: datetime_string=null for: "+k);
                    continue;
                }
                List<Old_and_new_Path> l = new ArrayList<>();
                for (int j = 0 ;j < number_of_oan; j++)
                {
                    String old_path_string = properties_manager.get(generate_key_for_old_path(index,j));
                    if ( old_path_string == null)
                    {
                        logger.log("WEIRD: old_path_string=null with "+j);
                        continue;
                    }
                    String new_path_string = properties_manager.get(generate_key_for_new_path(index,j));
                    if ( new_path_string == null)
                    {
                        //l.add(new Old_and_new_Path(Path.of(old_path_string),null,cmd,stt,false));
                    }
                    else
                    {
                        l.add(new Old_and_new_Path(Path.of(old_path_string), Path.of(new_path_string), cmd, stt,false));
                    }
                }
                Undo_item undo_item = new Undo_item(l,LocalDateTime.parse(datetime_string),index,logger);
                if ( dbg) logger.log("undo item:"+undo_item.to_string());
                returned.add(undo_item);
            }
        }
        returned.sort(Undo_item.comparator_by_date);
        return returned;
    }



    //**********************************************************
    private void write_one_undo_item_to_disk(Undo_item undo_item)
    //**********************************************************
    {
        {
            String k = generate_key_for_how_many_oans(undo_item.index);
            String v = String.valueOf(undo_item.oans.size());
            properties_manager.add(k, v);
            if ( dbg) logger.log("       "+k+"="+v);
        }
        {
            String k = generate_key_for_datetime(undo_item.index);
            String v = undo_item.time_stamp.toString();
            properties_manager.add(k, v);
            if ( dbg)  logger.log("       "+k+"="+v);
        }
        int j = 0;
        for (Old_and_new_Path oan : undo_item.oans)
        {
            {
                String key_for_old_path = generate_key_for_old_path(undo_item.index, j);
                String string_for_old_path = oan.old_Path.toAbsolutePath().toString();
                properties_manager.add(key_for_old_path, string_for_old_path);
                if ( dbg) logger.log("       "+key_for_old_path+"="+string_for_old_path);
            }
            if ( oan.new_Path != null)
            {
                String key_for_new_path = generate_key_for_new_path(undo_item.index, j);
                String string_for_new_path = oan.new_Path.toAbsolutePath().toString();
                properties_manager.add(key_for_new_path, string_for_new_path);
                if ( dbg) logger.log("       "+key_for_new_path+"="+string_for_new_path);
            }
            j++;
        }
        properties_manager.store_properties(false);

    }

    //**********************************************************
    public void remove_undo_item(Undo_item undo_item)
    //**********************************************************
    {
        if ( dbg) logger.log("Undo_core REMOVE:"+undo_item.to_string());
        UUID index = undo_item.index;
        remove_file_stored_undo_item(undo_item, index);
    }

    //**********************************************************
    private void remove_file_stored_undo_item(Undo_item undo_item, UUID index)
    //**********************************************************
    {
        {
            String key = generate_key_for_how_many_oans(index);
            Object found = properties_manager.remove(key);
            if ( found == null)
            {
                logger.log("WEIRD error tried to remove "+key+" from properties but it was not there?");
            }
            else
            {
                //if ( dbg)
                    logger.log("✅  OK UNDO removed "+key+" from properties");
            }
        }
        {
            String key = generate_key_for_datetime(index);
            Object found = properties_manager.remove(key);
            if ( found == null)
            {
                logger.log("WEIRD error tried to remove "+key+" from properties but it was not there?");
            }
            else
            {
                if ( dbg) logger.log("✅  OK removed "+key+" from properties");
            }
        }
        int j = 0;
        for (Old_and_new_Path oan : undo_item.oans)
        {
            {
                String key_for_old_path = generate_key_for_old_path(index, j);
                Object found = properties_manager.remove(key_for_old_path);
                if ( found == null)
                {
                    logger.log("WEIRD error tried to remove "+key_for_old_path+" from properties but it was not there?");
                }
                else
                {
                    if ( dbg) logger.log("✅ OK removed "+key_for_old_path+" from properties");
                }

            }
            {
                String key_for_new_path = generate_key_for_new_path(index, j);
                Object found = properties_manager.remove(key_for_new_path);
                if ( found == null)
                {
                    logger.log("WEIRD error tried to remove "+key_for_new_path+" from properties but it was not there?");
                }
                else
                {
                    if ( dbg) logger.log("✅ OK removed "+key_for_new_path+" from properties");
                }
            }
            j++;
        }
        properties_manager.store_properties(false);
    }

    //**********************************************************
    public Undo_item get_most_recent()
    //**********************************************************
    {
        List<Undo_item> l = read_all_undo_items_from_disk();
        if ( l.isEmpty()) return  null;
        l.sort(Undo_item.comparator_by_date);
        return l.get(0);
    }

    //**********************************************************
    public void remove(String k)
    //**********************************************************
    {
        properties_manager.remove(k);
    }
}
