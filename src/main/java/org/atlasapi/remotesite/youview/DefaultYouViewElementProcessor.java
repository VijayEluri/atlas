package org.atlasapi.remotesite.youview;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import nu.xom.Element;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.ScheduleEntry.ItemRefAndBroadcast;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.lookup.entry.LookupEntry;
import org.atlasapi.persistence.lookup.entry.LookupEntryStore;
import org.atlasapi.remotesite.ContentMerger;
import org.atlasapi.remotesite.ContentMerger.MergeStrategy;

import static com.google.common.base.Preconditions.checkNotNull;


public class DefaultYouViewElementProcessor implements YouViewElementProcessor {

    private final YouViewContentExtractor extractor;
    private final ContentResolver resolver;
    private final ContentWriter writer;
    private final ContentMerger contentMerger;
    private final LookupEntryStore lookupEntryStore;
    private final Predicate<Alias> scheduleEventAliasPredicate;
    
    public DefaultYouViewElementProcessor(YouViewContentExtractor extractor, ContentResolver resolver, 
            ContentWriter writer, LookupEntryStore lookupEntryStore) {
        this.extractor = checkNotNull(extractor);
        this.resolver = checkNotNull(resolver);
        this.writer = checkNotNull(writer);
        this.lookupEntryStore = checkNotNull(lookupEntryStore);
        this.contentMerger = new ContentMerger(MergeStrategy.MERGE, MergeStrategy.KEEP, MergeStrategy.MERGE);
        final String scheduleEventAliasNamespace = extractor.getScheduleEventAliasNamespace(); 
        this.scheduleEventAliasPredicate = input -> input.getNamespace().equals(scheduleEventAliasNamespace);
    }
    
    @Override
    public ItemRefAndBroadcast process(Channel channel, Publisher targetPublisher, Element element) {
        Item item = extractor.extract(channel, targetPublisher, element);
        removeStaleScheduleEventOnOldItems(item);
        Optional<Identified> existing = resolve(item.getCanonicalUri());
        if (existing.isPresent()) {
            Identified identified = existing.get();
            write(contentMerger.merge(ContentMerger.asItem(identified), item));
        } else {
            write(item);
        }
        return new ItemRefAndBroadcast(item, getBroadcastFromItem(item));
    }

    private Broadcast getBroadcastFromItem(Item item) {
        Version version = Iterables.getOnlyElement(item.getVersions());
        return Iterables.getOnlyElement(version.getBroadcasts());
    }

    private Optional<Identified> resolve(String uri) {
        ImmutableSet<String> uris = ImmutableSet.of(uri);
        return resolver.findByCanonicalUris(uris).get(uri).toGuavaOptional();
    }

    private void write(Content content) {
        writer.createOrUpdate((Item) content);
    }
    
    private void removeStaleScheduleEventOnOldItems(Item item) {
        final String scheduleEventAliasNamespace = extractor.getScheduleEventAliasNamespace();
        Optional<Alias> scheduleEventAlias = Iterables.tryFind(item.getAliases(), scheduleEventAliasPredicate);
        Iterable<LookupEntry> entries = lookupEntryStore.entriesForAliases(Optional.of(scheduleEventAliasNamespace), 
                ImmutableSet.of(scheduleEventAlias.get().getValue()));
        for (LookupEntry entry : entries) {
            if (!entry.uri().equals(item.getCanonicalUri())) {
                Identified ided = resolver.findByCanonicalUris(ImmutableSet.of(entry.uri())).getFirstValue().requireValue();
                ided.setAliases(Iterables.filter(ided.getAliases(), Predicates.not(scheduleEventAliasPredicate)));
                writer.createOrUpdate((Item)ided);
            }
        }
    }
}
