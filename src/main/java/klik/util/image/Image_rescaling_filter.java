package klik.util.image;

import app.photofox.vipsffm.enums.VipsKernel;

public enum Image_rescaling_filter
{
    Native, // this is the default JavaFX
    NearestNeighbour,
    Linear,
    Cubic,
    Mitchell,
    Lanczos2,
    Lanczos3,
    MagicKernelSharp2013,
    MagicKernelSharp2021
    ;

    VipsKernel get() {
        switch (this)
        {
            case Native:
            {
                return null;
            }
            case NearestNeighbour:
            {
                return VipsKernel.KERNEL_NEAREST;
            }
            case Linear:
            {
                return VipsKernel.KERNEL_LINEAR;
            }
            case Cubic:
            {
                return VipsKernel.KERNEL_CUBIC;
            }
            case Mitchell:
            {
                return VipsKernel.KERNEL_MITCHELL;
            }
            case Lanczos2:
            {
                return VipsKernel.KERNEL_LANCZOS2;
            }
            case Lanczos3:
            {
                return VipsKernel.KERNEL_LANCZOS3;
            }
            case MagicKernelSharp2013:
            {
                return VipsKernel.KERNEL_MKS2013;
            }
            case MagicKernelSharp2021:
            {
                return VipsKernel.KERNEL_MKS2021;
            }

        }
        return null;
    }

    public String get_String()
    {
        switch (this)
        {
            case Native:
            {
                return "JavaFX default rescaler ";
            }
            case NearestNeighbour:
            {
              return "Nearest Neighbor rescaler ";
            }
            case Linear:
            {
                return "Linear rescaler ";
            }
            case Cubic:
            {
                return "Cubic rescaler ";
            }
            case Mitchell:
            {
                return "Mitchell rescaler ";
            }
            case Lanczos2:
            {
                return "Lanczos2 rescaler ";
            }
            case Lanczos3:
            {
                return "Lanczos3 rescaler ";
            }
            case MagicKernelSharp2021:
            {
                return "Magic Kernel Sharp 2021 rescaler ";
            }
            case MagicKernelSharp2013:
            {
                return "Magic Kernel Sharp 2013 rescaler ";
            }
        }
        return "FATAL ERROR";
    }

    public Image_rescaling_filter next()
    {
        Image_rescaling_filter[] vals= values();
        int next = (ordinal()+1)%values().length;
        return vals[next];
    }
    public Image_rescaling_filter previous()
    {
        Image_rescaling_filter[] vals= values();
        int previous = (ordinal()-1 + values().length)%values().length;
        return vals[previous];
    }
}
