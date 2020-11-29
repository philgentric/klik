package klik.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;





//*******************************************************
public class System_out_logger implements Logger
//*******************************************************
{
	StringBuilder sb = new StringBuilder(500);
	//*******************************************************
	public System_out_logger()
	//*******************************************************
	{
	}
	//*******************************************************
	@Override
	public void log(boolean show_date_tag, boolean also_System_out_println, String s)
	//*******************************************************
	{
		System.out.println(s);
	}



	//*******************************************************
	@Override
	public void close()
	//*******************************************************
	{
	}


	//*******************************************************
	@Override
	public void close(boolean slow_but_clean)
	//*******************************************************
	{
	
	}



	@Override
	public void flush() 
	{
	}


}
