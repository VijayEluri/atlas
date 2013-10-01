package org.atlasapi.remotesite.bbc.nitro;

import java.util.List;
import java.util.Map;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.ScheduleEntry.ItemRefAndBroadcast;
import org.atlasapi.persistence.content.schedule.mongo.ScheduleWriter;
import org.atlasapi.remotesite.bbc.ion.BbcIonServices;
import org.atlasapi.remotesite.channel4.epg.BroadcastTrimmer;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.metabroadcast.atlas.glycerin.Glycerin;
import com.metabroadcast.atlas.glycerin.GlycerinException;
import com.metabroadcast.atlas.glycerin.model.Broadcast;
import com.metabroadcast.atlas.glycerin.queries.BroadcastsQuery;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.common.time.DateTimeZones;

/**
 * <p>
 * {@link ChannelDayProcessor} which fetches a processes a day's worth of Nitro
 * {@link Broadcast}s using a {@link Glycerin} and {@link NitroBroadcastHandler}
 * .
 * </p>
 * 
 * <p>
 * Performs the necessary management of the schedule via a
 * {@link ScheduleWriter} and {@link BroadcastTrimmer}.
 * </p>
 */
public class NitroScheduleDayUpdater implements ChannelDayProcessor {
    
    private static final Integer MAX_PAGE_SIZE = 300;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Glycerin glycerin;
    private final NitroBroadcastHandler<? extends List<Optional<ItemRefAndBroadcast>>> broadcastHandler;
    private final BroadcastTrimmer trimmer;
    private final ScheduleWriter scheduleWriter;

    public NitroScheduleDayUpdater(ScheduleWriter scheduleWriter, BroadcastTrimmer trimmer, NitroBroadcastHandler<? extends List<Optional<ItemRefAndBroadcast>>> handler, Glycerin glycerin) {
        this.scheduleWriter = scheduleWriter;
        this.trimmer = trimmer;
        this.broadcastHandler = handler;
        this.glycerin = glycerin;
    }
    
    @Override
    public UpdateProgress process(Channel channel, LocalDate date) throws Exception {
        
        String serviceId = BbcIonServices.services.inverse().get(channel.getUri());
        DateTime from = date.toDateTimeAtStartOfDay(DateTimeZones.UTC);
        DateTime to = from.plusDays(1);
        log.debug("updating {}: {} -> {}", new Object[]{serviceId, from, to});
        
        ImmutableList<Broadcast> broadcasts = getBroadcasts(serviceId, from, to);
        ImmutableList<Optional<ItemRefAndBroadcast>> processingResults = processBroadcasts(broadcasts);
        updateSchedule(channel, from, to, Optional.presentInstances(processingResults));
        
        int processedCount = Iterables.size(Optional.presentInstances(processingResults));
        int failedCount = processingResults.size() - processedCount;

        log.debug("updated {}: {} -> {}: {} broadcasts ({} failed)", new Object[]{serviceId, from, to, processedCount, failedCount});
        return new UpdateProgress(processedCount, failedCount);
    }

    private void updateSchedule(Channel channel, DateTime from, DateTime to, 
            Iterable<ItemRefAndBroadcast> processed) {
        if (Iterables.isEmpty(processed)) {
            return;
        }
        trimmer.trimBroadcasts(new Interval(from, to), channel, acceptableIds(processed));
        scheduleWriter.replaceScheduleBlock(Publisher.BBC_NITRO, channel, processed);
    }

    private ImmutableList<Optional<ItemRefAndBroadcast>> processBroadcasts(ImmutableList<Broadcast> broadcasts) throws NitroException {
        return ImmutableList.copyOf(broadcastHandler.handle(broadcasts));
    }

    
    private Map<String, String> acceptableIds(Iterable<ItemRefAndBroadcast> processed) {
        ImmutableMap.Builder<String, String> ids = ImmutableMap.builder();
        for (ItemRefAndBroadcast itemRefAndBroadcast : processed) {
            ids.put(itemRefAndBroadcast.getBroadcast().getSourceId(),
                    itemRefAndBroadcast.getItemUri());
        }
        return ids.build();
    }

    // TODO: at the moment we're avoiding pagination by gambling there won't
    // be more than 300 broadcasts in one day. In future it may be necessary.
    private ImmutableList<Broadcast> getBroadcasts(String serviceId, DateTime from, DateTime to)
            throws GlycerinException {
        BroadcastsQuery query = BroadcastsQuery.builder()
                .withSid(serviceId)
                .withStartFrom(from)
                .withStartTo(to)
                .withPageSize(MAX_PAGE_SIZE)
                .build();
        return glycerin.execute(query).getResults();
    }

}
