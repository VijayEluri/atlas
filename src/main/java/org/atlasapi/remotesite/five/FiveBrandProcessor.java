package org.atlasapi.remotesite.five;

import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.atlasapi.genres.GenreMap;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Image;
import org.atlasapi.media.entity.ImageType;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.atlasapi.remotesite.ContentMerger;
import org.atlasapi.remotesite.ContentMerger.MergeStrategy;

import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.http.HttpResponse;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.NodeFactory;
import nu.xom.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.media.entity.Specialization.FILM;

public class FiveBrandProcessor {

    private static final Logger log = LoggerFactory.getLogger(FiveBrandProcessor.class);

    private static final Pattern FILM_YEAR = Pattern.compile(".*\\((\\d{4})\\)$");

    private final static String WATCHABLES_URL_SUFFIX = "/watchables?expand=season%7Ctransmissions";
    private final ContentWriter writer;
    private final GenreMap genreMap = new FiveGenreMap();
    private final FiveEpisodeProcessor episodeProcessor;
    private final String baseApiUrl;
    private final RemoteSiteClient<HttpResponse> httpClient;
    private final ContentResolver contentResolver;
    private final ContentMerger contentMerger;

    private FiveBrandProcessor(
            ContentWriter writer,
            ContentResolver contentResolver,
            String baseApiUrl,
            RemoteSiteClient<HttpResponse> httpClient,
            Multimap<String, Channel> channelMap,
            FiveLocationPolicyIds locationPolicyIds
    ) {
        this.writer = checkNotNull(writer);
        this.baseApiUrl = checkNotNull(baseApiUrl);
        this.httpClient = checkNotNull(httpClient);
        this.contentResolver = checkNotNull(contentResolver);
        this.episodeProcessor = FiveEpisodeProcessor.create(
                baseApiUrl,
                httpClient,
                channelMap,
                locationPolicyIds
        );
        this.contentMerger = new ContentMerger(
                MergeStrategy.REPLACE,
                MergeStrategy.KEEP,
                MergeStrategy.REPLACE
        );
    }

    public static FiveBrandProcessor create(
            ContentWriter writer,
            ContentResolver contentResolver,
            String baseApiUrl,
            RemoteSiteClient<HttpResponse> httpClient,
            Multimap<String, Channel> channelMap,
            FiveLocationPolicyIds locationPolicyIds
    ) {
        return new FiveBrandProcessor(
                writer,
                contentResolver,
                baseApiUrl,
                httpClient,
                channelMap,
                locationPolicyIds
        );
    }

    public void processShow(Element element) {
        Brand brand = extractBrand(element);

        String id = childValue(element, "id");

        EpisodeProcessingNodeFactory nodeFactory = EpisodeProcessingNodeFactory.create(
                brand, episodeProcessor
        );

        try {
            String responseBody = httpClient.get(getShowUri(id) + WATCHABLES_URL_SUFFIX).body();
            new Builder(nodeFactory).build(new StringReader(responseBody));
        } catch(Exception e) {
            log.error("Exception parsing episodes for brand " + brand.getTitle(), e);
            return;
        }

        if(FILM.equals(brand.getSpecialization()) && nodeFactory.items.size() == 1) {
            setFilmDescription((Film)Iterables.getOnlyElement(nodeFactory.items), element);
        }

        write(brand);

        episodeProcessor.getSeriesMap()
                .values()
                .forEach(this::write);

        nodeFactory.items
                .forEach(item -> write(brand, item));
    }

    private void write(Brand brand, Item itemToWrite) {
        itemToWrite.setContainer(brand);

        Maybe<Identified> maybeExisting = contentResolver.findByCanonicalUris(
                ImmutableSet.of(itemToWrite.getCanonicalUri())
        )
                .getFirstValue();

        if (maybeExisting.hasValue()) {
            itemToWrite = contentMerger.merge((Item) maybeExisting.requireValue(), itemToWrite);
        }

        writer.createOrUpdate(itemToWrite);
    }

    private void write(Container containerToWrite) {
        Maybe<Identified> maybeExisting = contentResolver.findByCanonicalUris(
                ImmutableSet.of(containerToWrite.getCanonicalUri())
        )
                .getFirstValue();

        if (maybeExisting.hasValue()) {
            containerToWrite = contentMerger.merge(
                    (Container) maybeExisting.requireValue(),
                    containerToWrite
            );
        }

        writer.createOrUpdate(containerToWrite);
    }

    private Brand extractBrand(Element element) {
        String id = childValue(element, "id");
        String uri = getShowUri(id);

        Maybe<Identified> maybeBrand = contentResolver.findByCanonicalUris(
                ImmutableSet.of(uri)
        )
                .getFirstValue();

        Brand brand = createBrand(element);

        if (maybeBrand.hasValue()) {
            return mergeBrand((Brand) maybeBrand.requireValue(), brand);
        } else {
            return brand;
        }
    }

