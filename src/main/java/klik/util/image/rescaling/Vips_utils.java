// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klik.util.image.rescaling;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import app.photofox.vipsffm.enums.VipsBandFormat;
import app.photofox.vipsffm.enums.VipsInterpretation;
import app.photofox.vipsffm.enums.VipsKernel;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import klik.util.log.Logger;
import klik.util.ui.Popups;

import java.lang.foreign.Arena;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

//
// vips is a fast image processing lib implemented in C
// see https://github.com/libvips/libvips
//
// at this time, we use vips to provide alternate rescalers
// to the default one in ImageView (see below)
// it is very fast (but not as fast the default)
//
// technically, it seems that:
// OpenJFX uses Prism ES2 which is based on OpenGL, which Apple has deprecated
//  with JFX26 build4 and above a metal-based solution will be available,
// but it seems that on Mac it will not use the advanced 'metal' HW features?
//
// 1. ImageView
// setSmooth(true)  would be a linear filter (GL_LINEAR)
// setSmooth(false)  would be a nearest neighbor (GL_NEAREST)
//
// 2. if downscale is requested to the Image constructor,
// it goes through ImageTools with simple subsampling and maybe a bilinear


// there is a FFM (Foreign Function and Memory aka "Panama") adapter
// to use it from java:
// see https://github.com/lopcode/vips-ffm
// see in build.gradle how this lib is imported
//
// prerequisites: install vips, on mac it is super simple:
// brew install vips
//
// Note: criteo as a JNI adapter see https://github.com/criteo/JVips, I did not try it

