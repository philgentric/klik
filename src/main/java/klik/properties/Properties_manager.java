package klik.properties;

import klik.util.Key_value;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

//**********************************************************
public class Properties_manager 
//**********************************************************
{
	private static final boolean dbg = false;
	private static final String AGE = "_age";
	public static final int max = 30 ;

	private static Properties the_Properties = new Properties();
	Path f;
	
	//**********************************************************
	public Properties_manager(Path f_)
	//**********************************************************
	{
		f = f_;
		load_properties();
	}
	//**********************************************************
	public void store_properties()
	//**********************************************************
	{
		if ( dbg  == true ) System.out.println("store_properties()");
		FileOutputStream fos;
		try
		{
			//File f = get_file(PROPERTIES_FILENAME);
			if ( Files.exists(f) == false)
			{
				Files.createFile(f);
			}
			if ( Files.isWritable (f) == false )
			{
				//TODO: make this a dialog
				System.out.println("ALERT: cannot write properties in:"+f.toAbsolutePath());
				return;
			}

			fos = new FileOutputStream(f.toFile());
			the_Properties.store(fos,"no comment");
			fos.close();
			if ( dbg) System.out.println("ALL properties stored in:"+f.toAbsolutePath());
		}
		catch (Exception e)
		{
			System.out.println("store_properties Exception: " + e);
		}
	}
	//**********************************************************
	public void load_properties()
	//**********************************************************
	{
		if ( dbg ) System.out.println("load_properties()");
		FileInputStream fis;
		try
		{

			if ( Files.exists(f))
			{
				if (Files.isReadable(f) == false)
				{
					System.out.println("cannot read properties from:" + f.toAbsolutePath());
					return;
				}
				fis = new FileInputStream(f.toFile());
				the_Properties.load(fis);
				if ( dbg) System.out.println("properties loaded from:"+f.toAbsolutePath());
				fis.close();
			}

		}
		catch (Exception e)
		{
			System.out.println("load_properties Exception: " + e);
		}
	}


	/*
	 * low level API: use only for single ponctual items
	 */

	//**********************************************************
	public String get(String key)
	//**********************************************************
	{
		return (String) the_Properties.get(key);
	}
	//**********************************************************
	public String get(String key, String replace)
	//**********************************************************
	{
		return (String) the_Properties.getProperty(key, replace);
	}
	
	
	//**********************************************************
	private void clear()
	//**********************************************************
	{
		the_Properties.clear();		
	}
	
	/*
	 * imperative store: if a previous event with the same key had been saved,
	 * it will be erased
	 */
	//**********************************************************
	public void imperative_store(String key, String value, boolean and_save) 
	//**********************************************************
	{
		//(new Exception()).printStackTrace();
		if ( dbg) System.out.println("properties.store "+key+"="+value);


		the_Properties.put(key,value);	
		LocalDateTime now = LocalDateTime.now();
		the_Properties.put(key+AGE,	now.toString());

		if ( and_save) store_properties();
	}
	//**********************************************************
	public void remove(String key, String value)
	//**********************************************************
	{
		the_Properties.remove(key,value);
	}
	//**********************************************************
	public void clear(String key_base)
	//**********************************************************
	{
		for ( int i = 0; i < max ; i++)
		{
			String key = key_base + i;
			String value = get(key);
			if (value != null) remove(key,value);
		}
	}

	/*
	 * SMART API: for a given keyword (base-key), can store up to "max" items
	 * When max is reached, the oldest element is overwritten
	 */
	
	// list all stored values for a base-key
	//**********************************************************
	public List<String> get_values_for_base(String key_base)
	//**********************************************************
	{
		List<String> returned = new ArrayList<>();
		for ( int i = 0; i < max ; i++)
		{
			if ( get(key_base+i) != null ) returned.add(get(key_base+i));
		}
		Collections.sort(returned);
		return returned;
	}
	

	// list all stored values for a base-key
	//**********************************************************
	public List<Key_value> get_key_values_for_base(String key_base)
	//**********************************************************
	{
		List<Key_value> returned = new ArrayList<>();
		for ( int i = 0; i < max ; i++)
		{
			if ( get(key_base+i) != null )
			{
				String b = get(key_base+i);
				returned.add(new Key_value(key_base+i,b));
			}
		}
		Collections.sort(returned);
		return returned;
	}
	
	//**********************************************************
	public String get_most_recent_value_for_base(String key_base)
	//**********************************************************
	{
		String returned = null;
		LocalDateTime most_recent = null;
		for ( int i = 0; i < max ; i++)
		{
			String candidate = get(key_base+i);
			String date = get(key_base+i+AGE);
			LocalDateTime ld = LocalDateTime.parse(date);
			if ( most_recent == null)
			{
				most_recent = ld;
				returned = candidate;
			}
			else
			{
				if ( ld.isAfter(most_recent))
				{
					most_recent = ld;
					returned = candidate;
				}
			}
		}
		return returned;	
	}

	//**********************************************************
	public List<History_item> get_history_of(String key_base)
	//**********************************************************
	{
		List<History_item> returned = new ArrayList<>();
		for ( int i = 0; i < max ; i++)
		{
			String path = get(key_base+i);
			if ( path == null) continue;
			String date = get(key_base+i+AGE);
			returned.add(new History_item(path,date));
		}
		Collections.sort(returned,History_item.comparator_by_date);
		return returned;
	}


	// saves a value for a base-key, handling oldest-replacement silently
	//**********************************************************
	public boolean save_multiple(String key_base, String value)
	//**********************************************************
	{
		// avoid saving several times the same value	
		for (Entry<?, ?> e: the_Properties.entrySet())
		{
			String val = (String) e.getValue();
			if ( val.equals(value))
			{
				String local_key = (String) e.getKey();
				if ( local_key.startsWith(key_base)) return false;
			}
		}
		String key = get_one_empty_key_for_base(key_base);

		imperative_store(key,value,true);	
		return true;
	}

	// saves a value for a base-key, handling oldest-replacement silently
	//**********************************************************
	public boolean save_unico(String key, String value)
	//**********************************************************
	{
		imperative_store(key,value,true);
		return true;
	}

	
	
	// if there are no more available slots, 
	// this will ERASE the OLDEST value
	//**********************************************************
	private String get_one_empty_key_for_base(String key_base)
	//**********************************************************
	{
		for ( int i = 0; i < max ; i++)
		{
			if ( get(key_base+i) == null ) return key_base+i;
		}
		
		// erase the oldest one
		LocalDateTime oldest = null;
		String key = null;
		for ( int i = 0; i < max ; i++)
		{
			String date = get(key_base+i+AGE);
			if ( date == null)
			{
				key = key_base+i;
				break;
			}
			else
			{
				LocalDateTime ld = LocalDateTime.parse(date);
				if ( oldest == null) 
				{
					oldest = ld;
					key = key_base+i;
				}
				else
				{
					if (oldest.isAfter(ld) )
					{
						oldest = ld;
						key = key_base+i;
					}
				}			
			}
		}
		return key;
	}

	
	//**********************************************************
	public boolean remove_invalid_dir(Path dir)
	//**********************************************************
	{
		String to_be_removed_key;
		String to_be_removed_value;
		for (Entry<?, ?> e: the_Properties.entrySet())
		{
			String val = (String) e.getValue();
			if ( val.equals(dir.toAbsolutePath().toString()))
			{
				to_be_removed_key = (String) e.getKey();
				to_be_removed_value = val;
				boolean status = the_Properties.remove(to_be_removed_key, to_be_removed_value);
				store_properties();
				return status;
			}
		}
		return false;
	}

	
	/*
	 * unit test
	 */
	
	//**********************************************************
	public static void main(String[] deb)
	//**********************************************************
	{
		String TOTO = "toto";
		File f_ = new File("debil.txt");
		Properties_manager pm = new Properties_manager(f_.toPath());
		
		for (int i = 0; i < 15; i++)
		{
			String s =  pm.get_one_empty_key_for_base(TOTO);
			String value = "value for "+s;
			pm.imperative_store(s, value , true);
		}
		
		for (String s : pm.get_values_for_base(TOTO))
		{
			System.out.println(s);
		}
		
		String s =  pm.get_one_empty_key_for_base(TOTO);
		if ( s == null)
		{
			System.out.println("FATL 35343");
			return;
		}
		String value = "value for REPLACED "+s;
		pm.imperative_store(s, value , true);
		System.out.println(s+ " is now key for: "+ value);
	}
	
	

}