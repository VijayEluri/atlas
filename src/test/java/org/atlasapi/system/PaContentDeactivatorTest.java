package org.atlasapi.system;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import org.atlasapi.media.entity.*;
import org.atlasapi.persistence.content.ContentCategory;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.listing.ContentListingCriteria;
import org.atlasapi.persistence.content.listing.ContentListingProgress;
import org.atlasapi.persistence.content.listing.ProgressStore;
import org.atlasapi.persistence.lookup.entry.LookupEntry;
import org.atlasapi.persistence.lookup.entry.LookupEntryStore;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PaContentDeactivatorTest {

    private PaContentDeactivator deactivator;
    private LookupEntryStore lookup;
    private ContentLister lister;
    private ContentWriter writer;
    private ProgressStore progressStore;
    private Brand activeContent;
    private Brand inactiveContent;
    private Brand emptyContainer;

    @Before
    public void setUp() throws Exception {
        lookup = mock(LookupEntryStore.class);
        lister = mock(ContentLister.class);
        writer = mock(ContentWriter.class);
        progressStore = mock(ProgressStore.class);
        deactivator = new PaContentDeactivator(lookup, lister, writer, progressStore);
        activeContent = new Brand("10", "10", Publisher.PA);
        activeContent.setChildRefs(
                ImmutableList.of(
                        new ChildRef(10l, "", "", DateTime.now(), EntityType.CLIP)
                )
        );
        activeContent.setId(10l);

        inactiveContent = new Brand("20", "20", Publisher.PA);
        inactiveContent.setId(20l);
        inactiveContent.setGenericDescription(false);

        emptyContainer = new Brand("30", "30", Publisher.PA);
        emptyContainer.setId(30l);
        setupMocks(activeContent, inactiveContent, emptyContainer);
    }

    @Test
    public void testDeactivatesContentCorrectly() throws Exception {
        SetMultimap<String, String> typesToIds = MultimapBuilder.SetMultimapBuilder
                .hashKeys()
                .hashSetValues()
                .build();

        typesToIds.put("pa:brand", "10");
        deactivator.deactivate(typesToIds, false);
        Thread.sleep(2000);
        assertThat(activeContent.isActivelyPublished(), is(true));
        assertThat(inactiveContent.isActivelyPublished(), is(false));
        assertThat(emptyContainer.isActivelyPublished(), is(false));
    }

    private void setupMocks(Content activeContent, Content inactiveContent, Brand emptyContainer) {
        LookupEntry activeLookup = mock(LookupEntry.class);
        when(activeLookup.id()).thenReturn(10l);

        when(lookup.entriesForAliases(any(Optional.class), anyListOf(String.class)))
                .thenReturn(ImmutableList.of(activeLookup));

        when(progressStore.progressForTask(PaContentDeactivator.class.getSimpleName()+"containers"))
                .thenReturn(Optional.<ContentListingProgress>absent());
        when(progressStore.progressForTask(PaContentDeactivator.class.getSimpleName()+"children"))
                .thenReturn(Optional.<ContentListingProgress>absent());

        ImmutableList<ContentCategory> contentCategories = ImmutableList.of(
                ContentCategory.CONTAINER,
                ContentCategory.CHILD_ITEM,
                ContentCategory.TOP_LEVEL_ITEM
        );

        Iterator<Content> contentIterator = ImmutableList.of(activeContent, inactiveContent, emptyContainer).iterator();

        ContentListingCriteria criteria = ContentListingCriteria.defaultCriteria()
                .forContent(contentCategories)
                .forPublishers(Publisher.PA)
                .build();

        when(lister.listContent(criteria)).thenReturn(contentIterator);
    }
}