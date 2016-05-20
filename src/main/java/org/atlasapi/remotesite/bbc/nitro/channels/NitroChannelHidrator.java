package org.atlasapi.remotesite.bbc.nitro.channels;

import java.io.File;

import javax.annotation.Nullable;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Image;
import org.atlasapi.media.entity.ImageTheme;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NitroChannelHidrator {

    public static final String BBC_SERVICE_NAME_SHORT = "bbc:service:name:short";
    private final Logger log = LoggerFactory.getLogger(NitroChannelHidrator.class);

    public static final String NAME = "name";
    public static final String SHORT_NAME = "shortName";
    public static final String IMAGE = "image";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";

    private final String servicesPath = "/data/youview/sv.json";
    private final String masterBrandPath = "/data/youview/mb.json";
    private final YAMLFactory yamlFactory = new YAMLFactory();
    private final ObjectMapper objectMapper = new ObjectMapper(yamlFactory);

    private final Predicate<Channel> IN_SERVICE_TABLE = new Predicate<Channel>() {

        @Override
        public boolean apply(@Nullable Channel input) {
            return locatorsToTargetInfo.containsKey(input.getCanonicalUri());
        }
    };

    private final Predicate<Channel> IN_MASTERBRAND_TABLE = new Predicate<Channel>() {

        @Override
        public boolean apply(@Nullable Channel input) {
            return masterbrandNamesToValues.containsRow(input.getTitle());
        }
    };

    private Multimap<String, String> locatorsToTargetInfo;
    private Table<String, String, String> locatorsToValues;
    private Table<String, String, String> masterbrandNamesToValues;

    public NitroChannelHidrator() {
        populateTables();
    }

    public Iterable<Channel> filterAndHydrateServices(Iterable<Channel> services) {
        Iterable<Channel> filteredServices = Iterables.filter(services, IN_SERVICE_TABLE);
        for (Channel filteredService : filteredServices) {
            String canonicalUri = filteredService.getCanonicalUri();
            filteredService.addAlias(new Alias(BBC_SERVICE_NAME_SHORT, locatorsToValues.get(
                    canonicalUri, SHORT_NAME)));
            Image image = new Image(locatorsToValues.get(canonicalUri, IMAGE));
            image.setWidth(Integer.parseInt(locatorsToValues.get(canonicalUri, WIDTH)));
            image.setHeight(Integer.parseInt(locatorsToValues.get(canonicalUri, HEIGHT)));
            image.setTheme(ImageTheme.LIGHT_OPAQUE);
            filteredService.addImage(image);
            filteredService.setTargetRegions(ImmutableSet.copyOf(locatorsToTargetInfo.get(
                    canonicalUri)));
        }
        return filteredServices;
    }

    public Iterable<Channel> filterAndHydrateMasterbrands(Iterable<Channel> masterbrands) {
        Iterable<Channel> filteredMasterbrands = Iterables.filter(masterbrands, IN_MASTERBRAND_TABLE);
        for (Channel filteredService : filteredMasterbrands) {
            String name = filteredService.getTitle();
            filteredService.addAlias(new Alias(BBC_SERVICE_NAME_SHORT, masterbrandNamesToValues.get(
                    name, SHORT_NAME)));
            Image image = new Image(masterbrandNamesToValues.get(name, IMAGE));
            image.setWidth(Integer.parseInt(masterbrandNamesToValues.get(name, WIDTH)));
            image.setHeight(Integer.parseInt(masterbrandNamesToValues.get(name, HEIGHT)));
            image.setTheme(ImageTheme.LIGHT_OPAQUE);
            filteredService.addImage(image);
        }
        return filteredMasterbrands;
    }


    private void populateTables() {
        ImmutableMultimap.Builder<String, String> locatorsToTargetInfoBuilder = ImmutableMultimap.builder();
        ImmutableTable.Builder<String, String, String> locatorsToValuesBuilder = ImmutableTable.builder();
        ImmutableTable.Builder<String, String, String> masterbrandNamesToValuesBuilder = ImmutableTable.builder();
        try {
            YouviewService[] services = objectMapper.readValue(new File(servicesPath), YouviewService[].class);
            for (YouviewService service : services) {
                for (String targetRegion : service.getTargets()) {
                    locatorsToTargetInfoBuilder.put(service.getLocator(), targetRegion);
                }
                locatorsToValuesBuilder.put(service.getLocator(), NAME, service.getName());
                locatorsToValuesBuilder.put(service.getLocator(), SHORT_NAME, service.getShortName());
                locatorsToValuesBuilder.put(service.getLocator(), IMAGE, service.getImage());
                locatorsToValuesBuilder.put(service.getLocator(), WIDTH, service.getWidth().toString());
                locatorsToValuesBuilder.put(service.getLocator(), HEIGHT, service.getHeight().toString());
            }
            locatorsToTargetInfo = locatorsToTargetInfoBuilder.build();
            locatorsToValues = locatorsToValuesBuilder.build();

            YouviewMasterbrand[] masterbrands = objectMapper.readValue(new File(masterBrandPath), YouviewMasterbrand[].class);

            for (YouviewMasterbrand masterbrand : masterbrands) {
                masterbrandNamesToValuesBuilder.put(masterbrand.getName(), SHORT_NAME, masterbrand.getShortName());
                masterbrandNamesToValuesBuilder.put(masterbrand.getName(), IMAGE, masterbrand.getImage());
                masterbrandNamesToValuesBuilder.put(masterbrand.getName(), HEIGHT, masterbrand.getHeight().toString());
                masterbrandNamesToValuesBuilder.put(masterbrand.getName(), WIDTH, masterbrand.getWidth().toString());
            }
            masterbrandNamesToValues = masterbrandNamesToValuesBuilder.build();
        } catch (Exception e) {
            log.error("Exception while processing channel configuration", e);
            throw Throwables.propagate(e);
        }
    }

}