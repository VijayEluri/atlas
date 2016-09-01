package org.atlasapi.remotesite.bbc.nitro.extract;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Image;
import org.atlasapi.remotesite.ContentExtractor;

/**
 * Extracts an {@link Image} from a
 * {@link com.metabroadcast.atlas.glycerin.model.Image} according to the provided dimensions.
 */
public class NitroImageExtractor implements
        ContentExtractor<com.metabroadcast.atlas.glycerin.model.Brand.Images.Image, Image> {

    private static final String BBC_NITRO_TYPE = "bbc:nitro:type";
    private static final String IDENT = "ident";
    private static final String $_RECIPE = "$recipe";
    public static final String RESOLUTION = "1024x576";

    private final String recipe;
    private final int width;
    private final int height;

    /**
     * Create a new extractor which extracts {@link Image}s with the provided
     * dimensions.
     *
     * @param width  - the width of the image
     * @param height - the height of the image
     */
    public NitroImageExtractor(int width, int height) {
        checkArgument(width > 0, "width should be a positive number");
        checkArgument(height > 0, "height should be a positive number");

        this.width = width;
        this.height = height;
        this.recipe = String.format("%dx%d", width, height);
    }

    @Override
    public Image extract(com.metabroadcast.atlas.glycerin.model.Brand.Images.Image source) {
        checkNotNull(source, "null image source");
        checkNotNull(source.getTemplateUrl(), "null image template");
        String url = source.getTemplateUrl().replace("$recipe", recipe);

        if(!url.contains("http://")) {
            url = "http://" + url ;
        }
        if (url.contains($_RECIPE)) {
            url = url.replace($_RECIPE, RESOLUTION);
        }
        Image image = new Image(url);
        image.setWidth(1024);
        image.setHeight(576);
        image.addAlias(new Alias(BBC_NITRO_TYPE, IDENT));

        return image;
    }

}