    private Brand mergeBrand(Brand current, Brand extracted) {
        current.setCurie(extracted.getCurie());

        current.setTitle(extracted.getTitle());
        current.setDescription(extracted.getDescription());
        current.setGenres(extracted.getGenres());

        current.setImage(extracted.getImage());
        current.setImages(extracted.getImages());

        current.setMediaType(extracted.getMediaType());
        current.setSpecialization(extracted.getSpecialization());

        return current;
    }

    private Brand createBrand(Element element) {
        String id = childValue(element, "id");
        String uri = getShowUri(id);
        
        Brand brand = new Brand(uri, getBrandCurie(id), Publisher.FIVE);
        brand.setTitle(childValue(element, "title"));
        brand.setDescription(getDescription(element).valueOrNull());
        brand.setGenres(getGenres(element));

        Maybe<Image> imageMaybe = getImage(element);

        if (imageMaybe.hasValue()) {
            Image image = imageMaybe.requireValue();
            brand.setImage(image.getCanonicalUri());
            brand.setImages(ImmutableSet.of(image));
        } else {
            brand.setImage(null);
        }

        brand.setMediaType(MediaType.VIDEO);
        brand.setSpecialization(specializationFrom(element));

        return brand;  
    }

    private void setFilmDescription(Film film, Element element) {
        Maybe<String> description = getDescription(element);

        if(description.hasValue()) {
            film.setDescription(description.requireValue());
        }

        String shortDesc = childValue(element, "short_description");
        if(!Strings.isNullOrEmpty(shortDesc)) {
            Matcher matcher = FILM_YEAR.matcher(shortDesc);
            if(matcher.matches()) {
                film.setYear(Integer.parseInt(matcher.group(1)));
            }
        }
    }

    private Specialization specializationFrom(Element element) {
        String progType = childValue(element, "programme_type");

        if(progType.equals("Feature Film") || progType.equals("TV Movie")) {
            return Specialization.FILM;
        }

        return Specialization.TV;
    }

    private String getShowUri(String id) {
        return baseApiUrl + "/shows/" + id;
    }

    private String getBrandCurie(String id) {
        return "five:b-" + id;
    }

    @Nullable
    private String childValue(Element element, String childName) {
        Element firstChild = element.getFirstChildElement(childName);
        if(firstChild != null) {
            return firstChild.getValue();
        }
        return null;
    }

    private Maybe<String> getDescription(Element element) {
        String longDescription = element.getFirstChildElement("long_description").getValue();
        if (!Strings.isNullOrEmpty(longDescription)) {
            return Maybe.just(longDescription);
        }

        String shortDescription = element.getFirstChildElement("short_description").getValue();
        if (!Strings.isNullOrEmpty(shortDescription)) {
            return Maybe.just(shortDescription);
        }

        return Maybe.nothing();
    }

    private Set<String> getGenres(Element element) {
        return genreMap.mapRecognised(ImmutableSet.of(
                "http://www.five.tv/genres/" + element.getFirstChildElement("genre").getValue())
        );
    }

    private Maybe<Image> getImage(Element element) {
        Elements imageElements = element
                .getFirstChildElement("images")
                .getChildElements("image");

        if (imageElements.size() > 0) {
            String image = imageElements.get(0).getValue();

            if(!image.contains("http://")) {
                image = "http://" + image;
            }

            Image imageObj = new Image(image);

            if (image.contains("api-images.channel5.com/images/default")) {
                imageObj.setType(ImageType.GENERIC_IMAGE_CONTENT_PLAYER);

            }
            return Maybe.just(imageObj);
        }

        return Maybe.nothing();
    }

    private static class EpisodeProcessingNodeFactory extends NodeFactory {

        private final FiveEpisodeProcessor episodeProcessor;
        private final List<Item> items = Lists.newArrayList();
        private final Brand brand;

        private EpisodeProcessingNodeFactory(Brand brand, FiveEpisodeProcessor episodeProcessor) {
            this.brand = checkNotNull(brand);
            this.episodeProcessor = checkNotNull(episodeProcessor);
        }

        public static EpisodeProcessingNodeFactory create(
                Brand brand,
                FiveEpisodeProcessor episodeProcessor
        ) {
            return new EpisodeProcessingNodeFactory(brand, episodeProcessor);
        }

        @Override
        public Nodes finishMakingElement(Element element) {
            if (element.getLocalName().equalsIgnoreCase("watchable")) {
                try {
                    items.add(episodeProcessor.processEpisode(element, brand));
                }
                catch (Exception e) {
                    log.error("Exception when processing episode", e);
                }

                return new Nodes();
            }
            else {
                return super.finishMakingElement(element);
            }
        }
    }
}
