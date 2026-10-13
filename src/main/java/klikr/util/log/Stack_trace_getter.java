// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.log;

import klikr.util.execute.actor.Executor;

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
		if (Executor.full_thread_stack_dbg)
		{
			return sw.toString();
		}

		return sw.toString()+
				"\n"+Logger.warning+" If the exception occurred in a thread" +
				"\nand you want to trace where that thread was started," +
				"\nedit Executor.java and set full_thread_stack_dbg=true" +
				"\nNote that there is an additive per thread performance penalty" +
				"\nSo only full_thread_stack_dbg=false should go into releases";
	}
	
		
}
