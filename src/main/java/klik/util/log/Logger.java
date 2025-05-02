
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
	default void log(String s)
	//*******************************************************
	{
		log(true,s);
	}
	//*******************************************************
	default void log(boolean also_System_out_println, String s)
	//*******************************************************
	{
		log(also_System_out_println,s);
	}
	//*******************************************************
	default void log_stack_trace(String s) {log(Stack_trace_getter.get_stack_trace(s));}
	//*******************************************************

	//**********************************************************
	default void log_exception(String header, Exception e)
	//**********************************************************
	{
		String err = header;
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		err += "\n"+sw.toString();
		log(true, err);
	}

}
