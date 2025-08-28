package klik.look;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import klik.util.log.Logger;

/*
about the copyright notice below:
this is extracted from Module javafx.swing source

reasons to do this are:

for a native build with gluonfx, one MUST remove all dependencies to AWT

and the javafx.swing package has a lot of them,
so it MUST be excluded from the build definition.

So I had to work through all my source, hunting for
import java.awt.<whatever>;
...

HOWEVER, there is one feature that I wanted to keep: TaskBar
because afaik there are only 2 ways to give a custom icon
to an app displayed in the taskbar
1. java portable multi-OS way = Taskbar = AWT => no native possible with gluonfx
2. OS specific JNI tricks (see the one I have implemented in 'MacDock', for Mac)

Remaining problem: native for Linux and Windows do not have the icons in the taskbar.

Another thing I wanted is to **keep a single source that can do both**

My solution:
Use a 'static final boolean'
set to false when compiling for gluon
so that the public 'set' method in this file
is 'disregarded' by the compiler in some way... (??)
Anyway... at run time it does not try to load the AWT lib,
and that's the target.

Not sure how this works nor if this is a 'legal/defined' feature,
but it worked for me so far, note that:
 - the code is actually compiled i.e. if you insert a syntax error
 compilation will fail!
 - if you don't do the static final boolean false trick
compilation will work but **native execution** will fail with:
UnsatisfiedLinkError: Can't load library: awt

*/

/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


