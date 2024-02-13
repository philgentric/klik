package klik.level2.deduplicate;

import klik.actor.Aborter;
import klik.files_and_paths.Guess_file_type;
import klik.files_and_paths.My_File;
import klik.files_and_paths.Name_cleaner;
import klik.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


//**********************************************************
public class Runnable_for_finding_duplicate_file_pairs implements Runnable
//**********************************************************
{
	private static final boolean dbg = false;
	public static final boolean ultra_dbg = false;
	Logger logger;
	private final List<My_File_and_status> files  = new ArrayList<>();
	BlockingQueue<File_pair> output_queue_of_same_in_pairs;
	Deduplication_engine deduplication_engine;
	private AtomicBoolean is_finished = new AtomicBoolean(false);
	private final Aborter private_aborter;
	//**********************************************************
	public Runnable_for_finding_duplicate_file_pairs(
			Deduplication_engine deduplication_,
			List<My_File> files_,
			BlockingQueue<File_pair> output_queue,
			Aborter private_aborter_,
			Logger logger_)
	//**********************************************************
	{
		logger = logger_;
		private_aborter = private_aborter_;
		for ( My_File mf: files_) files.add(new My_File_and_status(mf));
		output_queue_of_same_in_pairs = output_queue;
		deduplication_engine = deduplication_;
	}

	//**********************************************************
	@Override
	public void run()
	//**********************************************************
	{
		deduplication_engine.remaining_threads.incrementAndGet();
		int duplicates_found_by_this_thread = 0;
		deduplication_engine.get_interface().set_total_files_to_be_examined(files.size());

		//boolean[] stop = new boolean[1];
		if ( dbg) logger.log("Runnable_for_finding_duplicate_file_pairs RUN starts");

		int ignored = 0;
		int target = (files.size()* files.size()-files.size())/2;
		for ( int i = 0; i < files.size(); i++ )
		{
			if (ultra_dbg) logger.log("Runnable_for_finding_duplicate_file_pairs i="+i);

			if ( private_aborter.should_abort())
			{
				if (dbg) logger.log("Runnable_for_finding_duplicate_file_pairs abort");
				is_finished.set(true);
				return;
			}

			My_File_and_status fi = files.get(i);

			if ( fi.to_be_deleted )
			{
				if ( ultra_dbg) logger.log(" skipping1 file:i="+i);//+" name="+fi.my_file.file.getAbsolutePath()+" as it is already scheduled for deletion");
				continue;
			}
			if ( ultra_dbg) logger.log(" considering file:i="+i);//+" name="+fi.my_file.file.getAbsolutePath()+" start = "+start+" end="+end);

			deduplication_engine.get_interface().increment_examined();

			for ( int j = i+1; j < files.size(); j++ )
			{
				if ( ultra_dbg) logger.log("Runnable_for_finding_duplicate_file_pairs j="+j);

				if ( private_aborter.should_abort())
				{
					logger.log("Runnable_for_finding_duplicate_file_pairs abort");
					is_finished.set(true);
					return;
				}

				My_File_and_status fj = files.get(j);
				target--;
				if ( fj.to_be_deleted )
				{
					if ( ultra_dbg) logger.log(" skipping2 file:j="+j);//+" name="+fj.my_file.file.getAbsolutePath()+" as it is already scheduled for deletion");
					//ignored++;
					continue;
				}
				if ( ultra_dbg) logger.log("       considering "+files.size()+" files... i="+i+" j="+j);//+" name="+fj.my_file.file.getAbsolutePath());

				if ( ! My_File.files_have_same_content(fi.my_file,fj.my_file, private_aborter, logger))
				{
					if ( ultra_dbg) logger.log(" not same CONTENT:"+fi.my_file.file.getAbsolutePath()+" - "+fj.my_file.file.getAbsolutePath());
				}
				else
				{
					duplicates_found_by_this_thread++;
					if ( dbg) logger.log("duplicate fond:\n     "+fi.my_file.file.getAbsolutePath()+"\n    "+fj.my_file.file.getAbsolutePath());

					decide_which_to_delete(fi, fj);
					deduplication_engine.get_interface().increment_to_be_deleted();
					//logger.log(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>finder 1 more TO_BE_DELETED = "+duplicates_found_by_this_thread);

					if (dbg) logger.log(" SAME:\n" +
							i+"= "+fi.my_file.file.getAbsolutePath()+" to be deleted: "+fi.to_be_deleted+"\n" +
							j+"= "+fj.my_file.file.getAbsolutePath()+" to be deleted: "+fj.to_be_deleted);
					boolean is_image = Guess_file_type.is_this_path_an_image(fi.my_file.file.toPath());
					output_queue_of_same_in_pairs.add(new File_pair(fi,fj, is_image));
					target -= files.size()-1-j;

				}

			}
		}

		logger.log("target is: "+target);
		logger.log("to be deleted: "+duplicates_found_by_this_thread);


		if ( dbg) logger.log(" total:"+duplicates_found_by_this_thread);
		deduplication_engine.duplicates_found.addAndGet(duplicates_found_by_this_thread);
		int remaining = deduplication_engine.remaining_threads.decrementAndGet();
		if ( remaining != 0)
		{
			deduplication_engine.get_interface().set_status_text("Thread found "+duplicates_found_by_this_thread+" duplicated pairs ... Search continues on "+ remaining +" threads!");
		}
		else
		{
			deduplication_engine.get_interface().set_status_text("Total = "+ deduplication_engine.duplicates_found.get()+" duplicated pairs found, "+ignored+" ignored pairs (e.g. hidden files)");
		}

		is_finished.set(true);
	}


	//**********************************************************
	private void decide_which_to_delete(My_File_and_status fi, My_File_and_status fj)
	//**********************************************************
	{
		// first we check if the one of the file names has been cleaned
		if(Name_cleaner.clean(fi.my_file.file.getName(),true,logger).equals(fi.my_file.file.getName()))
		{
			// i name is clean
			if(! Name_cleaner.clean(fj.my_file.file.getName(),true,logger).equals(fj.my_file.file.getName()))
			{
				// i name is clean, and j is not ..
				fi.to_be_deleted=false;
				fj.to_be_deleted=true;
				return;
			}
		}
		else
		{
			if(Name_cleaner.clean(fj.my_file.file.getName(),true,logger).equals(fj.my_file.file.getName()))
			{
				// j name is clean, and i is not ..
				fi.to_be_deleted=true;
				fj.to_be_deleted=false;
				return;

			}
		}

		// in order to decide which file to delete we compare the path length
		int lenght_of_path_for_i = fi.my_file.file.getAbsolutePath().length();
		int lenght_of_path_for_j = fj.my_file.file.getAbsolutePath().length();

		if ( lenght_of_path_for_i == lenght_of_path_for_j)
		{
			if (fi.my_file.file.getName().contains("_") )
			{
				if ( fj.my_file.file.getName().contains("_"))
				{
					// both have underscore(s): delete i
					fi.to_be_deleted=true;
					logger.log("i to be deleted as both names have underscores");
				}
				else
				{
					// no underscore in j's name: delete j
					fj.to_be_deleted=true;
					logger.log("j to be deleted as name has no underscores, and i has");
				}
			}
			else
			{
				// no underscore in i's name:
				if ( fj.my_file.file.getName().contains("_"))
				{
					// j has underscore(s): delete i
					fi.to_be_deleted=true;
					logger.log("i to be deleted as name has no underscores, and j has");
				}
				else
				{
					// none have underscores  ... delete j
					fj.to_be_deleted=true;
					logger.log("j to be deleted as none have underscores");
				}
			}
		}
		else if ( lenght_of_path_for_i > lenght_of_path_for_j)
		{
			fi.to_be_deleted=true;
			logger.log("i to be deleted as its path is longer");
		}
		else
		{
			logger.log("j to be deleted as its path is longer");
			fj.to_be_deleted=true;
		}
	}


	//**********************************************************
	public boolean is_finished()
	//**********************************************************
	{
		return is_finished.get();
	}
}
