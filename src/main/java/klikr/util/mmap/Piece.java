package klikr.util.mmap;

import javafx.scene.image.*;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import org.jspecify.annotations.Nullable;

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
    //public final Path index_file;
    public final Path giant_file;
    private final Arena arena;
    private final Logger logger;
    private final AtomicLong current_offset = new AtomicLong(0);
    //private final Map<String, Meta> index = new ConcurrentHashMap<>();
    private static final long ALIGNMENT = 16 * 1024;
    final int who_are_you;

    //**********************************************************
    Piece(int who_are_you, Path cache_folder, Logger logger)
    //**********************************************************
    {
        this.who_are_you = who_are_you;
        this.logger = logger;
        giant_file = cache_folder.resolve("giant."+who_are_you);
        //this.index_file = giant_file.getParent().resolve(giant_file.getFileName().toString()+".index");
        // Arena.ofShared() allows multi-threaded access
        this.arena = Arena.ofShared();
    }


    //**********************************************************
    public boolean init(Map<String, Meta> index, int size_in_megabytes)
    //**********************************************************
    {

        // 1. Pre-allocate DB file so the map has non-zero length to work with
        if (Files.exists(giant_file))
        {
            logger.log("Mmap file already exists, recomputing offset");
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
        write_file_internal(path,simple_meta.offset());
        String tag = path.toAbsolutePath().normalize().toString();
        logger.log("write_file_internal DONE " + tag );
    }

    //**********************************************************
    public void write_file_internal(Path path, long offset)
    //**********************************************************
    {
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

        long size = bytes.length;

        MemorySegment sourceParam = MemorySegment.ofArray(bytes);
        MemorySegment.copy(sourceParam, 0, segment, simple_meta.offset(), size);
        return true;
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
    public MemorySegment get_MemorySegment(Meta meta)
    //**********************************************************
    {
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
    public boolean write_image_as_pixels(long offset,  Image image)
    //**********************************************************
    {

        PixelReader pr = image.getPixelReader();
        if ( pr == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌ PANIC in write_image, PixelReader is null for image: " + image));
            return false;
        }
        int width = (int)image.getWidth();
        int height = (int)image.getHeight();

        // could we use ????
        // ByteBuffer buffer = ByteBuffer.allocateDirect((int) width*height*4);
        // pr.getPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(), buffer.array(), 0, width * 4);


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

        return true;
    }


    //**********************************************************
    public Image read_image_as_pixel(Image_as_pixel_metadata meta)
    //**********************************************************
    {
        MemorySegment segment = get_MemorySegment(meta);
        if (segment == null) return null;
        int width = meta.width();
        logger.log("image w = "+width);
        int height = meta.height();
        logger.log("image h = "+height);

        ByteBuffer a = segment.asByteBuffer();

        /*
        ByteBuffer b = ByteBuffer.allocate(a.capacity());
        for (int i = 0; i < a.limit(); ++i) {
            b.put(a.get(i));
        }

        // Flip the buffers to start reading from the beginning
        a.flip();
        b.flip();

         */
        PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(
                width,
                height,
                a,
                PixelFormat.getByteBgraPreInstance() // Must match the format used in write_image
            );
        Image returned = new WritableImage(pixelBuffer);
        logger.log("Retrieved image FROM PIXELS, w= "+returned.getWidth()+" h= "+returned.getHeight());
        return returned;
    }


    /*


    public boolean write_image_as_pixels(long address,
                                         Image fxImage)
    {
        int imgWidth = (int)fxImage.getWidth();
        int imgHeight = (int)fxImage.getHeight()
        // ---- a. Pull raw BGRA data into a *private* byte[] ----
        PixelReader pr = fxImage.getPixelReader();
        if (pr == null) {
            logger.log("❌ PANIC in write_image, PixelReader is null for image: " + fxImage);
            return false;
        }

        int pixelCount = imgWidth * imgHeight;

        // Using a fresh byte[] guarantees we start with zero‑initialized memory.
        byte[] rawBytes = new byte[pixelCount * 4];

        pr.getPixels(0, 0,
                imgWidth,
                imgHeight,
                PixelFormat.getByteBgraInstance(),
                rawBytes,
                0,
                // *** IMPORTANT: use the *actual* stride returned by getPixels() ----
                imgWidth * 4);               // usually 1; otherwise calculate real stride

        // ---- b. Premultiply Alpha (in‑place on our private buffer) ----
        for (int i = 0, base = 0; i < pixelCount; ++i, base += 4) {
            int a = rawBytes[base + 3] & 0xFF;
            if (a == 0) continue;                     // skip fully transparent pixels (avoid int‑div overflow)

            float scale = a / 255.0f;
            rawBytes[base     ] = (byte)((rawBytes[base     ] * scale));   // b
            rawBytes[base + 1] = (byte)((rawBytes[base + 1] * scale));     // g
            rawBytes[base + 2] = (byte)((rawBytes[base + 2] * scale));     // r
            // alpha stays unchanged – it is stored as‑is in the file/format you write later
        }

        // ---- c. Compute the total size we will copy to native RAM ----
        long sizeInBytes = pixelCount * 4;   // BGRA = 4 bytes per pixel

        // ---- d. Allocate a fresh MemorySegment for this operation only ----
        // (instead of re‑using an existing one that may still contain stale data)
        try (MemorySegment seg = memoryPool.acquire(sizeInBytes)) {

            // Fill it directly from the byte[] – no intermediate copy needed.
            seg.write(ByteBuffer.wrap(rawBytes));

            // Store the segment’s *address* into your native cache entry
            nativeCache.put(address, new SegmentInfo(seg.address(), sizeInBytes));
            return true;
        }
    }

    public Image read_image_as_pixel(Memory address, int width, int height) {
        // Grab the exact segment we stored earlier (size is fixed: width*height*4)
        try (MemorySegment seg = memoryPool.get(address)) {          // or however you retrieve it
            if (seg == null) {
                logger.warning("Attempted to read an image at address that was never written.");
                return null;
            }

            // Direct ByteBuffer that reads from the *exact* region we need.
            ByteBuffer buf = seg.asByteBuffer();

            // Ensure the buffer starts at position 0 and has the proper limit.
            buf.position(0);
            buf.limit(width * height * 4);   // total bytes for this image

            // The pixel format *must* match the one we wrote (premultiplied alpha)
            PixelFormat fmt = PixelFormat.getByteBgraPreInstance();

            // Create a PixelBuffer that points to this buffer.
            PixelBuffer<ByteBuffer> pb = new PixelBuffer<>(width, height,
                    buf,
                    fmt);

            // Build the WritableImage – this does NOT copy any data; it just
            // binds the native bytes directly to a JavaFX image object.
            Image img = new WritableImage(pb);

            // At this point `img` is fully populated from the native buffer.
            return img;
        }
    }*/





    //**********************************************************
    public void write_image_as_file(Image_as_file_metadata meta, Path path)
    //**********************************************************
    {
        write_file_internal(path,meta.offset());
        String tag = path.toAbsolutePath().normalize().toString();
        logger.log("write_image_as_file tag:->" + tag + "<- at aligned offset: " + meta.offset());
    }

    //**********************************************************
    public Image read_image_as_file(String tag, Image_as_file_metadata meta)
    //**********************************************************
    {
        byte[] bytes = get_bytes(meta);
        if (bytes == null) return null;

        try( ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            Image returned = new Image(bais);
            logger.log("Retrieved image FROM FILE, w= " + returned.getWidth() + " h= " + returned.getHeight());
            if ( !returned.isError())
            {
                return returned;
            }
            logger.log("error:" + returned.isError() + " " + returned.getException());
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        return null;
    }


    //**********************************************************
    public byte[] read_file(Meta meta)
    //**********************************************************
    {
        return get_bytes(meta);
    }

    //**********************************************************
    private byte[] get_bytes(Meta meta)
    //**********************************************************
    {
        MemorySegment segment = get_MemorySegment(meta);
        if (segment == null)
        {
            logger.log(" no segment for "+ meta.tag());
            return null;
        }
        return segment.toArray(ValueLayout.JAVA_BYTE);
    }


    //**********************************************************
    synchronized void clear_cache()
    //**********************************************************
    {
        current_offset.set(0);
    }

}
