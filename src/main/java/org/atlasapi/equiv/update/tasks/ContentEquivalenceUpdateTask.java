
package org.atlasapi.equiv.update.tasks;


import static org.atlasapi.persistence.content.ContentCategory.CONTAINER;
import static org.atlasapi.persistence.content.ContentCategory.TOP_LEVEL_ITEM;
import static org.atlasapi.persistence.content.listing.ContentListingCriteria.defaultCriteria;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.atlasapi.equiv.update.ContentEquivalenceUpdater;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.listing.ContentListingProgress;
import org.atlasapi.persistence.logging.AdapterLog;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class ContentEquivalenceUpdateTask extends AbstractContentEquivalenceUpdateTask<Content> {

    private final ContentLister contentStore;
    
    private String schedulingKey = "equivalence";
    private List<Publisher> publishers;
    private Set<String> ignored;

    private final ContentEquivalenceUpdater<Content> rootUpdater;

    
    public ContentEquivalenceUpdateTask(ContentLister contentStore, ContentEquivalenceUpdater<Content> rootUpdater, AdapterLog log, ScheduleTaskProgressStore progressStore, Set<String> ignored) {
        super(log, progressStore);
        this.contentStore = contentStore;
        this.rootUpdater = rootUpdater;
        this.ignored = ignored;
    }
    
    @Override
    protected Iterator<Content> getContentIterator(ContentListingProgress progress) {
        return contentStore.listContent(defaultCriteria().forPublishers(publishers).forContent(CONTAINER, TOP_LEVEL_ITEM).startingAt(progress).build());
    }

    public ContentEquivalenceUpdateTask forPublishers(Publisher... publishers) {
        this.publishers = ImmutableList.copyOf(publishers);
        this.schedulingKey = Joiner.on("-").join(Iterables.transform(this.publishers, Publisher.TO_KEY))+"-equivalence";
        return this;
    }

    @Override
    protected String schedulingKey() {
        return schedulingKey;
    }

    @Override
    protected void handle(Content content) {
        if (!ignored.contains(content.getCanonicalUri())) {
            rootUpdater.updateEquivalences(content, Optional.<List<Content>>absent());
        }
    }

}
