package klik.image_indexer;

import klik.images.Image_context;

public interface Image_source {

    Image_context get_next(boolean special);
    Image_context get_previous(boolean special);

}
