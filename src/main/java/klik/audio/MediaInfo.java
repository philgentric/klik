package klik.audio;

import klik.util.execute.Execute_command;
import klik.util.log.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class MediaInfo
//**********************************************************
{
    private static final String warning_mediainfo = "This feature requires mediainfo\n . for mac: brew install mediainfo\n . for any system, download from mediaarea.net\n";

    //**********************************************************
    public static List<String> get(Path path, Logger logger)
    //**********************************************************
    {
        List<String> cmds = new ArrayList<>();
        cmds.add("mediainfo");
        cmds.add(path.toAbsolutePath().toString());

        StringBuilder sb = new StringBuilder();
        String out = Execute_command.execute_command_list(cmds, path.getParent().toFile(), 2000, sb,logger);
        if ( out == null)
        {
            logger.log(warning_mediainfo);
            return List.of("nothing found");
        }
        String[] lines = out.split("\\R");

        return List.of(lines);
    }

    //**********************************************************
    public static String extract_release(List<String> l,Logger logger)
    //**********************************************************
    {
        String release = null;
        for(String s : l)
        {
            String marker = "Track name";
            if (s.contains(marker))
            {
                if (!s.contains("Track name/Position"))
                {
                    if (!s.contains("Track name/Total"))
                    {
                        logger.log("looking for:"+marker+" found:"+s);
                        int i = s.indexOf(marker);
                        if (i >= 0) {
                            release = s.substring(i + marker.length());
                            release = release.replace(":", "");
                            release = release.trim();
                            logger.log("looking for:"+marker+" found:"+release);
                        }
                    }
                }
            }
        }
        if ( release != null) return release;
        for(String s : l)
        {
            String marker = "Title";
            if (s.contains(marker))
            {
                logger.log("found ->"+s+"<-");
                int i = s.indexOf(marker);
                if (i >= 0) {
                    release = s.substring(i + marker.length());
                    release = release.replace(":", "");
                    release = release.trim();
                    logger.log("looking for:"+marker+" found:"+release);
                }
            }
        }
        return release;

    }
    //**********************************************************
    public static String extract_performer(List<String> l, Logger logger)
    //**********************************************************
    {
        String performer = null;
        for(String s : l)
        {
            String marker = "Performer";
            if (s.contains(marker)) {
                if (!s.contains("Album/Performer"))
                {
                    int i = s.indexOf(marker);
                    if (i >= 0) {
                        performer = s.substring(i + marker.length());
                        performer = performer.replace(":", "");
                        performer = performer.trim();
                    }
                }
            }
        }
        return performer;
    }
}
