package org.atlasapi.remotesite.thesun;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import nu.xom.Nodes;

import org.atlasapi.media.entity.ChildRef;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.metabroadcast.common.base.Maybe;


public class TheSunTvPicksEntryProcessor {
    private final ContentWriter contentWriter;
    private final ContentResolver contentStore;
    private static String URI_PREFIX = "http://thesun.co.uk/guid/";
    
    public TheSunTvPicksEntryProcessor(ContentWriter contentWriter, ContentResolver contentStore) {
        super();
        this.contentWriter = contentWriter;
        this.contentStore = contentStore;
    }

    public Collection<Item> convertToItems(Nodes entryNodes) {
        Collection<Item> items = Lists.newLinkedList();
        for (int i=0; i<entryNodes.size(); i++) {
            TheSunRssItemElement itemElement = (TheSunRssItemElement) entryNodes.get(i);
            items.add(convertToItem(itemElement));
        }
        return items;
    }
    
    private Item convertToItem(TheSunRssItemElement itemElement) {
        Item item = new Item();
        String itemUri = URI_PREFIX + itemElement.getGuid();
        item.setCanonicalUri(itemUri);
        item.setDescription(itemElement.getDescription());
        item.setLongDescription(itemElement.getStory());
        List<TheSunRssEnclosureElement> enclosures = itemElement.getEnclosures();
        if (!enclosures.isEmpty()) {
            item.setImage(enclosures.get(0).getUrl());
        }
        item.setPublisher(Publisher.THE_SUN);
        // An explicit equivalence equivalent PA item is written into the author field of the feed
        // Look up PA item to check it exists
        String paUri = itemElement.getAuthor();
        Maybe<Identified> resolved = contentStore.findByCanonicalUris(ImmutableSet.of(paUri)).get(paUri);
        if (resolved.hasValue()) {
            item.addAliasUrl(paUri); 
        } else {
            throw new RuntimeException("Could not find equivalent item for: " + itemUri);
        }    
        return item;
    }

    public void createOrUpdate(Collection<Item> items) {
        for (Item item : items) {
            contentWriter.createOrUpdate(item);
        }
    }

    public Set<ChildRef> getChildRefs(Collection<Item> items) {
        Set<ChildRef> childRefs = Sets.newHashSet();
        
        for (Item item : items) {
            // lookup item to get id (so we can add it to the content group)
            Maybe<Identified> resolved = contentStore.findByCanonicalUris(ImmutableSet.of(item.getCanonicalUri())).get(item.getCanonicalUri());
            if (resolved.hasValue()) {
                Item resolvedItem = (Item) resolved.valueOrNull();
                childRefs.add(resolvedItem.childRef());
            } else {
                throw new RuntimeException("Could not find childref for: " + item.getCanonicalUri());
            }   
        }
        return childRefs;
    }
}