//**********************************************************
public class My_taskbar_icon
//**********************************************************
{
    //**********************************************************
    public static void set(
        javafx.scene.image.Image taskbar_icon,
        String badge_text,
        klik.util.log.Logger logger)
    //**********************************************************
    {

        if (taskbar_icon == null) {
            logger.log("My_taskbar_icon.set: taskbar_icon is null");
            return;
        }


        {
            // when compiling for native with gluonfx
            // one may need to comment this code?
            // it depends how the compiler process this:
            // the thing is, at run start time, the system
            // MUST NOT try to load AWT
            // so I suspect this could be compiler dependant?
            if (java.awt.Taskbar.isTaskbarSupported())
            {
                java.awt.Taskbar task_bar = java.awt.Taskbar.getTaskbar();
                if (task_bar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE))
                {
                    java.awt.image.BufferedImage bim = fromFXImage(taskbar_icon, null, logger);
                    task_bar.setIconImage(bim);
                }
                if (task_bar.isSupported(java.awt.Taskbar.Feature.ICON_BADGE_TEXT)) {
                    task_bar.setIconBadge(badge_text);
                }
            }
        }
    }

    //**********************************************************
    public static java.awt.image.BufferedImage fromFXImage(javafx.scene.image.Image img, java.awt.image.BufferedImage bimg, Logger logger)
    //**********************************************************
    {
        PixelReader pr = img.getPixelReader();
        if (pr == null) {
            logger.log("fromFXImage FATAL: getPixelReader() failed");
            return null;
        }
        int iw = (int) img.getWidth();
        int ih = (int) img.getHeight();
        PixelFormat fxFormat = pr.getPixelFormat();

        boolean srcPixelsAreOpaque = false;
        switch (fxFormat.getType()) {
            case INT_ARGB_PRE:
            case INT_ARGB:
            case BYTE_BGRA_PRE:
            case BYTE_BGRA:
                // Check fx image opacity only if
                // supplied BufferedImage is without alpha channel
                if (bimg != null &&
                        (bimg.getType() == java.awt.image.BufferedImage.TYPE_INT_BGR ||
                                bimg.getType() == java.awt.image.BufferedImage.TYPE_INT_RGB)) {
                    srcPixelsAreOpaque = checkFXImageOpaque(pr, iw, ih);
                }
                break;
            case BYTE_RGB:
                srcPixelsAreOpaque = true;
                break;
        }
        int prefBimgType = getBestBufferedImageType(pr.getPixelFormat(), bimg, srcPixelsAreOpaque);

        //logger.log("fromFXImage image type = "+prefBimgType);

        if (bimg != null) {
            int bw = bimg.getWidth();
            int bh = bimg.getHeight();
            if (bw < iw || bh < ih || bimg.getType() != prefBimgType) {
                bimg = null;
            } else if (iw < bw || ih < bh) {
                java.awt.Graphics2D g2d = bimg.createGraphics();
                g2d.setComposite(java.awt.AlphaComposite.Clear);
                g2d.fillRect(0, 0, bw, bh);
                g2d.dispose();
            }
        }

        if (bimg == null) {
            bimg = new java.awt.image.BufferedImage(iw, ih, prefBimgType);
        }
        java.awt.image.DataBufferInt db = (java.awt.image.DataBufferInt)bimg.getRaster().getDataBuffer();
        int data[] = db.getData();

        int offset = bimg.getRaster().getDataBuffer().getOffset();
        int scan =  0;
        java.awt.image.SampleModel sm = bimg.getRaster().getSampleModel();
        if (sm instanceof java.awt.image.SinglePixelPackedSampleModel) {
            scan = ((java.awt.image.SinglePixelPackedSampleModel)sm).getScanlineStride();
        }

        WritablePixelFormat pf = getAssociatedPixelFormat(bimg);
        pr.getPixels(0, 0, iw, ih, pf, data, offset, scan);
        //logger.log("fromFXImage END!");
        return bimg;
    }

    //**********************************************************
    private static WritablePixelFormat getAssociatedPixelFormat(java.awt.image.BufferedImage bimg)
    //**********************************************************
    {
        switch (bimg.getType()) {
            // We lie here for xRGB, but we vetted that the src data was opaque
            // so we can ignore the alpha.  We use ArgbPre instead of Argb
            // just to get a loop that does not have divides in it if the
            // PixelReader happens to not know the data is opaque.
            case java.awt.image.BufferedImage.TYPE_INT_RGB:
            case java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE:
                return PixelFormat.getIntArgbPreInstance();
            case java.awt.image.BufferedImage.TYPE_INT_ARGB:
                return PixelFormat.getIntArgbInstance();
            default:
                // Should not happen...
                throw new InternalError("Failed to validate BufferedImage type");
        }
    }

    //**********************************************************
    private static boolean checkFXImageOpaque(PixelReader pr, int iw, int ih)
    //**********************************************************
    {
        for (int x = 0; x < iw; x++) {
            for (int y = 0; y < ih; y++) {
                javafx.scene.paint.Color color = pr.getColor(x,y);
                if (color.getOpacity() != 1.0) {
                    return false;
                }
            }
        }
        return true;
    }

    //**********************************************************
    static int
    getBestBufferedImageType(PixelFormat fxFormat, java.awt.image.BufferedImage bimg,
                             boolean isOpaque)
    //**********************************************************
    {
        if (bimg != null) {
            int bimgType = bimg.getType();
            if (bimgType == java.awt.image.BufferedImage.TYPE_INT_ARGB ||
                    bimgType == java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE ||
                    (isOpaque &&
                            (bimgType == java.awt.image.BufferedImage.TYPE_INT_BGR ||
                                    bimgType == java.awt.image.BufferedImage.TYPE_INT_RGB)))
            {
                // We will allow the caller to give us a BufferedImage
                // that has an alpha channel, but we might not otherwise
                // construct one ourselves.
                // We will also allow them to choose their own premultiply
                // type which may not match the image.
                // If left to our own devices we might choose a more specific
                // format as indicated by the choices below.
                return bimgType;
            }
        }
        switch (fxFormat.getType()) {
            default:
            case BYTE_BGRA_PRE:
            case INT_ARGB_PRE:
                return java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;
            case BYTE_BGRA:
            case INT_ARGB:
                return java.awt.image.BufferedImage.TYPE_INT_ARGB;
            case BYTE_RGB:
                return java.awt.image.BufferedImage.TYPE_INT_RGB;
            case BYTE_INDEXED:
                return (fxFormat.isPremultiplied()
                        ? java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE
                        : java.awt.image.BufferedImage.TYPE_INT_ARGB);
        }
    }
}

