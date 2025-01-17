package klik.image_ml.image_similarity;

import klik.actor.Aborter;
import klik.util.files_and_paths.File_pair;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.files_and_paths.My_File;
import klik.util.log.Logger;

import java.util.List;
import java.util.concurrent.BlockingQueue;


//**********************************************************
public class Runnable_for_finding_duplicate_file_pairs_similarity implements Runnable
//**********************************************************
{
	private static final boolean dbg = true;
	public static final double THRESHOLD = 0.12;
	Logger logger;
	private final List<My_File> all_files;
	BlockingQueue<Similarity_file_pair> output_queue_of_same_in_pairs;
	Deduplication_by_similarity_engine deduplication_by_similarity_engine;
	private final Aborter private_aborter;
	private final Image_similarity image_similarity;
	private final boolean quasi_same;
	//**********************************************************
	public Runnable_for_finding_duplicate_file_pairs_similarity(
			boolean quasi_same,
			Deduplication_by_similarity_engine deduplication_by_similarity_engine_,
			List<My_File> all_files_,
			BlockingQueue<Similarity_file_pair> output_queue,
			Aborter private_aborter_,
			Logger logger_)
	//**********************************************************
	{
		this.quasi_same = quasi_same;
		all_files = all_files_;
		logger = logger_;
		private_aborter = private_aborter_;
		output_queue_of_same_in_pairs = output_queue;
		deduplication_by_similarity_engine = deduplication_by_similarity_engine_;
		image_similarity = new Image_similarity(deduplication_by_similarity_engine.browser,logger);

	}

	//**********************************************************
	@Override
	public void run()
	//**********************************************************
	{

		deduplication_by_similarity_engine.threads_in_flight.incrementAndGet();
		int duplicates_found_by_this_thread = 0;

		//boolean[] stop = new boolean[1];
		if ( dbg) logger.log("Runnable_for_finding_duplicate_file_pair_similarity RUN starts");

		int ignored = 0;
		for (My_File f : all_files)
		{
			if ( private_aborter.should_abort()) return;
			if (!Guess_file_type.is_file_an_image(f.file)) continue;
			List<Image_similarity.Most_similar> similars = image_similarity.find_similars(quasi_same, f.file.toPath(), 1, false, THRESHOLD, deduplication_by_similarity_engine.console_window.count_pairs_examined);


			if ( similars.isEmpty()) continue;
			deduplication_by_similarity_engine.duplicates_found.incrementAndGet();
			Image_similarity.Most_similar most_similar = similars.getFirst();
			if (dbg) logger.log("similars fond:\n     " + f.file.getAbsolutePath() + "\n    " + most_similar.path());

			File_pair pair= new File_pair(f.file, most_similar.path().toFile());
			Similarity_file_pair similarity_file_pair = new Similarity_file_pair(most_similar.similarity(), pair);
			deduplication_by_similarity_engine.console_window.count_duplicates.incrementAndGet();
			if (dbg) logger.log(" DUPLICATES:\n" +
					pair.f1().getAbsolutePath()  +"\n" +
					pair.f2().getAbsolutePath() );
			output_queue_of_same_in_pairs.add(similarity_file_pair);

		}


		logger.log("found duplicates:  "+deduplication_by_similarity_engine.duplicates_found.get());
		int remaining = deduplication_by_similarity_engine.threads_in_flight.decrementAndGet();
		if ( remaining != 0)
		{
			deduplication_by_similarity_engine.console_window.set_status_text("Thread found "+duplicates_found_by_this_thread+" duplicated pairs ... Search continues on "+ remaining +" threads!");
		}
		else
		{
			deduplication_by_similarity_engine.console_window.set_status_text("Total = "+ deduplication_by_similarity_engine.duplicates_found.get()+" duplicated pairs found, "+ignored+" ignored pairs (e.g. hidden files)");
		}

		//threads_in_flight.decrementAndGet();
	}



}
