package org.atlasapi.output;

import com.google.common.collect.ImmutableList;
import com.metabroadcast.applications.client.model.internal.Application;
import org.atlasapi.media.entity.Schedule.ScheduleChannel;
import org.atlasapi.media.entity.simple.ScheduleQueryResult;
import org.atlasapi.output.simple.ChannelSimplifier;
import org.atlasapi.output.simple.ItemModelSimplifier;

import java.util.Set;

/**
 * {@link AtlasModelWriter} that translates the full URIplay object model into a simplified form and
 * renders that as XML.
 *
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class SimpleScheduleModelWriter
        extends TransformingModelWriter<Iterable<ScheduleChannel>, ScheduleQueryResult> {

    private final ItemModelSimplifier itemModelSimplifier;
    private final ChannelSimplifier channelSimplifier;

    public SimpleScheduleModelWriter(
            AtlasModelWriter<ScheduleQueryResult> outputter,
            ItemModelSimplifier itemModelSimplifier,
            ChannelSimplifier channelSimplifier
    ) {
        super(outputter);
        this.itemModelSimplifier = itemModelSimplifier;
        this.channelSimplifier = channelSimplifier;
    }

    @Override
    protected ScheduleQueryResult transform(
            Iterable<ScheduleChannel> fullGraph,
            Set<Annotation> annotations,
            Application application
    ) {
        ScheduleQueryResult outputGraph = new ScheduleQueryResult();
        for (ScheduleChannel scheduleChannel : fullGraph) {
            outputGraph.add(scheduleChannelFrom(scheduleChannel, annotations, application));
        }
        return outputGraph;
    }

    private org.atlasapi.media.entity.simple.ScheduleChannel scheduleChannelFrom(
            ScheduleChannel scheduleChannel,
            Set<Annotation> annotations,
            Application application
    ) {
        org.atlasapi.media.entity.simple.ScheduleChannel newScheduleChannel
                = new org.atlasapi.media.entity.simple.ScheduleChannel();
        newScheduleChannel.setChannelUri(scheduleChannel.channel().getUri());
        newScheduleChannel.setChannelKey(scheduleChannel.channel().getKey());
        newScheduleChannel.setChannelTitle(scheduleChannel.channel().getTitle());

        if (annotations.contains(Annotation.CHANNEL)) {
            newScheduleChannel.setChannel(channelSimplifier.simplify(
                    scheduleChannel.channel(),
                    false,
                    false,
                    false,
                    false,
                    application
            ));
        }

        ImmutableList.Builder<org.atlasapi.media.entity.simple.Item> items
                = ImmutableList.builder();
        for (org.atlasapi.media.entity.Item item : scheduleChannel.items()) {
            items.add(itemModelSimplifier.simplify(item, annotations, application));
        }

        newScheduleChannel.setItems(items.build());
        return newScheduleChannel;
    }

}
