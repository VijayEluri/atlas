package org.atlasapi.remotesite.btvod;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Set;

import org.atlasapi.media.entity.Image;
import org.atlasapi.media.entity.ImageType;
import org.atlasapi.remotesite.btvod.model.BtVodEntry;
import org.atlasapi.remotesite.btvod.model.BtVodImage;

import com.google.api.client.util.Maps;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * Extract low-resolution brand images from MPX feed.
 * 
 * Images aren't provided at the brand level, however. we
 * must find the images from the first episode, ordered editorially.
 * 
 * To do this in an efficient manner during ingest, and avoid writing a
 * brand many times, users of this class are expected to pre-process the
 * feed so the correct images for each brand can be computed before
 * brands are written.
 *
 */
public class DerivingFromItemBrandImageExtractor implements BrandImageExtractor, BtVodDataProcessor<Void> {

    private final String baseUrl;
    private final Map<String, BrandImage> backgroundImages = Maps.newHashMap();
    private final Map<String, BrandImage> packshotImages = Maps.newHashMap();
    private final BrandUriExtractor brandUriExtractor;
    private final BtVodSeriesUriExtractor seriesUriExtractor;

    public DerivingFromItemBrandImageExtractor(
            BrandUriExtractor brandUriExtractor,
            String baseUrl,
            BtVodSeriesUriExtractor seriesUriExtractor
    ) {
        this.baseUrl = checkNotNull(baseUrl);
        this.brandUriExtractor = checkNotNull(brandUriExtractor);
        this.seriesUriExtractor = checkNotNull(seriesUriExtractor);
    }
    
    @Override
    public Set<Image> extractImages(BtVodEntry entry) {
        Optional<String> brandUri = brandUriExtractor.extractBrandUri(entry);
        if (!brandUri.isPresent()) {
            return ImmutableSet.of();
        }
        
        ImmutableSet.Builder<Image> images = ImmutableSet.builder();
        BrandImage backgroundImage = backgroundImages.get(brandUri.get());
        
        if (backgroundImage != null) {
            images.add(buildImage(backgroundImage.image, ImageType.ADDITIONAL, false));
        }
        
        BrandImage packshotImage = packshotImages.get(brandUri.get());
        if (packshotImage != null) {
            images.add(buildImage(packshotImage.image, ImageType.PRIMARY, true));
        }
        
        return images.build();
    }

    @Override
    public boolean process(BtVodEntry entry) {
        Optional<String> brandUri = brandUriExtractor.extractBrandUri(entry);
        if (BrandUriExtractor.SERIES_TYPE.equals(entry.getProductType()) || !brandUri.isPresent()) {
            return true;
        }
        Integer seriesNumber = seriesUriExtractor.extractSeriesNumber(entry).orNull();
        Integer episodeNumber = BtVodItemWriter.extractEpisodeNumber(entry);
        if (episodeNumber != null && seriesNumber != null) {
            retainIfBestImage(brandUri.get(), backgroundImages, getBackgroundImage(entry), seriesNumber, episodeNumber);
            retainIfBestImage(brandUri.get(), packshotImages, getPackshotDoubleImage(entry), seriesNumber, episodeNumber);
        }
        return true;
    }
    
    private void retainIfBestImage(String brandUri, Map<String, BrandImage> images,
            BtVodImage image, Integer seriesNumber, Integer episodeNumber) {
        
        if (image == null) {
            return;
        }
        
        BrandImage current = images.get(brandUri);
        
        if (current == null 
                || newImageIsForEarlierEpisode(current, seriesNumber, episodeNumber)) {
            images.put(brandUri, 
                        new BrandImage(seriesNumber, 
                                       episodeNumber, 
                                       image));
        }
    }

    private boolean newImageIsForEarlierEpisode(BrandImage current, Integer seriesNumber,
            Integer episodeNumber) {
        
        return (seriesNumber < current.seriesNumber)
                || (seriesNumber == current.seriesNumber && episodeNumber < current.episodeNumber);
    }

    private BtVodImage getBackgroundImage(BtVodEntry row) {
        return Iterables.getFirst(row.getProductImages().getBackgroundImages(), null);
    }
    
    private BtVodImage getPackshotDoubleImage(BtVodEntry row) {
        return Iterables.getFirst(row.getProductImages().getPackshotDoubleImages(), null);
    }

    @Override
    public Void getResult() {
        return null;
    }
    
    private static class BrandImage {
        private int seriesNumber;
        private int episodeNumber;
        private BtVodImage image;
        
        public BrandImage(int seriesNumber, int episodeNumber, BtVodImage image) {
            this.seriesNumber = seriesNumber;
            this.episodeNumber = episodeNumber;
            this.image = image;
        }
    }
    
    private Image buildImage(BtVodImage btVodImage, ImageType imageType, boolean hasTitleArt) {
        return Image.builder(uriFor(btVodImage))
                .withHeight(btVodImage.getPlproduct$height())
                .withWidth(btVodImage.getPlproduct$width())
                .withType(imageType)
                .withHasTitleArt(hasTitleArt)
                .build();
    }

    private String uriFor(BtVodImage image) {
        return String.format(
                "%s%s",
                baseUrl,
                image.getPlproduct$url()
        );
    }

    @Override
    public void start() {
        backgroundImages.clear();
        packshotImages.clear();
    }
}
