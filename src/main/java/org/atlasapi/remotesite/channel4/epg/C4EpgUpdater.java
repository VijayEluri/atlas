package org.atlasapi.remotesite.channel4.epg;

import java.util.Map;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.atlasapi.remotesite.channel4.C4AtomApi;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.google.common.collect.BiMap;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRange;
import com.metabroadcast.common.time.DayRangeGenerator;

public class C4EpgUpdater extends ScheduledTask {

    private final C4AtomApi c4AtomApi;
    private final DayRangeGenerator rangeGenerator;
    private final AdapterLog log;

    private C4EpgChannelDayUpdater channelDayUpdater;

    public C4EpgUpdater(C4AtomApi atomApi, C4EpgChannelDayUpdater updater, AdapterLog log, DayRangeGenerator generator) {
        this.c4AtomApi = atomApi;
        this.log = log;
        this.channelDayUpdater = updater;
        this.rangeGenerator = generator;
    }

    @Override
    protected void runTask() {
        DateTime start = new DateTime(DateTimeZones.UTC);
        log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass()).withDescription("C4 EPG Update initiated"));
        
        DayRange dayRange = rangeGenerator.generate(new LocalDate(DateTimeZones.UTC));
        
        BiMap<String, Channel> channelMap = c4AtomApi.getChannelMap();
		int total = Iterables.size(dayRange) * channelMap.size();
        int processed = 0;
        UpdateProgress progress = UpdateProgress.START;
        
        for (Map.Entry<String, Channel> channelEntry : channelMap.entrySet()) {
            for (LocalDate scheduleDay : dayRange) {
                reportStatus(progressReport("Processing", processed++, total, progress));
                progress = progress.reduce(channelDayUpdater.update(channelEntry.getKey(), channelEntry.getValue(), scheduleDay));
            }
        }
        
        reportStatus(progressReport("Processed", processed++, total, progress));
        String runTime = new Period(start, new DateTime(DateTimeZones.UTC)).toString(PeriodFormat.getDefault());
        log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass()).withDescription("C4 EPG Update finished in " + runTime));
        
        if (progress.hasFailures()) {
            throw new IllegalStateException(String.format("Completed with %s failures", progress.getFailures()));
        }
    }

    private String progressReport(String prefix, int processed, int total, UpdateProgress progress) {
        return String.format("%s %s/%s. %s failures. %s broadcasts processed", prefix, processed, total, progress.getFailures(), progress.getProcessed());
    }

}
