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
}
