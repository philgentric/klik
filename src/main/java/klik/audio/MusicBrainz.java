package klik.audio;

import javafx.scene.image.Image;
import javafx.stage.Window;
import klik.util.execute.actor.Aborter;
import klik.util.files_and_paths.*;
import klik.util.files_and_paths.old_and_new.Command;
import klik.util.files_and_paths.old_and_new.Old_and_new_Path;
import klik.util.files_and_paths.old_and_new.Status;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class MusicBrainz
//**********************************************************
{
    private static final boolean ultra_dbg = false;
    private static final String user_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36";
    //**********************************************************
    public static String find_release_UUID(String artist, String release, Logger logger)
    //**********************************************************
    {

        String query = "artist:" + artist + " AND release:" + release;
        StringBuilder response = new StringBuilder();

        URL url = null;
        try {
            String urlStr = "https://musicbrainz.org/ws/2/release/?query=" +
                    URLEncoder.encode(query, "UTF-8") + "&fmt=json";
            url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", user_agent);
            int responseCode = conn.getResponseCode();
            if ( ! (responseCode == HttpURLConnection.HTTP_OK) )
            {
                logger.log("ERROR response code: " + responseCode);
                return null;
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
        }
        catch (IOException e)
        {
            logger.log("find_release_UUID failed"+e+"\nurl="+url);
            return null;
        }

        //logger.log(artist+" "+ release + " "+ response);
        // response format is:
        // {"created":"2025-09-19T09:26:40.808Z","count":25,"offset":0,
        // "releases":[{"id":"4342d749-2c18-46f8-a1dd-490b6a2f817a
        String marker = "releases\":[{\"id\":\"";
        String big_string = response.toString();
        int index = big_string.indexOf(marker);
        if ( index > 0)
        {
            big_string = big_string.substring(index+marker.length());
            int index_2 = big_string.indexOf("\"");
            big_string = big_string.substring(0,index_2);
            return big_string;

        }
        return null;
    }

    //**********************************************************
    public static String get_cover_art_URL(String artist, String release, Logger logger)
    //**********************************************************
    {
        String release_UUID =  find_release_UUID( artist, release,  logger);

        String urlStr = "https://coverartarchive.org/release/" + release_UUID;
        StringBuilder response = new StringBuilder();
        URL url = null;
        try {
            url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", user_agent);
            connection.setInstanceFollowRedirects(true);
            int responseCode = connection.getResponseCode();
            if ( ! (responseCode == HttpURLConnection.HTTP_OK) )
            {
                logger.log("Unexpected response code: " + responseCode);
                return null;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null)
            {
                response.append(line);
            }
            in.close();

        }
        catch (IOException e)
        {
            logger.log("get_cover_art_URL failed, "+e+"\nurl="+url);
            return null;
        }

        //logger.log(response.toString());

        // the format is:
        //{"images": [{"approved": true, "back": false, "comment": "", "edit": 124793262, "front": true, "id": 41713677638,
        // "image": "https://coverartarchive.org/release/62710482-0fcc-4059-9833-75ec7c93c058/41713677638.jpg",
        // "thumbnails": {"1200": "https://coverartarchive.org/release/62710482-0fcc-4059-9833-75ec7c93c058/41713677638-1200.jpg",
        // "250": "https://coverartarchive.org/release/62710482-0fcc-4059-9833-75ec7c93c058/41713677638-250.jpg",
        // "500": "https://coverartarchive.org/release/62710482-0fcc-4059-9833-75ec7c93c058/41713677638-500.jpg",
        // "large": "https://coverartarchive.org/release/62710482-0fcc-4059-9833-75ec7c93c058/41713677638-500.jpg",
        // "small": "https://coverartarchive.org/release/62710482-0fcc-4059-9833-75ec7c93c058/41713677638-250.jpg"},
        // "types": ["Front"]}],
        // "release": "https://musicbrainz.org/release/62710482-0fcc-4059-9833-75ec7c93c058"}

        String marker = "\"small\"";// sometimes there is no space
        String icon_url = response.toString();
        logger.log("icon_url: "+icon_url);
        int index = icon_url.indexOf(marker);
        if ( index >= 0)
        {
            icon_url = icon_url.substring(index+marker.length());
            logger.log("icon_url: "+icon_url);
            icon_url = icon_url.substring(icon_url.indexOf(":")+1);
            logger.log("icon_url: "+icon_url);
            icon_url = icon_url.substring(icon_url.indexOf("\"")+1);
            logger.log("icon_url: "+icon_url);
            icon_url = icon_url.substring(0,icon_url.indexOf("\""));
            logger.log("icon_url: "+icon_url);
            icon_url = icon_url.trim();
            logger.log("icon_url: "+icon_url);
            return  icon_url;
        }
        return null;
    }

    //**********************************************************
    private static boolean download_bloody_icon(String url_string, File icon_file,Logger logger)
    //**********************************************************
    {
        try {
            URL url = new URL(url_string);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", user_agent);
            connection.setInstanceFollowRedirects(true);
            int responseCode = connection.getResponseCode();
            if ( ! (responseCode == HttpURLConnection.HTTP_OK) )
            {
                logger.log("Error response code: " + responseCode);
                if ( responseCode == 301)
                {
                    String redirectUrl = connection.getHeaderField("Location");
                    return download_bloody_icon(redirectUrl,icon_file,logger);
                }
                return false;
            }

            InputStream in = connection.getInputStream();
            byte[] imageData = in.readAllBytes();

            try (FileOutputStream outputStream = new FileOutputStream(icon_file))
            {
                outputStream.write(imageData);
            }
            in.close();
            return true;
        }
        catch (IOException e)
        {
            logger.log("get_cover_art_URL failed, "+e+"\nurl="+url_string);
            return false;
        }
    }
    //**********************************************************
    public static Image download_icon(String artist, String release, Path icon_folder, Window owner, Logger logger)
    //**********************************************************
    {
        String image_URL = get_cover_art_URL(artist,release,logger);
        if ( image_URL == null) return null;

        File icon_file = make_icon_file(artist,release,icon_folder,owner,logger);

        download_bloody_icon(image_URL,icon_file,logger);

        try {
            InputStream is = new FileInputStream(icon_file.getAbsolutePath());
            Image returned = new Image(is);
            is.close();
            logger.log("✅ icon acquired from:"+image_URL+" saved as: "+icon_file.getAbsolutePath());
            return returned;
        }
        catch (IOException e)
        {
            logger.log("❌ WARNING icon acquired from:"+image_URL+" NOT saved as: "+icon_file+" "+e);

        }
        return null;
    }

    //**********************************************************
    public static String make_name(String artist, String release, Window owner,Logger logger)
    //**********************************************************
    {
        String name = artist.replaceAll(" ","_")+"_"+release.replaceAll(" ","_");
        name = Filename_sanitizer.sanitize(name,logger);
        return name;
    }

    //**********************************************************
    private static File make_icon_file(String artist, String release, Path icon_folder, Window owner,Logger logger)
    //**********************************************************
    {
        String name = make_name(artist,release,owner,logger);
        name += ".jpg";

        //Path icon_cache_dir = Static_files_and_paths_utilities.get_cache_dir(Cache_folder.klik_icon_cache, owner, logger);
        return new File(icon_folder.toFile(),name);
    }

    //**********************************************************
    private static Image get_icon_internal(String artist, String release, Path icon_folder, Window owner, Logger logger)
    //**********************************************************
    {

        File f = make_icon_file(artist,release,icon_folder,owner,logger);
        try (FileInputStream input_stream = new FileInputStream(f))
        {
            Image image = new Image(input_stream);
            return image;
        }
        catch (FileNotFoundException e)
        {
            // this always happens, the first time
            if (ultra_dbg)
                logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }

        Image icon = download_icon(artist,release,icon_folder,owner,logger);
        return icon;
    }

    //**********************************************************
    public static Image get_icon(
            Path path,
            String performer,
            String release,
            Window owner, Logger logger)
    //**********************************************************
    {
        Image icon;
        if ( (performer != null) && (release != null))
        {
            logger.log("performer:"+ performer + " release:"+ release);
            Path icon_folder = path.getParent();
            icon = MusicBrainz.get_icon_internal(performer, release, icon_folder, owner, logger);
        }
        else
        {
            icon = null;
        }
        return icon;
    }

    //**********************************************************
    public static boolean improve_file_name(
            Path path,
            String performer,
            String release,
            Window owner, Logger logger)
    //**********************************************************
    {
        if ( (performer != null) && (release != null))
        {
            logger.log("performer:"+ performer + " release:"+ release);

            String candidate_name =  MusicBrainz.make_name(performer, release, owner, logger);
            String extension = Extensions.get_extension(path.getFileName().toString());
            candidate_name = Extensions.add(candidate_name,extension);
            if ( path.getFileName().toString().equals(candidate_name))
            {
                return false; // name unchanged
            }
            else
            {
                logger.log("name ->"+ path.getFileName()+"<- could be improved ->"+candidate_name+"<-");
                Path new_path = null;
                try
                {
                    new_path = path.getParent().resolve(candidate_name);
                }
                catch( InvalidPathException e)
                {
                    logger.log(""+e);
                }
                if (new_path != null)
                {
                    List<Old_and_new_Path> ll = new ArrayList<>();
                    ll.add(new Old_and_new_Path(path, new_path, Command.command_rename, Status.before_command, false));
                    Moving_files.perform_safe_moves_in_a_thread(ll, true, 100, 100, owner, new Aborter("dummy", logger), logger);
                    return true;
                }
            }
        }
        return  false;
    }
}
