package org.atlasapi.output;

import com.metabroadcast.applications.client.model.internal.Application;
import org.atlasapi.media.entity.Event;
import org.atlasapi.media.entity.simple.EventQueryResult;
import org.atlasapi.output.simple.ModelSimplifier;
import org.atlasapi.persistence.content.ContentResolver;

import java.util.Set;

public class SimpleEventModelWriter extends TransformingModelWriter<Iterable<Event>, EventQueryResult> {

    private final ModelSimplifier<Event, org.atlasapi.media.entity.simple.Event> eventSimplifier;

    public SimpleEventModelWriter(
            AtlasModelWriter<EventQueryResult> delegate,
            ContentResolver contentResolver,
            ModelSimplifier<Event, org.atlasapi.media.entity.simple.Event> eventSimplifier
    ) {
        super(delegate);
        this.eventSimplifier = eventSimplifier;
    }
    
    @Override
    protected EventQueryResult transform(
            Iterable<Event> fullEvents,
            Set<Annotation> annotations,
            Application application
    ) {
        EventQueryResult result = new EventQueryResult();
        for (Event fullEvent : fullEvents) {
            result.add(eventSimplifier.simplify(fullEvent, annotations, application));
        }
        return result;
    }

}
