package org.atlasapi.remotesite.channel4.pmlsd;

import java.util.Map;

import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Publisher;

import com.metabroadcast.common.time.Clock;

import com.google.common.base.Optional;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;

import static com.google.common.base.Preconditions.checkNotNull;

final class C4OnDemandEpisodeExtractor extends BaseC4EpisodeExtractor {

    private final C4AtomEntryVersionExtractor versionExtractor;
    private final C4AtomFeedUriExtractor uriExtractor = new C4AtomFeedUriExtractor();
    private final Publisher publisher;

    public C4OnDemandEpisodeExtractor(Optional<Platform> platform, Publisher publisher,
            ContentFactory<Feed, Feed, Entry> contentFactory, C4LocationPolicyIds locationPolicyIds, 
            boolean createIosBrandLocations, Clock clock) {
        super(contentFactory, clock);
        this.publisher = publisher;
        versionExtractor = new C4AtomEntryVersionExtractor(platform, locationPolicyIds, createIosBrandLocations);
    }

    @Override
    protected Episode setAdditionalEpisodeFields(Entry entry, Map<String, String> lookup,
            Episode episode) {
        String fourOdUri = C4AtomApi.fourOdUri(entry);
        String programmeId = lookup.get(C4AtomApi.DC_PROGRAMME_ID);
        if (programmeId != null) {
            episode.addAlias(new Alias(C4AtomApi.ALIAS, programmeId));
            episode.addAlias(new Alias(C4AtomApi.ALIAS_FOR_BARB, programmeId));
        }
        if (fourOdUri != null) {
            episode.addAliasUrl(fourOdUri);
        }
        
        String seriesEpisodeUri = C4AtomApi.canonicalizeEpisodeFeedId(entry);
        if(seriesEpisodeUri != null) {
            episode.addAliasUrl(seriesEpisodeUri);
        }

        String uri = uriExtractor.uriForClip(publisher, entry).get();
        checkNotNull(uri, "No version URI extracted for %s", entry.getId());

        episode.addVersion(versionExtractor.extract(
                new C4VersionData(
                        entry.getId(),
                        uri,
                        getMedia(entry),
                        lookup,
                        episode.getLastUpdated()
                )
        ));

        return episode;
    }
}
