package klik.deduplicate;

import klik.util.Guess_file_type_from_extension;
import klik.util.Logger;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


//**********************************************************
public class Duplicate_file_pairs_finder implements Runnable
//**********************************************************
{
	private static final boolean dbg = true;
	private static final boolean ultra_dbg = false;
	private static final int BUFFER_SIZE = 8192;//4096;//1024;
	Logger logger;
	int start;
	int end;
	List<My_File> files;
	ConcurrentLinkedQueue<File_pair> same_in_pairs;
	FX_popup popup;
	Deduplication deduplication;
	int ID;
	
	//**********************************************************
	public Duplicate_file_pairs_finder(Deduplication deduplication_, int ID_, int start_, int end_, List<My_File> files_,
									   ConcurrentLinkedQueue<File_pair> same_, FX_popup popup_, Logger logger_)
	//**********************************************************
	{
		ID = ID_;
		start = start_;
		end = end_;
		logger = logger_;
		files = files_;
		same_in_pairs = same_;
		popup = popup_;
		deduplication = deduplication_;
	}

	//**********************************************************
	@Override
	public void run()
	//**********************************************************
	{
		deduplication.remaining_threads.incrementAndGet();
		int this_thread_total = 0;
		int count = 0;
		for ( int i = 0; i < files.size(); i++ )
		{
			My_File fi = files.get(i);
			if ( ultra_dbg) logger.log(ID+" considering file:i="+i+" name="+fi.f.getAbsolutePath()+" start = "+start+" end="+end);
			for ( int j = start; j < end; j++ )
			{

				if (j >= i)
				{
					if ( ultra_dbg) logger.log(ID+" skipping file:j="+j);
					continue;
				}
				My_File fj = files.get(j);
				if ( ultra_dbg) logger.log(ID+" considering file:j="+j+" name="+fj.f.getAbsolutePath());

				count++;
				if ( count == 100000)
				{
					count = 0;
					//logger.log("Deduplicator: looking at ..."+f2.f.getAbsolutePath());
					popup.ping();
				}
				
				
				if ( files_are_same(fi,fj, logger))
				{

					this_thread_total++;

					if ( Guess_file_type_from_extension.is_file_a_image(fi.f) && Guess_file_type_from_extension.is_file_a_image(fj.f))
					{
						if (dbg) logger.log(ID+" SAME:"+fi.f.getAbsolutePath()+" - "+fj.f.getAbsolutePath());
						same_in_pairs.add(new File_pair(fi.f,fj.f, true));
					}
					else
					{
						if (dbg) logger.log(ID+" SAME but NOT images:"+fi.f.getAbsolutePath()+" - "+fj.f.getAbsolutePath());
						same_in_pairs.add(new File_pair(fi.f,fj.f, false));
						
					}


				}
				else
				{
					if ( ultra_dbg) logger.log(ID+" not same:"+fi.f.getAbsolutePath()+" - "+fj.f.getAbsolutePath());
					
				}
			}

		}
		if ( dbg) logger.log(ID+" total:"+this_thread_total);
		String s = "Thread "+ID+" found a total of "+ this_thread_total + " identical file pairs";
		deduplication.grand_total.addAndGet(this_thread_total);
		int remaining = deduplication.remaining_threads.decrementAndGet();
		if ( remaining != 0)
		{
			popup.pong("Thread "+ID+" found "+this_thread_total+" duplicated pairs ... Search continues on "+ remaining +" threads!");
		}
		else
		{
			popup.pong("Thread "+ID+" ends search ! Total = "+deduplication.grand_total.get()+" duplicated pairs found");
		}
	}

	//**********************************************************
	static boolean files_are_same(My_File mf1, My_File mf2, Logger logger)
	//**********************************************************
	{

		if ( mf1.size < 0) mf1.size = mf1.f.length();
		if ( mf1.size == 0)
		{
			logger.log("WARNING: empty file found:"+mf1.f.getAbsolutePath());
			return false;
		}
		long size1 = mf1.size;
		if ( mf2.size < 0) mf2.size = mf2.f.length();
		if ( mf2.size == 0)
		{
			logger.log("WARNING: empty file found:"+mf2.f.getAbsolutePath());
			return false;
		}
		long size2 = mf2.size;
		if ( size1 != size2 )
		{
			//if ( dbg ) logger.log("sizes differ "+ file_to_be_copied.getName()+" "+ori_s+" vs "+des_s);
			return false;
		}

		if (( mf1.hash != null) && (mf2.hash != null))
		{
			if ( Arrays.equals(mf1.hash,mf2.hash) == false) 
			{
				return false;
			}
		}

		// let us check, block per block
		// at the first sign of a difference, return false
		boolean returned = true;
		try
		{
			BufferedInputStream bis1 = new BufferedInputStream(new FileInputStream(mf1.f));
			BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(mf2.f));
			// TODO optimize : allocate once
			byte[] hashsrc = new byte[BUFFER_SIZE];
			byte[] hashdest = new byte[BUFFER_SIZE];
			//Arrays.fill(hashsrc,(byte)0); // dont need to do that
			boolean quit = false;
			boolean first_loop = true;
			for(;;)
			{
				int available_src = bis1.available();
				int available_dest = bis2.available();
				if ( available_src != available_dest )
				{
					//if ( dbg) logger.log("read sizes differ");
					returned = false;
					break;
				}
				if ( available_src < BUFFER_SIZE)
				{
					bis1.read(hashsrc, 0, available_src);
					bis2.read(hashdest, 0, available_dest);
					quit = true;
				}
				else
				{
					bis1.read(hashsrc, 0, BUFFER_SIZE);
					bis2.read(hashdest, 0, BUFFER_SIZE);
				}

				if ( first_loop)
				{
					first_loop = false;
					mf1.hash = hashsrc;
					mf2.hash = hashdest;					
				}
				if ( Arrays.equals(hashsrc,hashdest) == false) 
				{
					//if ( dbg ) logger.log("content differ");
					returned = false;
					break;
				}
				if (quit) break;
			}
			bis1.close();
			bis2.close();
			return returned;

		}
		catch(Exception ioe)
		{
			System.out.println(ioe);
			return false;
		}

	}

	


}
