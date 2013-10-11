package org.atlasapi.remotesite.channel4.pmlsd.epg;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.RevenueContract;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.Version;
import org.atlasapi.remotesite.ContentExtractor;
import org.atlasapi.remotesite.channel4.pmlsd.C4AtomApi;
import org.atlasapi.remotesite.channel4.pmlsd.C4Module;
import org.atlasapi.remotesite.channel4.pmlsd.epg.model.C4EpgEntry;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.metabroadcast.common.time.Clock;

public class C4EpgEntryItemExtractor implements ContentExtractor<C4EpgEntryItemSource, Item> {

    private final C4EpgEntryUriExtractor uriExtractor = new C4EpgEntryUriExtractor();
    private final C4EpgEntryBroadcastExtractor broadcastExtractor = new C4EpgEntryBroadcastExtractor();
    private final Pattern seriesEpisodeNumberPattern = Pattern.compile("/episode-guide/series-(\\d+)/episode-(\\d+)");
    private final Clock clock;
    
    public C4EpgEntryItemExtractor(Clock clock) {
        this.clock = clock;
    }
    
    @Override
    public Item extract(C4EpgEntryItemSource source) {
        C4EpgEntry entry = source.getEntry().getEpgEntry();

        Item item;
        if (source.getBrand().isPresent()) {
            Episode episode = C4Module.contentFactory().createEpisode();
            episode.setEpisodeNumber(episodeNumberFrom(source));
            episode.setSeriesNumber(seriesNumberFrom(source));
            item = episode;
        } else {
            item = C4Module.contentFactory().createItem();
        }
        
        DateTime now = clock.now();
        
        item.setLastUpdated(now);
        
        item.setCanonicalUri(uriExtractor.uriForItemId(entry));
        item.setCurie(C4AtomApi.PROGRAMMES_BASE + entry.programmeId());
        
        item.setTitle(entry.title());
        item.setDescription(entry.summary());
        item.setMediaType(MediaType.VIDEO);
        item.setSpecialization(Specialization.TV);
        
        if (entry.media() != null && !Strings.isNullOrEmpty(entry.media().thumbnail())) {
            C4AtomApi.addImages(item, entry.media().thumbnail());
        }
        
        item.addVersion(extractVersion(source.getEntry(), now));
        
        return item;
    }

    private Version extractVersion(C4EpgChannelEntry entry, DateTime now) {
        Version version = new Version();
        version.setLastUpdated(now);
        
        version.setDuration(entry.getEpgEntry().duration());
        
        Broadcast broadcast = broadcastExtractor.extract(entry);
        broadcast.setLastUpdated(now);
        version.addBroadcast(broadcast);
        
        if (entry.getEpgEntry().media() != null && !Strings.isNullOrEmpty(entry.getEpgEntry().media().player())) {
            version.addManifestedAs(extractEncoding(entry, now));
        }
        
        return version;
    }

    private Encoding extractEncoding(C4EpgChannelEntry entry, DateTime now) {
        Encoding encoding = new Encoding();
        encoding.setLastUpdated(now);

        encoding.addAvailableAt(extractLocation(entry.getEpgEntry(), now));
        
        return encoding;
    }

    private Location extractLocation(C4EpgEntry entry, DateTime now) {
        Location location = new Location();
        location.setLastUpdated(now);
        location.setUri(entry.media().player());
        location.setTransportType(TransportType.LINK);
        location.setPolicy(policyFrom(entry));
        return location;
    }

    private static final Pattern AVAILABILTY_RANGE_PATTERN = Pattern.compile("start=(.*); end=(.*); scheme=W3C-DTF");

    private Policy policyFrom(C4EpgEntry entry) {
        Policy policy = new Policy();
        policy.setRevenueContract(RevenueContract.FREE_TO_VIEW);
        
        if(!Objects.equal(policy.getAvailableCountries(), entry.media().availableCountries())) {
            policy.setAvailableCountries(entry.media().availableCountries());
        }

        if (entry.available() != null) {
            Matcher matcher = AVAILABILTY_RANGE_PATTERN.matcher(entry.available());
            if (matcher.matches()) {
                DateTime availabilityStart = new DateTime(matcher.group(1));
                if (!Objects.equal(policy.getAvailabilityStart(), availabilityStart)) {
                    policy.setAvailabilityStart(availabilityStart);
                }
                DateTime availabilityEnd = new DateTime(matcher.group(2));
                if (!Objects.equal(policy.getAvailabilityEnd(), availabilityEnd)) {
                    policy.setAvailabilityEnd(availabilityEnd);
                }
            }
        }
        policy.setLastUpdated(entry.updated());
        return policy;
    }

    private Integer seriesNumberFrom(C4EpgEntryItemSource source) {
        C4EpgEntry epgEntry = source.getEntry().getEpgEntry();
        if (epgEntry.seriesNumber() != null) {
            return epgEntry.seriesNumber();
        }
        if (source.getSeries().isPresent() && source.getSeries().get().getSeriesNumber() != null) {
            return source.getSeries().get().getSeriesNumber();
        }
        return extractSeriesNumber(uriExtractor.uriForItemHierarchy(epgEntry));
    }

    private Integer extractSeriesNumber(Optional<String> hierarchyUri) {
        if (!hierarchyUri.isPresent()) {
            return null;
        }
        Matcher matcher = seriesEpisodeNumberPattern.matcher(hierarchyUri.get());
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private Integer episodeNumberFrom(C4EpgEntryItemSource source) {
        C4EpgEntry epgEntry = source.getEntry().getEpgEntry();
        if (epgEntry.seriesNumber() != null) {
            return epgEntry.episodeNumber();
        }
        return extractEpisodeNumber(uriExtractor.uriForItemHierarchy(epgEntry));
    }

    private Integer extractEpisodeNumber(Optional<String> hierarchyUri) {
        if (!hierarchyUri.isPresent()) {
            return null;
        }
        Matcher matcher = seriesEpisodeNumberPattern.matcher(hierarchyUri.get());
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(2));
        }
        return null;
    }

}
