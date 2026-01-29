package klikr.util.mmap;

import javafx.stage.Window;
import javafx.scene.image.Image;
import klikr.util.cache.Cache_folder;
import klikr.util.cache.Size_;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Simple_logger;
import klikr.util.log.Stack_trace_getter;

import java.io.*;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.*;

//**********************************************************
public class Mmap
//**********************************************************
{
    public static volatile Mmap instance;

    private final Map<Integer,Piece> pieces = new ConcurrentHashMap<>();
    private final Map<String, Meta> main_index = new ConcurrentHashMap<>();
    private final Logger logger;
    private final int piece_size_in_megabytes;
    private final Path cache_folder;
    private final Path main_index_file;


    private interface Operation{};
    private record Save_index() implements Operation{};
    private record Write_file(Path path, boolean and_save) implements Operation{};
    public record Write_image_as_pixels(String tag, Image image, boolean and_save, Runnable on_end) implements Operation{};
    public record Write_image_as_file(Path path, Image image, boolean and_save, Runnable on_end) implements Operation{};

    private final LinkedBlockingQueue<Operation> operation_queue = new LinkedBlockingQueue<>();

    private final static boolean stats_dbg = true;
    private final Map<String, Integer> usage = new ConcurrentHashMap<>();

    //**********************************************************
    public static Mmap get_instance(int piece_size_in_megabytes, Window owner, Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (Mmap.class)
            {
                if (instance == null)
                {
                    instance = new Mmap( piece_size_in_megabytes, owner, logger);
                }
            }
        }
        return instance;
    }

    //**********************************************************
    private Mmap(int piece_size_in_megabytes, Window owner,Logger logger)
    //**********************************************************
    {
        this.piece_size_in_megabytes = piece_size_in_megabytes;
        this.logger = logger;
        cache_folder = Static_files_and_paths_utilities.get_cache_folder(Cache_folder.icon_cache, owner, logger);
        main_index_file = cache_folder.resolve("main_index");
        load_index();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                for(;;)
                {
                    try
                    {
                        logger.log("going to block");
                        Operation op = operation_queue.poll(3, TimeUnit.SECONDS);
                        logger.log("operation!!! " + op);
                        if (op == null) continue;
                        if(op instanceof Save_index si)
                        {
                            save_index_internal();
                        }
                        else if(op instanceof Write_file wf)
                        {
                            write_file_internal(wf);
                        }
                        else if(op instanceof Write_image_as_pixels wiap)
                        {
                            write_image_as_pixels(wiap);
                        }
                        else if(op instanceof Write_image_as_file wiaf)
                        {
                            write_image_as_file(wiaf);
                        }
                    }
                    catch (InterruptedException e)
                    {
                        logger.log(""+e);
                    }
                }
            }
        };
        Actor_engine.execute(r,"mmap input pump",logger);


        if ( stats_dbg) {
            Runnable stats = new Runnable() {
                @Override
                public void run() {
                    for (; ; ) {
                        try {
                            Thread.sleep(20_000);
                            StringBuilder sb = new StringBuilder();
                            sb.append("****************************************\n");
                            for (Map.Entry<String, Integer> e : usage.entrySet()) {
                                sb.append(e.getKey()).append(" used: ").append(e.getValue()).append("\n");
                            }
                            sb.append("****************************************\n");
                            logger.log(sb.toString());

                        } catch (InterruptedException e) {
                            logger.log("" + e);
                        }
                    }
                }
            };
            Actor_engine.execute(stats, "mmap stats", logger);
        }
    }



    //**********************************************************
    public void save_index()
    //**********************************************************
    {
        operation_queue.offer(new Save_index());
    }


    //**********************************************************
    private void save_index_internal()
    //**********************************************************
    {
        util_save_index(main_index,main_index_file,logger);
    }

    //**********************************************************
    private Image_as_pixel_metadata find_room_for_image_as_pixel(Image image)
    //**********************************************************
    {
        long size= (long) (image.getWidth() * image.getHeight() * 4);
        Room room = find_room(size);
        if ( room == null) return null;
        return new Image_as_pixel_metadata(room.piece(), room.offset(), (int) image.getWidth(), (int) image.getHeight());
    }

    //**********************************************************
    private Image_as_file_metadata find_room_for_image_as_file(Image image, Path path)
    //**********************************************************
    {
        long length = path.toFile().length();
        Room room = find_room(length);
        if ( room == null) return null;
        return new Image_as_file_metadata(room.piece(), room.offset(), length);
    }


    //**********************************************************
    private Simple_metadata find_room_for_file(Path path)
    //**********************************************************
    {
        long size = path.toFile().length();
        Room room = find_room(size);
        if ( room == null)
        {
            logger.log("find_room_for_file failed for "+path);
            return null;
        }
        return new Simple_metadata(room.piece(), room.offset(), size);
    }

    record Room(Piece piece, long offset){}

    //**********************************************************
    private Room find_room(long length)
    //**********************************************************
    {
        for ( Piece piece : pieces.values() )
        {
            long offset = piece.has_room(length);
            if ( offset>= 0 )
            {
                return new Room(piece,offset);
            }
        }
        // need to create a new Piece
        int index = pieces.size();
        Piece piece = new Piece(index,cache_folder,logger);
        piece.init(piece_size_in_megabytes);
        pieces.put(index,piece);
        long offset = piece.has_room(length);
        if ( offset>= 0 )
        {
            return new Room(piece,offset);
        }
        return null;
    }

    //**********************************************************
    public void write_file(Path path, boolean and_save)
    //**********************************************************
    {
        logger.log("write_file offer ...");
        operation_queue.add(new Write_file(path,and_save));
    }
    //**********************************************************
    private void write_file_internal(Write_file wf)
    //**********************************************************
    {
        Simple_metadata meta = find_room_for_file(wf.path());
        if ( meta == null )
        {
            logger.log("no room found for "+wf.path());
            return;
        }
        meta.piece().write_file(meta,wf.path());
        String key = wf.path().toAbsolutePath().toString();
        main_index.put(key, meta);
        logger.log("mmap write_file_internal WROTE: "+key);
        if (wf.and_save())
        {
            save_index();
        }
    }

    //**********************************************************
    private byte[] read_file(Path p)
    //**********************************************************
    {
        Simple_metadata sm = (Simple_metadata) main_index.get(p.toAbsolutePath().toString());
        if ( sm == null)
        {
            logger.log("read_file failed: no metadata found for "+p);
            return null;
        }
        Piece piece = sm.piece();
        if ( piece == null)
        {
            logger.log("read_file failed: no piece for "+p);
            return null;
        }
        if ( stats_dbg)
        {
            usage.merge(p.toAbsolutePath().toString(), 1, Integer::sum);;
        }
        return piece.read_file(p);
    }

    // takes more file space but faster to reload
    //**********************************************************
    public void write_image_as_pixels(String tag, Image image, boolean and_save, Runnable on_end)
    //**********************************************************
    {
        operation_queue.offer(new Write_image_as_pixels(tag,image,and_save, on_end));
    }

    // for animated gifs, javafx does not have a PixelReader... so we cache the FILE
    //**********************************************************
    public void write_image_as_file(Path path, Image image, boolean and_save, Runnable on_end)
    //**********************************************************
    {
        operation_queue.offer(new Write_image_as_file(path,image,and_save, on_end));
    }

    //**********************************************************
    public void write_image_as_pixels(Write_image_as_pixels wi)
    //**********************************************************
    {
        Image_as_pixel_metadata meta = find_room_for_image_as_pixel(wi.image());
        if ( meta == null ) return;
        meta.piece().write_image_as_pixels(meta.offset(),wi.tag(),wi.image());
        main_index.put(wi.tag(), meta);
        logger.log("mmap image as pixel: "+wi.tag());
        if (wi.and_save())
        {
            save_index();
        }
        if ( wi.on_end() != null )
        {
            wi.on_end().run();
        }
    }

    //**********************************************************
    public void write_image_as_file(Write_image_as_file wi)
    //**********************************************************
    {
        Image_as_file_metadata meta = find_room_for_image_as_file(wi.image(), wi.path());
        if ( meta == null ) return;
        String key = wi.path().toAbsolutePath().toString();
        meta.piece().write_image_as_file(meta,wi.path());
        main_index.put(key, meta);
        logger.log("mmap image as file: "+key);
        if (wi.and_save())
        {
            save_index();
        }
        if ( wi.on_end() != null )
        {
            wi.on_end().run();
        }
    }


    //**********************************************************
    public Image read_image_as_pixel(String tag)
    //**********************************************************
    {
        Image_as_pixel_metadata meta = (Image_as_pixel_metadata) main_index.get(tag);
        if ( meta == null ) return null;
        logger.log("mmap reading image: "+tag+" is pixels=yes");
        Piece p = meta.piece();
        if (p == null) return null;
        if ( stats_dbg)
        {
            usage.merge(tag, 1, Integer::sum);;
        }
        return p.read_image_as_pixel(tag, meta);
    }
    //**********************************************************
    public Image read_image_as_file(String tag)
    //**********************************************************
    {
        Image_as_file_metadata meta = (Image_as_file_metadata) main_index.get(tag);
        if ( meta == null ) return null;
        logger.log("mmap reading image: "+tag+" is pixels=no");
        Piece p = meta.piece();
        if (p == null) return null;
        if ( stats_dbg)
        {
            usage.merge(tag, 1, Integer::sum);;
        }
        return p.read_image_as_file(tag, meta);
    }




    //**********************************************************
    public synchronized double clear_cache()
    //**********************************************************
    {
        double d = Size_.of_Map(main_index,Size_.of_String_F(),meta -> 8L);
        main_index.clear();
        if( stats_dbg) usage.clear();
        for ( Piece piece : pieces.values() ) piece.clear_cache();
        save_index();
        return d;
    }

    //**********************************************************
    public MemorySegment get_MemorySegment(String tag)
    //**********************************************************
    {
        Simple_metadata sm = (Simple_metadata) main_index.get(tag);
        Piece p = sm.piece();
        if (p == null) return null;
        return p.get_MemorySegment(tag);
    }



    static final byte SIMPLE_META = 0x01;
    static final byte IMAGE_PIXEL_META = 0x02;
    static final byte IMAGE_FILE_META = 0x03;
    //**********************************************************
    private static void util_save_index(Map<String,Meta> local, Path index_file, Logger logger)
    //**********************************************************
    {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(index_file.toFile())))
        {
            logger.log("index size ="+local.size());
            dos.writeInt(local.size());
            for (Map.Entry<String, Meta> entry : local.entrySet())
            {
                Meta meta = entry.getValue();
                if (meta instanceof Simple_metadata simple)
                {
                    logger.log("writing Simple_metadata ="+entry.getKey());

                    dos.writeInt(simple.piece().who_are_you);
                    dos.write(SIMPLE_META);
                    dos.writeUTF(entry.getKey());
                    dos.writeLong(simple.offset());
                    dos.writeLong(simple.length());
                }
                else if (meta instanceof Image_as_pixel_metadata iapm)
                {
                    logger.log("writing Image_as_pixel_metadata ="+entry.getKey());

                    dos.writeInt(iapm.piece().who_are_you);
                    dos.write(IMAGE_PIXEL_META);
                    dos.writeUTF(entry.getKey());
                    dos.writeLong(iapm.offset());
                    dos.writeInt(iapm.width());
                    dos.writeInt(iapm.height());
                }
                else if (meta instanceof Image_as_file_metadata isfm)
                {
                    logger.log("writing Image_as_file_metadata ="+entry.getKey());

                    dos.writeInt(isfm.piece().who_are_you);
                    dos.write(IMAGE_FILE_META);
                    dos.writeUTF(entry.getKey());
                    dos.writeLong(isfm.offset());
                    dos.writeLong(isfm.length());
                }
            }
            dos.flush();
            logger.log("Index saved with " + local.size() + " entries.");
            return;
        }
        catch (FileNotFoundException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
    }
    //**********************************************************
    public void load_index()
    //**********************************************************
    {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(main_index_file.toFile())))
        {
            int size = dis.readInt();
            for (int i = 0; i < size; i++)
            {
                int piece_index = dis.readInt();
                Piece local = pieces.get(piece_index);
                if(  local == null )
                {
                    local = new Piece(piece_index,cache_folder,logger);
                    pieces.put(piece_index,local);
                }
                byte type = dis.readByte();
                String key = dis.readUTF();
                long offset = dis.readLong();
                if ( type == SIMPLE_META )
                {
                    long length = dis.readLong();
                    Meta m = new Simple_metadata(pieces.get(piece_index),offset,length);
                    local.insert(key,m);
                    logger.log("cached item reloaded from file: "+key);
                }
                else if ( type == IMAGE_PIXEL_META )
                {
                    int width = dis.readInt();
                    int height = dis.readInt();
                    Meta m = new Image_as_pixel_metadata(pieces.get(piece_index),offset,width,height);
                    local.insert(key,m);
                    logger.log("cached item reloaded from file: "+key);
                }
                else if ( type == IMAGE_FILE_META )
                {
                    long length = dis.readLong();
                    Meta m = new Image_as_file_metadata(pieces.get(piece_index),offset,length);
                    local.insert(key,m);
                    logger.log("cached item reloaded from file: "+key);
                }
            }
            logger.log("Index local with " + main_index.size() + " entries.");
        }
        catch (FileNotFoundException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        // we can init only after everything is reloaded
        for (Piece piece : pieces.values())
        {
            piece.init(piece_size_in_megabytes);
        }
    }



    //**********************************************************
    public static void main(String[] args)
    //**********************************************************
    {
        Logger logger = new Simple_logger();
        Mmap mmap = Mmap.get_instance(100, null,logger);
        {
            // test#1: file
            Path p = Path.of("file1.txt");
            byte[] in = new byte[256];
            for (int i = 0; i < 255; i++)
            {
                in[i] = (byte) i;
            }
            try {
                Files.write(p, in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.log("File saved with " + p.toAbsolutePath());
            mmap.write_file(p, true);
            mmap.save_index();
            logger.log("File in cache !");
            byte[] check = mmap.read_file(p);

            for (int i = 0; i < check.length; i++)
            {
                if ( in[i] != check[i])
                {
                    logger.log("FATAL");
                    return;
                }
            }
            logger.log("Retrieved content: " + new String(check));
        }
        {
            // test#2: image RAW pixels
            String tag = "image.png";
            {
                Image i = new Image(new File(tag).toURI().toString());
                mmap.write_image_as_pixels(tag, i, true, null);
            }
            {
                Image j = mmap.read_image_as_pixel(tag);

            }
        }
        {
            // test#2: image RAW pixels
            String tag = "image.png";
            {
                Image i = new Image(new File(tag).toURI().toString());
                mmap.write_image_as_file(Path.of(tag), i, true, null);
            }
            {
                Image j = mmap.read_image_as_file(tag);

            }
        }

    }

}
