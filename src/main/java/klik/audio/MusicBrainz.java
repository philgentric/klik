package klik.audio;

import javafx.scene.image.Image;
import javafx.stage.Window;
import klik.properties.Cache_folder;
import klik.util.files_and_paths.Filename_sanitizer;
import klik.util.files_and_paths.Static_files_and_paths_utilities;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;

//**********************************************************
public class MusicBrainz
//**********************************************************
{
    private static final boolean ultra_dbg = false;
    private static final String user_agent = "MyMusicApp/1.0 (myemail@example.com)";
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
                logger.log("Unexpected response code: " + responseCode);
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
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", user_agent);
            int responseCode = conn.getResponseCode();
            if ( ! (responseCode == HttpURLConnection.HTTP_OK) )
            {
                logger.log("Unexpected response code: " + responseCode);
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
    public static Image download_icon(String artist, String release, Window owner, Logger logger)
    //**********************************************************
    {
        String image_URL = get_cover_art_URL(artist,release,logger);
        if ( image_URL == null) return null;

        File icon_file = make_icon_file(artist,release,owner,logger);
        try (BufferedInputStream in = new BufferedInputStream(new URL(image_URL).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(icon_file)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
        catch (IOException e)
        {
            logger.log(""+e);
        }

        return new Image(icon_file.toURI().toString());
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
    private static File make_icon_file(String artist, String release, Window owner,Logger logger)
    //**********************************************************
    {
        String name = make_name(artist,release,owner,logger);
        name += ".jpg";

        Path icon_cache_dir = Static_files_and_paths_utilities.get_cache_dir(Cache_folder.klik_icon_cache, owner, logger);
        return new File(icon_cache_dir.toFile(),name);
    }

    //**********************************************************
    public static Image get_icon(String artist, String release, Window owner, Logger logger)
    //**********************************************************
    {

        File f = make_icon_file(artist,release,owner,logger);
        try (FileInputStream input_stream = new FileInputStream(f))
        {
            Image image = new Image(input_stream);
            return image;
        }
        catch (FileNotFoundException e) {
            // this always happens, the first time
            if (ultra_dbg)
                logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }

        Image icon = download_icon(artist,release,owner,logger);
        return icon;
    }
}
