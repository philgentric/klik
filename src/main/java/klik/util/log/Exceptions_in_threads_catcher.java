// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.log;

//**********************************************************
public class Exceptions_in_threads_catcher
//**********************************************************
{
	public static volatile int oops = 0;

	//**********************************************************
	public static void set_exceptions_in_threads_catcher(Logger logger)
	//**********************************************************
	{

		Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {

            if ( e.toString().contains("IndexOutOfBoundsException: Index -1 out of bounds for length"))
            {
                logger.log("❌❌❌ THREAD PANIC **no stack**:"+e);
                oops++;
                if ( oops > 100)
                {
                    logger.log("❌❌❌ THREAD PANIC exiting the application");
                    System.exit(-1);
                }
                return;
            }

            String trace = Stack_trace_getter.get_stack_trace_for_throwable(e);
            logger.log("❌❌❌ THREAD PANIC:"+trace);
			if ( thread == null)
			{
				logger.log(" Should not happen");
			}
			else
			{
				logger.log("PLEASE REPORT: "+Stack_trace_getter.get_stack_trace(thread.getName() + " occurred here"));
			}
        });

		//logger.log(Stack_trace_getter.get_stack_trace("Exceptions_in_threads_catcher initialized"));
	}

	/*
	 * unit test: crash with array end overrun
	 */

	//**********************************************************
	public static void main(String[] args)
	//**********************************************************
	{
		Logger l = new File_logger("Exceptions_in_threads_catcher test");
		set_exceptions_in_threads_catcher(l);
		
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		overrun();
		overrun();
	}

	//**********************************************************
	static void overrun()
	//**********************************************************
	{
		int[] deb = new int[10];
		
		int a = deb[10]; // intended to crash
		
		System.out.println("a="+a);
		
	}
}
