package org.atlasapi.remotesite.pa;

import java.util.List;
import java.util.Map;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.testing.BrandTestDataBuilder;
import org.atlasapi.media.entity.testing.ComplexBroadcastTestDataBuilder;
import org.atlasapi.media.entity.testing.ComplexItemTestDataBuilder;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.persistence.content.people.ItemsPeopleWriter;
import org.atlasapi.remotesite.channel4.pmlsd.epg.ContentHierarchyAndBroadcast;

import com.metabroadcast.common.base.Maybe;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class ContentBufferTest {

    private final ContentWriter contentWriter = mock(ContentWriter.class);
    private final ContentResolver contentResolver = mock(ContentResolver.class);
    private final ItemsPeopleWriter itemsPeopleWriter = mock(ItemsPeopleWriter.class);
    
    private final ContentBuffer contentBuffer = ContentBuffer.create(contentResolver, contentWriter,
            itemsPeopleWriter);

    @Test
    public void testFindsCachedByUris() {
        Brand brand = BrandTestDataBuilder.brand().build();
        brand.setCanonicalUri("http://brand.com");

        List<String> aliasUrls = ImmutableList.of("http://brand.com/item/alias");
        Item item = ComplexItemTestDataBuilder.complexItem().build();
        item.setAliasUrls(aliasUrls);
        item.setCanonicalUri("http://brand.com/item");

        Broadcast broadcast = ComplexBroadcastTestDataBuilder.broadcast().build();
        contentBuffer.add(new ContentHierarchyAndBroadcast(Optional.of(brand),
                Optional.<Series>absent(), item, broadcast));

        Identified queried = Iterables.getOnlyElement(
                contentBuffer.findByUris(ImmutableSet.of("http://brand.com/item/alias"))
                        .getAllResolvedResults());

        assertEquals(item, queried);
    }

    @Test
    public void testFindsNotCachedByUris() {
        Brand brand = BrandTestDataBuilder.brand().build();
        brand.setCanonicalUri("http://brand.com");

        List<String> aliasUrls = ImmutableList.of("http://brand.com/item");
        Item item = ComplexItemTestDataBuilder.complexItem().build();
        item.setAliasUrls(aliasUrls);

        Broadcast broadcast = ComplexBroadcastTestDataBuilder.broadcast().build();

        when(contentResolver.findByUris(anyCollection())).thenReturn(ResolvedContent.builder().put("http://brand.com/item", item).build());

        contentBuffer.add(new ContentHierarchyAndBroadcast(Optional.of(brand),
                Optional.<Series>absent(), new Item(), broadcast));

        ResolvedContent byUris = contentBuffer.findByUris(ImmutableSet.of("http://brand.com/item"));

        assertFalse(byUris.isEmpty());
    }

    @Test
    public void testWriteThroughCache() {
        Brand brand = BrandTestDataBuilder.brand().build();
        brand.setCanonicalUri("http://brand.com");
        
        Item item = ComplexItemTestDataBuilder.complexItem().build();
        item.setCanonicalUri("http://brand.com/item");
        
        Broadcast broadcast = ComplexBroadcastTestDataBuilder.broadcast().build();
        
        contentBuffer.add(new ContentHierarchyAndBroadcast(Optional.of(brand), 
                Optional.<Series>absent(), item, broadcast));
        
        Identified queried = Iterables.getOnlyElement(
                                contentBuffer.findByCanonicalUris(ImmutableSet.of("http://brand.com/item"))
                                             .getAllResolvedResults());
        
        assertEquals(item, queried);
    }
    
    @Test
    public void testFlush() {
        Brand brand = BrandTestDataBuilder.brand().build();
        brand.setCanonicalUri("http://brand.com");
        
        Item item = ComplexItemTestDataBuilder.complexItem().build();
        item.setCanonicalUri("http://brand.com/item");
        
        Broadcast broadcast = ComplexBroadcastTestDataBuilder.broadcast().build();
        
        contentBuffer.add(new ContentHierarchyAndBroadcast(Optional.of(brand), 
                Optional.<Series>absent(), item, broadcast));
        
        contentBuffer.flush();
        
        verify(contentWriter).createOrUpdate(item);
        verify(contentWriter).createOrUpdate(brand);
    }
    
    @Test
    public void testCachePurgedAfterFlush() {
        Brand brand = BrandTestDataBuilder.brand().build();
        brand.setCanonicalUri("http://brand.com");
        
        Item item = ComplexItemTestDataBuilder.complexItem().build();
        item.setCanonicalUri("http://brand.com/item");
        
        Broadcast broadcast = ComplexBroadcastTestDataBuilder.broadcast().build();
        
        contentBuffer.add(new ContentHierarchyAndBroadcast(Optional.of(brand), 
                Optional.<Series>absent(), item, broadcast));
        
        contentBuffer.flush();
        
        Maybe<Identified> resolvedItem = Maybe.just((Identified)ComplexItemTestDataBuilder.complexItem().
                withDescription("This is from the resolver").
                build());
        
        Map<String, Maybe<Identified>> resolved = ImmutableMap.of("http://brand.com/item", resolvedItem);
        when(contentResolver.findByCanonicalUris(ImmutableSet.of("http://brand.com/item")))
            .thenReturn(new ResolvedContent(resolved));
            
        Identified queried = Iterables.getOnlyElement(
                                contentBuffer.findByCanonicalUris(ImmutableSet.of("http://brand.com/item"))
                                             .getAllResolvedResults());
        
        assertEquals(resolvedItem.requireValue(), queried);
    }

}