//**********************************************************
public class Vips_utils
//**********************************************************
{


    //**********************************************************
    public static Image VImage_to_FX_Image(VImage in, Logger logger)
    //**********************************************************
    {
        int w = in.getWidth();
        int h = in.getHeight();
        VImage vimage = in.colourspace(VipsInterpretation.INTERPRETATION_sRGB);
        MemorySegment memory_segment = vimage.writeToMemory();
        WritableImage writable_image = new WritableImage(w,h);
        PixelWriter pixel_writer = writable_image.getPixelWriter();
        int bands = 3; //(RGB)
        for ( int y = 0; y < h ; y++)
        {
            for ( int x = 0; x < w; x++)
            {
                long index = (y*w+x)*bands;
                int r = Byte.toUnsignedInt(memory_segment.get(ValueLayout.JAVA_BYTE,index));
                int g = Byte.toUnsignedInt(memory_segment.get(ValueLayout.JAVA_BYTE,index+1));
                int b = Byte.toUnsignedInt(memory_segment.get(ValueLayout.JAVA_BYTE,index+2));

                Color color = Color.rgb(r,g,b);
                pixel_writer.setColor(x,y,color);
            }
        }
        //System.out.println("fx image acquired from VImage");

        return writable_image;
    }

    //**********************************************************
    public static VImage FX_Image_to_VImage(Image in, Arena arena, Logger logger)
    //**********************************************************
    {
        int w = (int) in.getWidth();
        int h = (int) in.getHeight();
        PixelReader pixel_reader = in.getPixelReader();
        if ( pixel_reader == null)
        {
            logger.log("FX_Image_to_VImage: pixel_reader is null ");
            return null;
        }
        int bands = 3;
        //System.out.println("Arena.ofConfined() OK !"+arena.toString());
        MemorySegment memory_segment = arena.allocate(w*h*3);
        for ( int y = 0; y < h ; y++)
        {
            for (int x = 0; x < w; x++)
            {
                Color color = pixel_reader.getColor(x,y);
                long index = (y * w + x) * bands;

                memory_segment.setAtIndex(ValueLayout.JAVA_BYTE,index,(byte)(color.getRed()*255));
                memory_segment.setAtIndex(ValueLayout.JAVA_BYTE,index+1,(byte)(color.getGreen()*255));
                memory_segment.setAtIndex(ValueLayout.JAVA_BYTE,index+2,(byte)(color.getBlue()*255));
            }
        }
        int format = VipsBandFormat.FORMAT_UCHAR.getRawValue();
        try {

            VImage returned = VImage.newFromMemory(arena, memory_segment, w, h, 3, format);

            //System.out.println("VImage acquired from FX Image");
            return returned;
        }
        catch (UnsatisfiedLinkError e)
        {
            Popups.popup_warning("❗ VIPS not installed","Rescaling with non-default filter(s) requires VIPS",true,null,logger);
            return null;
        }
    }


    //**********************************************************
    public static Image resize(Image in, double scale, Image_rescaling_filter filter, Logger logger)
    //**********************************************************
    {
        Arena arena = Arena.ofConfined();
        VImage before = FX_Image_to_VImage(in,arena,logger);
        if ( before == null)
        {
            System.out.println("failed to translate FX Image to VImage");
            return null;
        }


        VipsKernel k = filter.get();
        VipsOption o = VipsOption.Enum("kernel",k);
        VImage after = before.resize(scale, o);
        if ( after == null)
        {
            System.out.println("failed to resize VImage");
            return null;
        }
        return VImage_to_FX_Image(after,logger);
    }



    static
    {
        // I found 4 documented ways to make vips FFM work.
        // In the 3 first cases, BAD SHAME, one needs to "hardcode" the library path
        // ... might break the code after a "brew upgrade"

        // Method1: does NOT work for me
        //  System.setProperty("java.library.path","/usr/local/lib:/opt/homebrew/Cellar/vips/8.17.2/lib");
        //  String s = System.getProperty("java.library.path");
        //  System.out.println("java.library.path="+s);
        //  System.loadLibrary("vips");

        // Method2: does NOT work for me
        //  System.load("/opt/homebrew/Cellar/vips/8.17.2/lib/libvips.dylib");

        // the typical error is:
        // UnsatisfiedLinkError: could not make loader for libvips
        //        at app.photofox.vipsffm.VipsLibLookup.buildSymbolLoader(VipsLibLookup.java:14)

        // Method3: works for me
        // use the override ( see makeOptionalLibraryLookup in VipsLibLookup)
        //System.setProperty("vipsffm.libpath.vips.override","/opt/homebrew/Cellar/vips/8.17.2/lib/libvips.dylib");
        //System.setProperty("vipsffm.libpath.glib.override","/opt/homebrew/Cellar/glib/2.86.0/lib/libglib-2.0.0.dylib");
        //System.setProperty("vipsffm.libpath.gobject.override","/opt/homebrew/Cellar/glib/2.86.0/lib/libgobject-2.0.0.dylib");

        // Method4: works for me
        // see https://github.com/lopcode/vips-ffm/discussions/132
        // I copied the code, (changed the log)
        // this code looks for the libs and uses the override
        // so it is better than a hardcode

        findAndSetMacLibraries();
    }


    //**********************************************************
    private static Path scanLibrary(List<String> paths, List<String> names)
    //**********************************************************
    {
        for (String path : paths) {
            for (String name : names) {
                String libName = System.mapLibraryName(name);
                Path libPathName = Path.of(path, libName);
                System.out.println("Checking for presence of "+ libPathName);
                if (Files.exists(libPathName)) {
                    System.out.println("Found "+ libPathName);
                    return libPathName;
                }
            }
        }
        return null;
    }

    //**********************************************************
    private static void setPropertyWhenLibraryExists(String property, List<String> paths, String... names)
    //**********************************************************
    {
        Path lib = scanLibrary(paths, List.of(names));
        if (lib != null) {
            System.out.println("Setting "+ property+ " to " + lib);
            System.setProperty(property, lib.toString());
        }
    }

    //**********************************************************
    private static void findAndSetMacLibraries()
    //**********************************************************
    {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (!os.contains("mac")) {
            // not a mac so bail out
            return;
        }
        List<String> libPaths = List.of(
                "/opt/homebrew/lib", // homebrew on apple silicon
                "/usr/local/lib", // homebrew on intel
                "/opt/sw/lib", // fink
                "/opt/local/lib" // macports
        );
        setPropertyWhenLibraryExists("vipsffm.libpath.vips.override", libPaths, "vips");
        setPropertyWhenLibraryExists("vipsffm.libpath.glib.override", libPaths, "glib-2.0.0");
        setPropertyWhenLibraryExists("vipsffm.libpath.gobject.override", libPaths, "gobject-2.0.0");
    }



/*
    // diagnostic code

    //private static final String LIB_NAME_LONG = "/opt/homebrew/Cellar/vips/8.17.2/lib/libvips.dylib";
    //private static final String LIB_NAME_LONG = "vips";

    private static final String LIB_NAME_SHORT = "vips";
    private static final Path LIB_DIR = Paths.get("/opt/homebrew/Cellar/vips/8.17.2/lib");
    // Adjust version if you want to try a specific one first; leave null to auto-detect
    private static final String OPTIONAL_VERSIONED = null; // e.g. "libvips.42.dylib"

    public static boolean check() {
        System.out.println("=== libvips diagnostics ===");
        archInfo();
        printInputArgs();
        if (!checkJavaLibraryPath()) return false;
        if (!listCandidateFiles()) return false;
        if (!testSystemLoadLibrary()) return false;
        if (!testAbsoluteLoad()) return false;
        if (!testFFMLookup()) return false;

        {
            String x = System.mapLibraryName("vips");
            System.out.println("x="+x);
        }
        System.out.println("=== done ===");
        return  true;
    }

    private static void archInfo() {
        System.out.println("[Arch]");
        System.out.println("os.name=" + System.getProperty("os.name"));
        System.out.println("os.arch=" + System.getProperty("os.arch"));
        System.out.println("sun.arch.data.model=" + System.getProperty("sun.arch.data.model"));
    }

    private static void printInputArgs() {
        System.out.println("[JVM Input Arguments]");
        RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
        mx.getInputArguments().forEach(a -> System.out.println("  " + a));
    }

    private static boolean checkJavaLibraryPath()
    {
        {
            System.out.println("[java.library.path check]");
            String prop = System.getProperty("java.library.path", "");
            List<String> entries = Arrays.asList(prop.split(File.pathSeparator));
            System.out.println("java.library.path entries:");
            entries.forEach(e -> System.out.println("  " + e));
            boolean ok = entries.stream().anyMatch(p -> {
                try {
                    return Paths.get(p).toRealPath().equals(LIB_DIR.toRealPath());
                } catch (Exception ex) {
                    return false;
                }
            });
            System.out.println("Contains /opt/homebrew/lib (real path match)=" + ok);
            if (!ok) return false;
        }

        return true;
    }

    private static boolean listCandidateFiles() {
        System.out.println("[Candidate libvips files]");
        if (!Files.isDirectory(LIB_DIR)) {
            System.out.println("Directory missing: " + LIB_DIR);
            return false;
        }
        try (Stream<Path> s = Files.list(LIB_DIR)) {
            List<Path> libs = s.filter(p -> p.getFileName().toString().startsWith("libvips"))
                    .sorted()
                    .collect(Collectors.toList());
            if (libs.isEmpty()) {
                System.out.println("No files starting with libvips found.");
            } else {
                for (Path p : libs) {
                    String target;
                    try {
                        target = Files.isSymbolicLink(p) ? " -> " + Files.readSymbolicLink(p) : "";
                    } catch (Exception e) {
                        target = " (symlink read error: " + e.getMessage() + ")";
                    }
                    System.out.println("  " + p.getFileName() + target);
                }
                return true;
            }
        } catch (Exception e) {
            System.out.println("Error listing: " + e);
        }
        return false;
    }

    private static boolean testSystemLoadLibrary() {
        System.out.println("[System.loadLibrary(\"" + LIB_NAME_SHORT + "\")]");
        try {
            System.loadLibrary(LIB_NAME_SHORT);
            System.out.println("SUCCESS: loadLibrary(\"" + LIB_NAME_SHORT + "\")");
            return true;
        } catch (Throwable t) {
            System.out.println("FAIL: " + t);
        }
        return false;
    }

    private static boolean testAbsoluteLoad() {
        System.out.println("[System.load absolute]");
        List<Path> candidates = new ArrayList<>();
        if (OPTIONAL_VERSIONED != null) {
            candidates.add(LIB_DIR.resolve(OPTIONAL_VERSIONED));
        }
        candidates.add(LIB_DIR.resolve("lib" + LIB_NAME_SHORT + ".dylib"));
        // Add discovered versioned ones
        try (Stream<Path> s = Files.list(LIB_DIR)) {
            s.filter(p -> p.getFileName().toString().matches("libvips\\.\\d+.*\\.dylib"))
                    .forEach(candidates::add);
        } catch (Exception e) {
            System.out.println("" + e);
            return false;
        }
        for (Path p : candidates) {
            if (!Files.exists(p)) {
                System.out.println("Skip (missing): " + p);
                continue;
            }
            try {
                System.load(p.toAbsolutePath().toString());
                System.out.println("SUCCESS: System.load(" + p + ")");
                return true;  // Stop after first success
            } catch (Throwable t) {
                System.out.println("FAIL: System.load(" + p + ") -> " + t);
            }
        }
        return false;
    }

    private static boolean testFFMLookup()
    {
        System.out.println("[FFM SymbolLookup test]");

        try {
            // JDK 22+ API (adjust if earlier)
            var arena = java.lang.foreign.Arena.ofShared();
            var lookup = java.lang.foreign.SymbolLookup.libraryLookup(LIB_NAME_LONG, arena);
            var symbol = lookup.find("vips_init"); // A common symbol in libvips
            System.out.println("lookup.find(\"vips_init\") present=" + symbol.isPresent());
            if ( symbol.isPresent())         return true;
        } catch (IllegalCallerException ice) {
            System.out.println("Likely missing --enable-native-access flag: " + ice);
        } catch (Throwable t) {
            System.out.println("lookup.find failed: " + t);
        }
        return false;
    }
*/
}

