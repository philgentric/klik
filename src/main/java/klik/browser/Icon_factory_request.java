package klik.browser;

//**********************************************************
public class Icon_factory_request
//**********************************************************
{
    public final Exception_recorder exception_recorder; // for error reporting
    public final double icon_size;
    public final Item_image destination;
    //**********************************************************
    public Icon_factory_request(Item_image destination_,double icon_size_, Exception_recorder exception_recorder_)
    //**********************************************************
    {
        icon_size = icon_size_;
        destination = destination_;
        exception_recorder = exception_recorder_;
    }
}
