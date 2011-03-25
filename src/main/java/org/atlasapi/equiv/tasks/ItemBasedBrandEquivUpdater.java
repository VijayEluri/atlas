package org.atlasapi.equiv.tasks;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

public class ItemBasedBrandEquivUpdater {

    private final ContentWriter contentWriter;

    private double certaintyThreshold = 0.9;
    private boolean writesResults = true;

    private final SymmetricItemVersionMerger versionMerger = new SymmetricItemVersionMerger();

    private final ItemEquivGenerator itemEquivGenerator;
    private final Set<Publisher> publishers;

    private final EquivCleaner equivCleaner;

    public ItemBasedBrandEquivUpdater(ItemEquivGenerator itemEquivGenerator, ContentResolver contentResolver, ContentWriter contentWriter) {
        this.contentWriter = contentWriter;
        this.itemEquivGenerator = itemEquivGenerator;
        this.publishers = this.itemEquivGenerator.supportedPublishers();
        this.equivCleaner = new EquivCleaner(contentResolver);
    }

    public ContainerEquivResult<Container<?>, Item> updateEquivalence(Brand brand) {

        Set<Identified> oldEquivalences = Sets.newHashSet();

        // All the items that are *strongly* equivalent to items in this brand.
        ListMultimap<Publisher, Item> binnedStrongSuggestions = ArrayListMultimap.create();

        List<EquivResult<Item>> itemEquivResults = Lists.newArrayListWithCapacity(brand.getContents().size());
        for (Episode episode : brand.getContents()) {

            SuggestedEquivalents<Item> itemSuggestions = itemEquivGenerator.equivalentsFor(episode);

            Map<Publisher, Item> strongSuggestions = itemSuggestions.strongSuggestions(certaintyThreshold);

            oldEquivalences.addAll(equivCleaner.cleanEquivalences(episode, strongSuggestions.values(), publishers));

            // version merging of strong suggestions
            versionMerger.mergeVersions(episode, strongSuggestions.values());

            // record for computing equivalent brands
            binnedStrongSuggestions.putAll(Multimaps.forMap(strongSuggestions));

            itemEquivResults.add(EquivResult.of(episode, itemSuggestions, certaintyThreshold));
        }

        // The containers of the strongly suggested items for all items in this brand.
        SuggestedEquivalents<Container<?>> brandSuggestedEquivalents = SuggestedEquivalents.from(containersFrom(binnedStrongSuggestions));

        Map<Publisher, Container<?>> strongSuggestions = brandSuggestedEquivalents.strongSuggestions(certaintyThreshold);

        oldEquivalences.addAll(equivCleaner.cleanEquivalences(brand, strongSuggestions.values(), publishers));

        // create equivalence relation for strongly equivalent suggestions
        for (Identified equiv : strongSuggestions.values()) {
            brand.addEquivalentTo(equiv);
            equiv.addEquivalentTo(brand);
        }

        // Write: the subject brand, any old equivalents, all TLEs of all
        // strongly suggested equivalent items (their versions will have been merged)
        if (writesResults) {
            contentWriter.createOrUpdate(brand, false);
            for (Identified equiv : ImmutableSet.copyOf(Iterables.concat(topLevelElements(binnedStrongSuggestions.values()), oldEquivalences))) {
                if (equiv instanceof Item) {
                    contentWriter.createOrUpdate((Item) equiv);
                }
                if (equiv instanceof Container<?>) {
                    contentWriter.createOrUpdate((Container<?>) equiv, false);
                }
            }
        }

        return ContainerEquivResult.of(brand, brandSuggestedEquivalents, certaintyThreshold).withItemResults(itemEquivResults);
    }

    @SuppressWarnings("unchecked")
    private Set<Content> topLevelElements(Collection<Item> suggestedItems) {
        List<Content> tles = Lists.newArrayListWithExpectedSize(suggestedItems.size());
        for (Item item : suggestedItems) {
            if (item.getFullContainer() == null) {
                tles.add(item);
            } else {
                Container<?> container = item.getFullContainer();
                int index = tles.indexOf(container);
                if(index < 0) {
                    tles.add(container);
                } else {
                    ((Container<Item>)tles.get(index)).addContents(item);
                }
            }
        }
        return ImmutableSet.copyOf(tles);
    }

    private Multimap<Publisher, Container<?>> containersFrom(ListMultimap<Publisher, Item> binnedSuggestedEquivalents) {
        return Multimaps.transformValues(binnedSuggestedEquivalents, new Function<Item, Container<?>>() {

            @Override
            public Container<?> apply(Item input) {
                return input.getFullContainer();
            }
        });
    }

    public ItemBasedBrandEquivUpdater withCertaintyThreshold(double threshold) {
        this.certaintyThreshold = threshold;
        return this;
    }

    public ItemBasedBrandEquivUpdater writesResults(boolean shouldWriteResults) {
        this.writesResults = shouldWriteResults;
        return this;
    }
}
