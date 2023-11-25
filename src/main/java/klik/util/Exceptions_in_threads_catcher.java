package klik.util;

public class Exceptions_in_threads_catcher
{
	public static volatile boolean oops = false;

	public static void set_exceptions_in_threads_catcher(Logger logger)
	{

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread thread, Throwable e)
			{

				if ( e.toString().contains("IndexOutOfBoundsException: Index -1 out of bounds for length"))
				{
					logger.log("THREAD PANIC **no stack**:"+e);
					oops = true;
					return;

				}

				String trace = Stack_trace_getter.get_stack_trace_for_throwable(e);
				logger.log("THREAD PANIC:"+trace);
				logger.log(Stack_trace_getter.get_stack_trace(thread.getName()+" occurred here"));

			}
		});

	}

	/*
	 * unit test: crash with array end overrun
	 */
	
	public static void main(String[] args)
	{
		Logger l = new System_out_logger();
		set_exceptions_in_threads_catcher(l);
		
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		overrun();
		overrun();
	}
	
	static void overrun()
	{
		int[] deb = new int[10];
		
		int a = deb[10]; // intended to crash
		
		System.out.println("a="+a);
		
	}
}
