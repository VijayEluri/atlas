package org.atlasapi.input;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.simple.Description;
import org.atlasapi.media.entity.simple.Item;
import org.atlasapi.media.entity.simple.Playlist;

import static com.google.common.base.Preconditions.checkNotNull;

public class DelegatingModelTransformer implements ModelTransformer<Description, Content> {

    private final ModelTransformer<Item, org.atlasapi.media.entity.Item> itemTransformer;
    private final ModelTransformer<Playlist, Series> seriesModelTransformer;
    private final ModelTransformer<Playlist, Brand> brandTransformer;

    public DelegatingModelTransformer(
            ModelTransformer<org.atlasapi.media.entity.simple.Playlist, Brand> brandTransformer,
            ModelTransformer<Item, org.atlasapi.media.entity.Item> itemTransformer,
            ModelTransformer<Playlist, Series> seriesModelTransformer) {
        this.brandTransformer = checkNotNull(brandTransformer);
        this.itemTransformer = checkNotNull(itemTransformer);
        this.seriesModelTransformer = checkNotNull(seriesModelTransformer);
    }

    @Override
    public Content transform(Description simple) {
        if (simple instanceof Playlist && simple.getType().equalsIgnoreCase("series")) {
            return seriesModelTransformer.transform((Playlist) simple);
        } else if (simple instanceof Playlist && simple.getType().equalsIgnoreCase("brand")) {
            return brandTransformer.transform((Playlist) simple);
        } else if (simple instanceof Item) {
            return itemTransformer.transform((Item) simple);
        }
        throw new IllegalArgumentException("Can't transform " + simple.getClass());
    }
}