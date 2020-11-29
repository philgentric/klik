package klik.util;

public class Exceptions_in_threads_catcher
{
	public static volatile boolean oops = false;

	public static void set_exceptions_in_threads_catcher(Logger logger)
	{

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) 
			{

				if ( e.toString().contains("IndexOutOfBoundsException: Index -1 out of bounds for length"))
				{
					logger.log("THREAD PANIC **no stack**:"+e.toString());
					oops = true;
					return;

				}


				String trace = Stack_trace_getter.get_stack_trace_for_throwable(e);
				logger.log("THREAD PANIC:"+trace);
				
				if ( trace.contains("java.lang.NoClassDefFoundError"))
				{
					if ( trace.contains("jfx") || trace.contains("javafx"))
					{
						logger.log("\n\n\n*WARNING**WARNING**WARNING**WARNING**WARNING**WARNING**WARNING**WARNING**WARNING**WARNING**WARNING**WARNING**WARNING*\n\nIt seems that your project settings may be missing VM arguments to specify some javafx dependencies");
						logger.log("check the VM args and eventually add this:\n-Djavafx.verbose=true -Dprism.verbose=true --module-path $PATH_TO_FX --add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web,javafx.swing");
						logger.log("to define $PATH_TO_FX in your .profile there should be something like this:\n" + 
								"JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.0.2.jdk/Contents/Home\n" + 
								"export JAVA_HOME;\n" + 
								"PATH_TO_FX=/Users/pgentric/KODE/java/external_jars/javafx-sdk-11.0.1/lib\n" + 
								"export PATH_TO_FX;\n\n\n");
					}
				}

				logger.log(Stack_trace_getter.get_stack_trace(t.getName()+" occurred here"));

			}
		});

	}

	/*
	 * unit test: crash with array end overrun
	 */
	
	public static void main(String args[])
	{
		
		//Logger l = new Disruptor_logger("debil.txt");
		Logger l = new System_out_logger();
		set_exceptions_in_threads_catcher(l);
		
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		overrun();
		overrun();
	}
	
	static void overrun()
	{
		int deb[] = new int[10];
		
		int a = deb[10];
		
		System.out.println("a="+a);
		
	}
}
