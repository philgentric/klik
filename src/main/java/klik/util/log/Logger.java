
package klik.util.log;

import java.io.*;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;

// there used to be several flavors, only the default System.out remains
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
	default void log_stack_trace(String s) {log(Stack_trace_getter.get_stack_trace(s));}
	//*******************************************************

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
	

	void log(boolean show_date_tag, boolean also_System_out_println, String s);
	void flush();
	void close(boolean clean);

	

	//*******************************************************
	static String add_time_stamp(String s)
	//*******************************************************
	{
		s = ZonedDateTime.now().toString() +" " + s;
		return s;
	}

	
	//*******************************************************
	static String get_2_decimal_formatted_double(double d)
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



}
