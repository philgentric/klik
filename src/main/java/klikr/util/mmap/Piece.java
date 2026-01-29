package klikr.util.mmap;

import javafx.scene.image.*;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

//**********************************************************
public class Piece
//**********************************************************
{
    private MemorySegment segment;
    public final Path index_file;
    public final Path giant_file;
    private final Arena arena;
    private final Logger logger;
    private final AtomicLong current_offset = new AtomicLong(0);
    private final Map<String, Meta> index = new ConcurrentHashMap<>();
    private static final long ALIGNMENT = 16 * 1024;
    final int who_are_you;

    //**********************************************************
    Piece(int index, Path cache_folder, Logger logger)
    //**********************************************************
    {
        this.who_are_you = index;
        this.logger = logger;
        giant_file = cache_folder.resolve("giant."+index);
        this.index_file = giant_file.getParent().resolve(giant_file.getFileName().toString()+".index");
        // Arena.ofShared() allows multi-threaded access
        this.arena = Arena.ofShared();
    }


    //**********************************************************
    public boolean init(int size_in_megabytes)
    //**********************************************************
    {

        // 1. Pre-allocate DB file so the map has non-zero length to work with
        if (Files.exists(giant_file))
        {
            logger.log("Mmap file already exists, RELOADING: "+giant_file.toAbsolutePath());
            long maxOffset = 0;
            for (Meta m : index.values())
            {
                if (m instanceof Simple_metadata s)
                {
                    maxOffset = Math.max(maxOffset, s.offset() + s.length());
                }
                else if (m instanceof Image_as_file_metadata i)
                {
                    maxOffset = Math.max(maxOffset, i.offset() + i.length());
                }
                else if (m instanceof Image_as_pixel_metadata i)
                {
                    maxOffset = Math.max(maxOffset, i.offset() + (long) i.width() * i.height() * 4);
                }

            }
            // Align the restored offset
            long alignedMax = (maxOffset + ALIGNMENT - 1) & ~(ALIGNMENT - 1);
            current_offset.set(alignedMax);
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
            segment = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size(), arena);
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace("Failed to memory-map the file: " + e.getMessage()));
            return false;
        }

        return  true;
    }

    //**********************************************************
    private boolean init_empty_giant_file(int size_in_megabytes)
    //**********************************************************
    {
        try (RandomAccessFile raf = new RandomAccessFile(giant_file.toFile(), "rw")) {
            // Set the file length immediately without allocating heap memory
            raf.setLength(1024L * 1024L * size_in_megabytes);
        } catch (IOException e) {
            logger.log("Failed to create file: " + e.getMessage());
            return true;
        }
        return false;
    }



    //**********************************************************
    public long has_room(long size)
    //**********************************************************
    {
        if ( segment == null)
        {
            logger.log("FATAL: segment == null");
            return -1;
        }

        if (size > segment.byteSize()) {
            logger.log("Item too huge for cache file");
            return -1;
        }

        while (true) {
            long current = current_offset.get();
            // Calculate position rounded up to the nearest 16KB boundary
            long aligned_start_offset = (current + ALIGNMENT - 1) & ~(ALIGNMENT - 1);
            long nextOffset = aligned_start_offset + size;

            if (nextOffset > segment.byteSize())
            {
                logger.log("Not enough space in memory mapped file");
                return -1;
            }

            // Try to update currentOffset to the END of this new file
            if (current_offset.compareAndSet(current, nextOffset)) {
                return aligned_start_offset;
            }
        }
    }

    /*
    //**********************************************************
    public String write_file(Simple_metadata simple_meta, Path path)
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
            long length = Files.length(path);
            copy_file_to_segment(path, simple_meta.offset(), length);

            Meta prev = index.putIfAbsent(tag, simple_meta);
            if (prev == null)
            {
                // ok, was available
                logger.log("Registered tag:->" + tag + "<- at aligned offset: " + simple_meta.offset());
                return tag;
            }
            logger.log("FATAL NOT Registered " + tag );
            return null;
        }
        catch (IOException e)
        {
            logger.log("Could not register file: " + e.getMessage());
            return null;
        }

    }
*/


    //**********************************************************
    public void write_file(Simple_metadata simple_meta, Path path)
    //**********************************************************
    {
        String tag = path.toAbsolutePath().normalize().toString();
        if (index.containsKey(tag))
        {
            logger.log("write_file, tag already registered: " + path);
            return;
        }
        Meta prev = index.putIfAbsent(tag, simple_meta);
        if (prev == null)
        {
            // ok, was available
            logger.log("Registered tag:->" + tag + "<- at aligned offset: " + simple_meta.offset());
            write_file_internal(path,simple_meta.offset());
            return;
        }
        logger.log("FATAL NOT Registered " + tag );
    }

    //**********************************************************
    public void write_file_internal(Path path, long offset)
    //**********************************************************
    {
        String tag = path.toAbsolutePath().normalize().toString();
        if (index.containsKey(tag))
        {
            logger.log("write_file, tag already registered: " + path);
            return;
        }
        try
        {
            long size = Files.size(path);
            copy_file_to_segment(path, offset, size);
        }
        catch (IOException e)
        {
            logger.log("Could not write file: " + e.getMessage());
        }

    }



    //**********************************************************
    public boolean write_bytes(Simple_metadata simple_meta, String tag, byte[] bytes)
    //**********************************************************
    {
        if (index.containsKey(tag))
        {
            logger.log("write_bytes, tag already registered: " + tag);
            return false;
        }

        long size = bytes.length;

        MemorySegment sourceParam = MemorySegment.ofArray(bytes);
        MemorySegment.copy(sourceParam, 0, segment, simple_meta.offset(), size);

        Meta prev = index.putIfAbsent(tag, simple_meta);
        if (prev == null)
        {
            // ok, was available
            logger.log("Registered bytes tag:->" + tag + "<- at aligned offset: " + simple_meta.offset());
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
            logger.log("Error copying file to memory-mapped segment: " + e.getMessage());
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
            return segment.asSlice(simple.offset(), simple.length());
        }
        else if ( meta instanceof Image_as_pixel_metadata imageMeta)
        {
            return segment.asSlice(imageMeta.offset(), (long) imageMeta.width() * imageMeta.height() * 4);
        }
        else if ( meta instanceof Image_as_file_metadata imageMeta)
        {
            return segment.asSlice(imageMeta.offset(), imageMeta.length());
        }
        return null;
    }


    //**********************************************************
    public void write_image_as_file(Image_as_file_metadata meta, Path path)
    //**********************************************************
    {
        String tag = path.toAbsolutePath().normalize().toString();
        if (index.containsKey(tag))
        {
            logger.log("write_file, tag already registered: " + path);
            return;
        }
        Meta prev = index.putIfAbsent(tag, meta);
        if (prev == null)
        {
            // ok, was available
            logger.log("Registered tag:->" + tag + "<- at aligned offset: " + meta.offset());
            write_file_internal(path,meta.offset());
            return;
        }
        logger.log("\n\n\nPANIC file not written !!!! Registered tag:->" + tag + "<- at aligned offset: " + meta.offset());

    }

    //**********************************************************
    public boolean write_image_as_pixels(long offset, String tag, Image image)
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

        MemorySegment sourceParam = MemorySegment.ofArray(bytes);
        MemorySegment.copy(sourceParam, 0, segment, offset, size);

        Meta prev = index.putIfAbsent(tag, new Image_as_pixel_metadata(this,offset, width, height));
        if (prev == null)
        {
            // ok, was available
            logger.log("Registered image with tag:->" + tag + "<- at aligned offset: " + offset);
            return true;
        }
        return false;
    }


    //**********************************************************
    public Image read_image_as_pixel(String tag, Image_as_pixel_metadata meta)
    //**********************************************************
    {
        MemorySegment segment = get_MemorySegment(tag);
        if (segment == null)
        {
            return null;
        }

        Meta meta2 = index.get(tag);
        if ( meta2 == null)
        {
            logger.log("File not found in index "+who_are_you);
            return null;
        }
        if ( meta2.equals(meta))
        {
            logger.log("OK, same meta ");
        }
        else {
            logger.log("\n\n\nPANIC, different meta "+meta+" vs "+meta2);
            return null;
        }
        int width = meta.width();
        logger.log("image w = "+width);

        int height = meta.height();
        logger.log("image h = "+height);

        java.nio.ByteBuffer directBuffer = segment.asByteBuffer();
        PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(
                width,
                height,
                directBuffer,
                PixelFormat.getByteBgraPreInstance() // Must match the format used in write_image
            );
        Image returned = new WritableImage(pixelBuffer);
        logger.log("Retrieved image FROM PIXELS, w= "+returned.getWidth()+" h= "+returned.getHeight());
        return returned;
    }

    //**********************************************************
    public Image read_image_as_file(String tag, Image_as_file_metadata meta)
    //**********************************************************
    {
        MemorySegment segment = get_MemorySegment(tag);
        if (segment == null)
        {
            return null;
        }

        Meta meta2 = index.get(tag);
        if ( meta2 == null)
        {
            logger.log("File not found in piece "+who_are_you);
            return null;
        }
        if ( meta2.equals(meta))
        {
            logger.log("OK, same meta ");
        }
        else
        {
            logger.log("\n\n\nPANIC, different meta "+meta+" vs "+meta2);
            return null;
        }
        long  length = meta.length();
        logger.log("image file size = "+length+ " offset = "+meta.offset());
        /*{
            byte[] magic = new byte[4];
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, meta.offset(), magic, 0, 4);
            System.out.printf("image magic = %02x %02x %02x %02x ", magic[0], magic[1], magic[2], magic[3]);
        }
        {
            byte[] magic = new byte[4];
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, 0, magic, 0, 4);
            System.out.printf("image magic = %02x %02x %02x %02x ", magic[0], magic[1], magic[2], magic[3]);
        }*/
        //ByteBuffer buf =  segment.asByteBuffer();
        byte[] bytes = new byte[(int)meta.length()];
        MemorySegment.copy(segment,ValueLayout.JAVA_BYTE,meta.offset(),bytes,0,bytes.length);
        try( ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            Image returned = new Image(bais);
            logger.log("Retrieved image FROM FILE, w= " + returned.getWidth() + " h= " + returned.getHeight());
            logger.log("error:" + returned.isError() + " " + returned.getException());
            return returned;
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        return null;
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

                if ( what == Mmap.SIMPLE_META)
                {
                    long length = dis.readLong();
                    index.put(tag, new Simple_metadata(this, offset, length));
                }
                else if (what == Mmap.IMAGE_PIXEL_META)
                {
                    int width = dis.readInt();
                    int height = dis.readInt();
                    index.put(tag, new Image_as_pixel_metadata(this, offset, width,height));
                }
                else if (what == Mmap.IMAGE_FILE_META)
                {
                    long length = dis.readLong();
                    index.put(tag, new Image_as_file_metadata(this, offset, length));
                }
                else
                {
                    break;
                }
            }
        }
        catch (FileNotFoundException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return false;
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return false;
        }
        return true;
    }

    //**********************************************************
    synchronized void clear_cache()
    //**********************************************************
    {
        logger.log("Cache full (buffer end reached). Clearing all entries and resetting offset.");

        // 1. Clear the index map so key lookups fail gracefully
        index.clear();

        // 2. Reset the atomic offset counter to 0
        current_offset.set(0);
    }

    public byte[] read_file(Path p)
    {
        String tag = p.toAbsolutePath().toString();
        MemorySegment segment = get_MemorySegment(tag);
        if (segment == null)
        {
            logger.log(" no segment for "+tag);
            return null;
        }

        return segment.toArray(java.lang.foreign.ValueLayout.JAVA_BYTE);
    }


    public void insert(String key, Meta m)
    {
        index.put(key,m);
    }
}
