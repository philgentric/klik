package klikr.util.mmap;

import javafx.stage.Window;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import klikr.util.cache.Cache_folder;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Simple_logger;
import klikr.util.log.Stack_trace_getter;

import java.io.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

//**********************************************************
public class Mmap
//**********************************************************
{
    public static volatile Mmap instance;
    // 16KB alignment constant
    private static final long ALIGNMENT = 16 * 1024;

    private MemorySegment segment;
    public final Path index_file;
    public final Path giant_file;
    private final Arena arena;
    private final Map<String, Meta> index = new ConcurrentHashMap<>();
    private final AtomicLong currentOffset = new AtomicLong(0);
    private final Logger logger;
    private final Runnable on_nuke;
    private final ArrayBlockingQueue<Boolean> save_queue = new ArrayBlockingQueue<>(1);

    private interface Meta{};
    private record Simple_metadata(long offset, long length) implements Meta{}
    private record Image_metadata(long offset, int width, int height) implements Meta {}


    // allow_nuke: if true, when the mmap file is full, it will clear all entries and start over
    // kind of drastic even for a cache...
    //**********************************************************
    public static Mmap get_instance(String tag, int size_in_megabytes, Runnable on_nuke, Window owner, Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (Mmap.class)
            {
                if (instance == null)
                {
                    Path folder = Static_files_and_paths_utilities.get_cache_folder(Cache_folder.icon_cache, owner, logger);
                    instance = new Mmap(folder.resolve(tag), size_in_megabytes, on_nuke, logger);
                }
            }
        }
        return instance;
    }



    //**********************************************************
    private Mmap(Path giant_file, int size_in_megabytes, Runnable on_nuke, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.on_nuke = on_nuke;
        this.giant_file = giant_file;
        this.index_file = giant_file.getParent().resolve(giant_file.getFileName().toString()+".index");
        // Arena.ofShared() allows multi-threaded access
        this.arena = Arena.ofShared();
        if ( !init(size_in_megabytes))
        {
            logger.log("FATAL ERROR: Mmap initialization failed");
        }
    }

    //**********************************************************
    private boolean init(int size_in_megabytes)
    //**********************************************************
    {

        // 1. Pre-allocate DB file so the map has non-zero size to work with
        if (Files.exists(giant_file))
        {
            logger.log("Mmap file already exists, RELOADING: "+giant_file.toAbsolutePath());
            if (read_index(index_file))
            {
                long maxOffset = 0;
                for (Meta m : index.values())
                {
                    if (m instanceof Simple_metadata s)
                    {
                        maxOffset = Math.max(maxOffset, s.offset + s.length);
                    }
                    else if (m instanceof Image_metadata i)
                    {
                        maxOffset = Math.max(maxOffset, i.offset + (long) i.width * i.height * 4);
                    }
                }
                // Align the restored offset
                long alignedMax = (maxOffset + ALIGNMENT - 1) & ~(ALIGNMENT - 1);
                currentOffset.set(alignedMax);
            }
        }
        else
        {
            logger.log("Mmap CREATION: "+giant_file.toAbsolutePath());

            if (init_empty_giant_file(size_in_megabytes))
            {
                return false;
            }
        }

        try (FileChannel channel = FileChannel.open(giant_file, StandardOpenOption.READ, StandardOpenOption.WRITE))
        {
            this.segment = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size(), arena);
        }
        catch (IOException e)
        {
            logger.log("Failed to memory-map the file: " + e.getMessage());
            return false;
        }
        Runnable savior = () -> {
            for(;;)
            {
                try
                {
                    // Block waiting for a save request
                    save_queue.take();
                    save_index_internal();
                }
                catch (InterruptedException e)
                {
                    logger.log(""+e);
                    break;
                }
            }
        };

        Actor_engine.execute(savior, "Mmap-index-saver", logger);
        return  true;
    }

    //**********************************************************
    private boolean init_empty_giant_file(int size_in_megabytes)
    //**********************************************************
    {
        try (RandomAccessFile raf = new RandomAccessFile(giant_file.toFile(), "rw")) {
            // Set the file size immediately without allocating heap memory
            raf.setLength(1024L * 1024L * size_in_megabytes);
        } catch (IOException e) {
            logger.log("Failed to create file: " + e.getMessage());
            return true;
        }
        return false;
    }

    //**********************************************************
    private long reserve_space(long size)
    //**********************************************************
    {
        if (size > segment.byteSize()) {
            logger.log("Item too huge for cache file");
            return -1;
        }

        while (true) {
            long current = currentOffset.get();
            // Calculate position rounded up to the nearest 16KB boundary
            long aligned_start_offset = (current + ALIGNMENT - 1) & ~(ALIGNMENT - 1);
            long nextOffset = aligned_start_offset + size;

            if (nextOffset > segment.byteSize())
            {
                if (on_nuke != null)
                {
                    synchronized(this) {
                        // Double-check: Another thread might have already cleared the cache while we waited
                        long freshCurrent = currentOffset.get();
                        long freshStart = (freshCurrent + ALIGNMENT - 1) & ~(ALIGNMENT - 1);

                        // If it is STILL full, then we are the chosen thread to nuke it.
                        if (freshStart + size > segment.byteSize()) {
                            clear_cache();
                            on_nuke.run();
                        }
                    }
                    continue;
                }
                else
                {
                    logger.log("Not enough space in memory mapped file");
                    return -1;
                }

            }
            // Try to update currentOffset to the END of this new file
            if (currentOffset.compareAndSet(current, nextOffset)) {
                return aligned_start_offset;
            }
        }
    }

    //**********************************************************
    public String write_file(Path path, boolean and_save)
    //**********************************************************
    {
        String tag = path.toAbsolutePath().normalize().toString();
        if (index.containsKey(tag))
        {
            logger.log("write_file, tag already registered: " + path);
            return null;
        }
        try
        {
            long size = Files.size(path);
            long offset = reserve_space(size);
            if (offset == -1) return null;
            copy_file_to_segment(path, offset, size);

            Meta prev = index.putIfAbsent(tag, new Simple_metadata(offset, size));
            if (prev == null)
            {
                // ok, was available
                logger.log("Registered tag:->" + tag + "<- at aligned offset: " + offset);
                if ( and_save) save_index();
                return tag;
            }
            logger.log("FATAL NOT Registered " + tag );
            return null;
        }
        catch (IOException e)
        {
            System.err.println("Could not register file: " + e.getMessage());
            return null;
        }

    }

    //**********************************************************
    public boolean write_bytes(String tag, byte[] bytes, boolean and_save)
    //**********************************************************
    {
        if (index.containsKey(tag))
        {
            logger.log("write_bytes, tag already registered: " + tag);
            return false;
        }

        long size = bytes.length;
        long offset = reserve_space(size);
        if (offset == -1) return false;

        MemorySegment sourceParam = MemorySegment.ofArray(bytes);
        MemorySegment.copy(sourceParam, 0, segment, offset, size);

        Meta prev = index.putIfAbsent(tag, new Simple_metadata(offset, size));
        if (prev == null)
        {
            // ok, was available
            logger.log("Registered bytes tag:->" + tag + "<- at aligned offset: " + offset);
            if ( and_save) save_index();
            return true;
        }
        return false;
    }

    //**********************************************************
    private void copy_file_to_segment(Path sourceFile, long destinationOffset, long size)
    //**********************************************************
    {
        // Use a confined arena for the source handling, it closes immediately after copy
        try (Arena localArena = Arena.ofConfined(); FileChannel srcChannel = FileChannel.open(sourceFile, StandardOpenOption.READ))
        {
            MemorySegment srcSegment = srcChannel.map(FileChannel.MapMode.READ_ONLY, 0, size, localArena);
            MemorySegment.copy(srcSegment, 0, this.segment, destinationOffset, size);
        }
        catch (IOException e)
        {
            System.err.println("Error copying file to memory-mapped segment: " + e.getMessage());
        }
    }

    //**********************************************************
    public MemorySegment get_MemorySegment(String tag)
    //**********************************************************
    {
        Meta meta = index.get(tag);
        if (meta == null) return null;
        if ( meta instanceof Simple_metadata simple)
        {
            return segment.asSlice(simple.offset, simple.length);
        }
        if ( meta instanceof Image_metadata imageMeta)
        {
            return segment.asSlice(imageMeta.offset, imageMeta.width * imageMeta.height * 4);
        }
        return null;
    }




    //**********************************************************
    public boolean write_image(String tag, Image image, boolean and_save)
    //**********************************************************
    {
        if (index.containsKey(tag))
        {
            logger.log("write_image, tag already registered: " + tag);
            return false;
        }
        PixelReader pr = image.getPixelReader();
        if ( pr == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌ PANIC in write_image, PixelReader is null for image: " + image));
            return false;
        }
        int width = (int)image.getWidth();
        int height = (int)image.getHeight();
        byte[] bytes = new byte[width*height*4];
        pr.getPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(), bytes, 0, width * 4);
        for (int i = 0; i < width*height; i++) {
            int base = i * 4;
            int b = bytes[base]   & 0xFF;
            int g = bytes[base+1] & 0xFF;
            int r = bytes[base+2] & 0xFF;
            int a = bytes[base+3] & 0xFF;

            // premultiply: c' = c * a / 255
            bytes[base]   = (byte)((b * a) / 255);
            bytes[base+1] = (byte)((g * a) / 255);
            bytes[base+2] = (byte)((r * a) / 255);
            bytes[base+3] = (byte)a;   // alpha stays the same
        }



        long size = bytes.length;
        long offset = reserve_space(size);
        if (offset == -1) return false;

        MemorySegment sourceParam = MemorySegment.ofArray(bytes);
        MemorySegment.copy(sourceParam, 0, segment, offset, size);

        Meta prev = index.putIfAbsent(tag, new Image_metadata(offset, width, height));
        if (prev == null)
        {
            // ok, was available
            logger.log("Registered image with tag:->" + tag + "<- at aligned offset: " + offset);
            if ( and_save) save_index();
            return true;
        }
        return false;
    }


    //**********************************************************
    public Image read_image(String tag)
    //**********************************************************
    {
        MemorySegment segment = get_MemorySegment(tag);
        if (segment == null)
        {
            return null;
        }

        Meta meta = index.get(tag);
        int width;
        int height;
        if (meta instanceof Image_metadata im)
        {
            width = im.width();
            height = im.height();

            /* works but is counterproductive as it creates a COPY of the pixels in heap memory
            so the memory pressure is DOUBLED !!!
            WritableImage wImage = new WritableImage(width, height);
            PixelWriter writer = wImage.getPixelWriter();
            byte[] bytes = segment.toArray(java.lang.foreign.ValueLayout.JAVA_BYTE);
            writer.setPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(), bytes, 0, width * 4);
            return wImage;
            */

            java.nio.ByteBuffer directBuffer = segment.asByteBuffer();
            PixelBuffer<java.nio.ByteBuffer> pixelBuffer = new PixelBuffer<>(
                    width,
                    height,
                    directBuffer,
                    PixelFormat.getByteBgraPreInstance() // Must match the format used in write_image
            );
            logger.log("Retrieved image: "+tag);
            return new WritableImage(pixelBuffer);
        }
        else
        {
            logger.log("File not found in index.");
            return null;
        }
    }



    //**********************************************************
    public void save_index()
    //**********************************************************
    {
        save_queue.offer(Boolean.TRUE);
    }


    static final byte SIMPLE_META = 0x01;
    static final byte IMAGE_META = 0x02;
    //**********************************************************
    private void save_index_internal()
    //**********************************************************
    {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(index_file.toFile())))
        {
            dos.writeInt(index.size());
            for (Map.Entry<String, Meta> entry : index.entrySet())
            {
                Meta meta = entry.getValue();
                if (meta instanceof Simple_metadata simple)
                {
                    dos.write(SIMPLE_META);
                    String tag = entry.getKey();
                    dos.writeUTF(tag);
                    dos.writeLong(simple.offset());
                    dos.writeLong(simple.length());
                }
                else if (meta instanceof Image_metadata imageMeta)
                {
                    dos.write(IMAGE_META);
                    String tag = entry.getKey();
                    dos.writeUTF(tag);
                    dos.writeLong(imageMeta.offset());
                    dos.writeInt(imageMeta.width());
                    dos.writeInt(imageMeta.height());
                }
            }
            dos.flush();
            logger.log("Index saved with " + index.size() + " entries.");
            return;
        }
        catch (FileNotFoundException e)
        {
            logger.log(""+e);
        }
        catch (IOException e)
        {
            logger.log(""+e);
        }
    }

    //**********************************************************
    public boolean read_index(Path index_file)
    //**********************************************************
    {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(index_file.toFile())))
        {
            int num_items = dis.readInt();
            for (int i = 0; i < num_items; i++)
            {
                byte what = dis.readByte();
                String tag = dis.readUTF();
                long offset = dis.readLong();

                if ( what == SIMPLE_META)
                {
                    long length = dis.readLong();
                    index.put(tag, new Simple_metadata(offset, length));
                }
                else if (what == IMAGE_META)
                {
                    int width = dis.readInt();
                    int height = dis.readInt();
                    index.put(tag, new Image_metadata(offset, width, height));
                }
                else
                {
                    break;
                }
            }
        }
        catch (FileNotFoundException e)
        {
            logger.log(""+e);
            return false;
        }
        catch (IOException e)
        {
            logger.log(""+e);
            return false;
        }
        return true;
    }

    //**********************************************************
    private synchronized void clear_cache()
    //**********************************************************
    {
        logger.log("Cache full (buffer end reached). Clearing all entries and resetting offset.");

        // 1. Clear the index map so key lookups fail gracefully
        index.clear();

        // 2. Reset the atomic offset counter to 0
        currentOffset.set(0);

        // 3. Save the empty index to disk immediately causing the .index file to be reset too
        save_index();
    }


    /*
    does not make sense to close at any "normal" time
    public void close()
    {
        save_index();
        arena.close();
    }
    */

    //**********************************************************
    public static void main(String[] args)
    //**********************************************************
    {
        int size_in_megabytes = 1024;
        Logger logger = new Simple_logger();
        Mmap mmap = Mmap.get_instance("giant",100,null, null,logger);
        if (!mmap.init(size_in_megabytes)) return;
        {
            // test#1: file
            String tag = mmap.write_file(Path.of("file1.txt"), true);
            MemorySegment segment = mmap.get_MemorySegment(tag);
            if (segment != null)
            {
                // Read content back to verify
                byte[] check = segment.toArray(java.lang.foreign.ValueLayout.JAVA_BYTE);
                logger.log("Retrieved content: " + new String(check));
            }
            else
            {
                logger.log("File not found in index.");
            }
        }
        {
            // test#2: image RAW pixels
            String tag = "image.png";
            {
                Image i = new Image(new File(tag).toURI().toString());
                mmap.write_image(tag, i, true);
            }
            {
                Image j = mmap.read_image(tag);

            }
        }

    }
}
