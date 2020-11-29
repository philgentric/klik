
package klik.util;

import java.io.*;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;



/* Logger allows to print traces to console and to a file
* printing to console is optional
* date/time tag is optional
* printing to file is based on using a queue as a buffer and a thread for IOs
* so that logging will not slow down too much the caller
* ... as long as the queue does not grow out of reasonable ;-)
* 
* note that printing to console will work EVEN after close()
* 
* There are 2 flavours of instances:
* 1) the fast+expensive one: it holds a queue, a thread and a permanently open file descriptor
* 2) the slow+cheap one: it opens/closes its log file every time and reuses a single queue and thread for all instances
* 
*/

//**********************************************************
public interface Logger
//**********************************************************
{
	//*******************************************************
	public default void log(String s)
	//*******************************************************
	{
		log(false,true,s);
	}
	//*******************************************************
	public default void log(boolean also_System_out_println, String s)
	//*******************************************************
	{
		log(true,also_System_out_println,s);
	}
	//*******************************************************
	public default void close()
	//*******************************************************
	{
		close(true);
	}
	//**********************************************************
	public default void log_exception(String header, Exception e)
	//**********************************************************
	{
		String err = header;
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		err += "\n"+sw.toString();
		log(true, err);
	}
	

	public void log(boolean show_date_tag, boolean also_System_out_println, String s);
	public void flush();
	public void close(boolean clean);

	


	//*******************************************************
	public static void system_out(String string) 
	//*******************************************************
	{
		System.out.println(string);
		
	}


	//*******************************************************
	public static String add_time_stamp(String s) 
	//*******************************************************
	{
		s = ZonedDateTime.now().toString() +" " + s;
		//s = Joda_utilities.get_a_date_string_of_now_millisec()+" " + s;
		return s;
	}
	/*
	 * if the filename is not absolute the local flag is checked:
	 * if true the log file will be created in the directory where the original command was started
	 * else a complicated/stupid logic will be used to find a "tmp" 
	 */
	//*******************************************************
	public static String make_absolute_file_path(boolean local, String filename) 
	//*******************************************************
	{
		if ( new File(filename).isAbsolute()  == false)
		{
			if ( local)
			{
	            return System.getProperty("user.dir")+System.getProperty("file.separator")+filename;
			}
		}
		return filename;
	}
	
	//*******************************************************
	public static String get_2_decimal_formatted_double(double d)
	//*******************************************************
	{
		DecimalFormat formatter = new DecimalFormat("#0.00");
		if ( d > 100000.0)
		{
			formatter = new DecimalFormat("0.##E0");
		}
		String s = null;
		try
		{
			s = formatter.format(d);
			return s;
		}
		catch (ArithmeticException eee)
		{
			return ""+eee;
		}
	}	

	//*******************************************************
	public static String short_double_string(double d)
	//*******************************************************
	{
		return get_2_decimal_formatted_double(d);
	}
	//*******************************************************
	public static void erase_file(File f)
	//*******************************************************
	{
		BufferedWriter local;
		try 
		{
			local = new BufferedWriter(new FileWriter(f,false)); // append is false => create new
			local.flush();
			local.close();
		}
		catch (IOException e)
		{
			System.out.println("FATAL: Logger: open failed for:"+f.getAbsolutePath());
		}
		//System.out.println("erased log file:"+f.getAbsolutePath());
	}
	
	
	

	


}
