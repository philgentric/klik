package klik.image_ml.image_similarity;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.browser.classic.Folder_path_list_provider;
import klik.browser.icons.image_properties_cache.Image_properties_RAM_cache;
import klik.browser.virtual_landscape.Path_comparator_source;
import klik.util.files_and_paths.File_pair;
import klik.util.files_and_paths.File_with_a_few_bytes;
import klik.util.files_and_paths.Guess_file_type;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;


//**********************************************************
public class Runnable_for_finding_duplicate_file_pairs_similarity implements Runnable
//**********************************************************
{
	private static final boolean dbg = false;
	private static final double SPECIAL_SIMILARITY_THRESHOLD = 0.15;
	//private static final double SPECIAL_SIMILARITY_THRESHOLD = 0.024;
	Logger logger;
	private final List<File_with_a_few_bytes> all_files;
	BlockingQueue<Similarity_file_pair> output_queue_of_same_in_pairs;
	Deduplication_by_similarity_engine deduplication_by_similarity_engine;
	private final Aborter private_aborter;
	private final Image_similarity image_similarity;
	private final boolean quasi_same;
	private final Image_properties_RAM_cache image_properties_cache;
	private final Supplier<Image_feature_vector_cache> fv_cache_supplier;
	private final Window owner;
	//**********************************************************
	public Runnable_for_finding_duplicate_file_pairs_similarity(
			Image_properties_RAM_cache image_properties_cache,
			Supplier<Image_feature_vector_cache> fv_cache_supplier,
			Path_comparator_source path_comparator_source,
			boolean quasi_same,
			Deduplication_by_similarity_engine deduplication_by_similarity_engine_,
			List<File_with_a_few_bytes> all_files_,
			BlockingQueue<Similarity_file_pair> output_queue,
			Window owner,
			Aborter private_aborter_,
			Logger logger_)
	//**********************************************************
	{
		this.owner = owner;
		this.image_properties_cache = image_properties_cache;
		this.fv_cache_supplier = fv_cache_supplier;
		this.quasi_same = quasi_same;
		all_files = all_files_;
		logger = logger_;
		private_aborter = private_aborter_;
		output_queue_of_same_in_pairs = output_queue;
		deduplication_by_similarity_engine = deduplication_by_similarity_engine_;
		double x = deduplication_by_similarity_engine.owner.getX()+100;
		double y = deduplication_by_similarity_engine.owner.getY()+100;
		image_similarity = new Image_similarity(
				new Folder_path_list_provider(deduplication_by_similarity_engine.target_dir.toPath()),
				path_comparator_source,
				x,y,
				owner,
				deduplication_by_similarity_engine.private_aborter,
				logger);

	}

	//**********************************************************
	@Override
	public void run()
	//**********************************************************
	{
		deduplication_by_similarity_engine.threads_in_flight.incrementAndGet();
		int duplicates_found_by_this_thread = 0;
		if ( dbg) logger.log("Runnable_for_finding_duplicate_file_pair_similarity RUN starts");
		int ignored = 0;
		List<Path> already_done = new ArrayList<>();
		Image_feature_vector_cache fv_cache = fv_cache_supplier.get();
		if ( fv_cache == null)
		{
			logger.log(Stack_trace_getter.get_stack_trace("FATAL: fv_cache is null"));
			return;
		}

		for (File_with_a_few_bytes f : all_files)
		{
			if ( private_aborter.should_abort()) return;
			if (!Guess_file_type.is_file_an_image(f.file)) continue;
			double x = deduplication_by_similarity_engine.owner.getX()+100;
			double y = deduplication_by_similarity_engine.owner.getY()+100;
			List<Image_similarity.Most_similar> similars = image_similarity.find_similars(
					quasi_same,
					f.file.toPath(),
					already_done,
					1,
					false,
					SPECIAL_SIMILARITY_THRESHOLD,
					image_properties_cache,
					()->fv_cache,
					false,
					owner,
					x,
					y,
					deduplication_by_similarity_engine.console_window.count_pairs_examined,
					private_aborter
					);
			already_done.add(f.file.toPath());
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
