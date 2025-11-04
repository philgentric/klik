// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.log;

import java.io.PrintWriter;
import java.io.StringWriter;

/* when debugging you may want to be able to print out the call stack
 * this is a wrapper for getting such a string error_message
 */
//**********************************************************
public class Stack_trace_getter
//**********************************************************
{
	//**********************************************************
	public static String get_stack_trace(String header)
	//**********************************************************
	{
		StringWriter sw = new StringWriter();
		Throwable t = new Throwable();
		t.printStackTrace(new PrintWriter(sw));		
		return header+" "+sw;
	}
	//**********************************************************
	public static String get_stack_trace_for_throwable(Throwable e)
	//**********************************************************
	{
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));		
		return sw.toString();
	}
	
		
}
