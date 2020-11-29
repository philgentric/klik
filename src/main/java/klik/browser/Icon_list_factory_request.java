package klik.browser;

import java.util.List;

//**********************************************************
public class Icon_list_factory_request
//**********************************************************
{
    public final double icon_size;
    // when both fields are null, this a "hara-kiri" message i.e. no more icons to make, please shutdown yourself
    public final List<Item_image> list;
    //**********************************************************
    public Icon_list_factory_request(List<Item_image> list_, double icon_size_)
    //**********************************************************
    {
        icon_size = icon_size_;
        list = list_;
    }
}
