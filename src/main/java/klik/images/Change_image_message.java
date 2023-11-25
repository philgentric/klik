package klik.images;

import klik.actor.Aborter;
import klik.actor.Message;
import klik.util.Logger;

//**********************************************************
public class Change_image_message implements Message
//**********************************************************
{
    public final Logger logger;
    public final int delta;
    public final Image_window image_stage;
    public final boolean ultimate;
    public final Image_context input_image_context;
    public final Image_context[] output_image_context;
    public final Aborter aborter;

    //**********************************************************
    public Change_image_message(int delta, Image_context image_context, Image_window image_stage, boolean ultimate, Image_context[] returned, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.delta = delta;
        this.input_image_context = image_context;
        this.image_stage = image_stage;
        this.ultimate = ultimate;
        this.output_image_context = returned;
        aborter = new Aborter();
    }

    @Override
    public Aborter get_aborter() {
        return aborter;
    }
}
