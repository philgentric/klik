package klik.machine_learning.song_similarity;

import javafx.stage.Window;
import klik.actor.Aborter;
import klik.machine_learning.feature_vector.Feature_vector;
import klik.machine_learning.feature_vector.Feature_vector_source;
import klik.properties.Non_booleans_properties;
import klik.util.execute.Execute_command;
import klik.util.files_and_paths.Extensions;
import klik.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Feature_vector_source_for_song_similarity implements Feature_vector_source
//**********************************************************
{
    private final static boolean ultra_dbg = false;
    Aborter aborter;
    //**********************************************************
    public Feature_vector_source_for_song_similarity(Aborter aborter)
    //**********************************************************
    {
        this.aborter  = aborter;
    }

    //**********************************************************
    public Feature_vector get_feature_vector(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        //logger.log("Feature_vector_source_for_song_similarity get_feature_vector");
        String wav_path = call_ffmpeg_to_convert_to_wav(path, owner, logger);
        if (wav_path == null)
        {
            logger.log("call_ffmpeg_to_convert_to_wav failed");
            return null;
        }

        String result = call_fpcalc_to_get_embedding(wav_path, logger);
        if (result == null)
        {
            new File(wav_path).delete();
            logger.log("call_fpcalc_to_get_embedding failed");
            return null;
        }
        if (result.isBlank())
        {
            new File(wav_path).delete();
            logger.log("call_fpcalc_to_get_embedding failed");
            return null;
        }
        if (!result.contains(Feature_vector_for_song.FINGERPRINT))
        {
            new File(wav_path).delete();
            logger.log("call_fpcalc_to_get_embedding failed");
            return null;
        }

        new File(wav_path).delete();
        return new Feature_vector_for_song(result,logger);
    }

    //**********************************************************
    private String call_fpcalc_to_get_embedding(String wav_path, Logger logger)
    //**********************************************************
    {
        List<String> cmds = new ArrayList<>();
        cmds.add("fpcalc");
        cmds.add("-raw");
        cmds.add(wav_path);

        StringBuilder sb = new StringBuilder();
        String out = Execute_command.execute_command_list(
                cmds,
                new File("."),
                1000*60,
                sb, logger);

        if ( ultra_dbg) logger.log(wav_path+"\nfpcalc output:\n"+out);
        return out;
    }

    //**********************************************************
    private String call_ffmpeg_to_convert_to_wav(Path path, Window owner, Logger logger)
    //**********************************************************
    {
        Path klik_trash = Non_booleans_properties.get_trash_dir(Path.of("").toAbsolutePath(),owner,logger);
        String base = Extensions.get_base_name(path.getFileName().toString());
        String wav_path = klik_trash.resolve(base+".wav").toString();

        //logger.log("tmp wav path is:"+wav_path);

        List<String> cmds = new ArrayList<>();
        cmds.add("ffmpeg");
        cmds.add("-y");
        cmds.add("-i");
        cmds.add(path.toString());
        cmds.add("-t");
        cmds.add("120");
        cmds.add("-ar");
        cmds.add("44100");
        cmds.add("-acodec");
        cmds.add("pcm_s16le");
        cmds.add("-ac");
        cmds.add("2");
        cmds.add(wav_path);


        StringBuilder sb = new StringBuilder();
        String out = Execute_command.execute_command_list(
                cmds,
                new File("."),
                1000*60,
                sb, logger);

        if ( sb.toString().contains("Error while decoding stream"))
        {
            logger.log("WARNING: ffmpeg could not decode "+path);
            return null;
        }

        if ( ultra_dbg) logger.log("ffmpeg output: "+sb);
        return wav_path;
    }


}
