package org.atlasapi.remotesite.bbc;

import static com.metabroadcast.common.scheduling.RepetitionRules.every;
import static com.metabroadcast.common.time.DateTimeZones.UTC;
import static org.atlasapi.http.HttpBackedRemoteSiteClient.httpRemoteSiteClient;
import static org.atlasapi.http.HttpResponseTransformers.gsonResponseTransformer;
import static org.atlasapi.http.HttpResponseTransformers.htmlNavigatorTransformer;
import static org.atlasapi.remotesite.bbc.ion.HttpBackedBbcIonClient.ionClient;
import static org.joda.time.Duration.standardMinutes;

import java.lang.reflect.Field;

import javax.annotation.PostConstruct;

import org.atlasapi.media.segment.SegmentWriter;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.people.ItemsPeopleWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.atlasapi.persistence.topic.TopicStore;
import org.atlasapi.remotesite.HttpClients;
import org.atlasapi.remotesite.bbc.atoz.BbcSlashProgrammesAtoZUpdater;
import org.atlasapi.remotesite.bbc.ion.BbcIonBroadcastHandler;
import org.atlasapi.remotesite.bbc.ion.BbcIonContainerFetcherClient;
import org.atlasapi.remotesite.bbc.ion.BbcIonDayRangeUrlSupplier;
import org.atlasapi.remotesite.bbc.ion.BbcIonEpisodeDetailItemAdapter;
import org.atlasapi.remotesite.bbc.ion.BbcIonEpisodeDetailItemContentExtractor;
import org.atlasapi.remotesite.bbc.ion.BbcIonScheduleController;
import org.atlasapi.remotesite.bbc.ion.BbcIonScheduleUpdater;
import org.atlasapi.remotesite.bbc.ion.BbcIonSegmentAdapter;
import org.atlasapi.remotesite.bbc.ion.DefaultBbcIonBroadcastHandler;
import org.atlasapi.remotesite.bbc.ion.HttpBackedBbcIonClient;
import org.atlasapi.remotesite.bbc.ion.OndemandBbcIonBroadcastHandler;
import org.atlasapi.remotesite.bbc.ion.SegmentUpdatingIonBroadcastHandler;
import org.atlasapi.remotesite.bbc.ion.SocialDataFetchingIonBroadcastHandler;
import org.atlasapi.remotesite.bbc.ion.model.IonContainerFeed;
import org.atlasapi.remotesite.bbc.ion.model.IonEpisodeDetailFeed;
import org.atlasapi.remotesite.bbc.ion.model.IonOndemandChanges;
import org.atlasapi.remotesite.bbc.ion.model.IonSchedule;
import org.atlasapi.remotesite.bbc.ion.model.IonSegmentEventFeed;
import org.atlasapi.remotesite.bbc.ion.ondemand.BbcIonOndemandChangeTaskBuilder;
import org.atlasapi.remotesite.bbc.ion.ondemand.BbcIonOndemandChangeUpdateBuilder;
import org.atlasapi.remotesite.bbc.ion.ondemand.BbcIonOndemandChangeUpdateController;
import org.atlasapi.remotesite.bbc.ion.ondemand.BbcIonOndemandChangeUpdater;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.time.DayRangeGenerator;

@Configuration
public class BbcModule {

	private final static RepetitionRule BRAND_UPDATE_TIME = RepetitionRules.NEVER;
	private final static RepetitionRule TEN_MINUTES = RepetitionRules.every(Duration.standardMinutes(10));
	private final static RepetitionRule ONE_HOUR = RepetitionRules.every(Duration.standardHours(1));
	
	public final static String SCHEDULE_ONDEMAND_FORMAT = "http://www.bbc.co.uk/iplayer/ion/schedule/service/%s/date/%s/media_set/pc/format/json";
	public final static String SCHEDULE_DEFAULT_FORMAT = "http://www.bbc.co.uk/iplayer/ion/schedule/service/%s/date/%s/timeslot/day/format/json";

    private @Autowired ContentResolver contentResolver;
	private @Autowired ContentWriter contentWriters;
	private @Autowired TopicStore topicStore;
	private @Autowired AdapterLog log;
	private @Autowired SimpleScheduler scheduler;
	private @Autowired ItemsPeopleWriter itemsPeopleWriter;
	private @Autowired DatabasedMongo mongo;
	private @Autowired SegmentWriter segmentWriter;
	
    @PostConstruct
    public void scheduleTasks() {
        scheduler.schedule(bbcFeedsUpdater(), BRAND_UPDATE_TIME);
        
        scheduler.schedule(bbcIonScheduleUpdater(0, 0).withName("BBC Ion schedule update (today only)"), TEN_MINUTES);
        scheduler.schedule(bbcIonScheduleUpdater(7, 7).withName("BBC Ion schedule update (14 days)"), ONE_HOUR);
        scheduler.schedule(bbcIonScheduleOndemandUpdater(7).withName("BBC Ion on-demand schedule update (7 days)"), every(standardMinutes(10)).withOffset(standardMinutes(5)));
        scheduler.schedule(bbcIonSocialDataUpdater(7, 7).withName("BBC Social data updater (±7 days)"), RepetitionRules.daily(new LocalTime(8, 0, 0)));
        scheduler.schedule(bbcIonSocialDataUpdater(0, 180).withName("BBC Social data updater (1/2 year)"), RepetitionRules.NEVER);
        //scheduler.schedule(bbcIonSegmentUpdater().withName("BBC Segment Updater"), TEN_MINUTES);
        
        scheduler.schedule(bbcIonOndemandChangeUpdater().withName("BBC Ion Ondemand Change Updater"), TEN_MINUTES);
        log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass()).withDescription("BBC update scheduled tasks installed"));
    }
	
    private BbcIonScheduleUpdater bbcIonSegmentUpdater() {
        BbcIonDayRangeUrlSupplier urlSupplier = dayRangeUrlSupplier(SCHEDULE_DEFAULT_FORMAT, 0, 2);
        final BbcIonSegmentAdapter segmentAdapter = new BbcIonSegmentAdapter(ionClient(HttpClients.webserviceClient(), IonSegmentEventFeed.class), segmentWriter);
        BbcIonBroadcastHandler broadcastHandler = new SegmentUpdatingIonBroadcastHandler(contentResolver, contentWriters, segmentAdapter);
        return new BbcIonScheduleUpdater(urlSupplier, bbcIonScheduleClient(), broadcastHandler, log);
    }

    private BbcIonScheduleUpdater bbcIonScheduleUpdater(int lookBack, int lookAhead) {
        BbcIonDayRangeUrlSupplier urlSupplier = dayRangeUrlSupplier(SCHEDULE_DEFAULT_FORMAT, lookAhead, lookBack);
        return new BbcIonScheduleUpdater(urlSupplier, bbcIonScheduleClient(), defaultBbcIonBroadcastHandler(), log);
    }
    
    private BbcIonScheduleUpdater bbcIonScheduleOndemandUpdater(int lookBack) {
        BbcIonDayRangeUrlSupplier urlSupplier = dayRangeUrlSupplier(SCHEDULE_ONDEMAND_FORMAT, 0, lookBack);
        return new BbcIonScheduleUpdater(urlSupplier, bbcIonScheduleClient(), new OndemandBbcIonBroadcastHandler(contentResolver, contentWriters, log), log);
    }
    
    private BbcIonScheduleUpdater bbcIonSocialDataUpdater(int ahead, int back) {
        BbcIonDayRangeUrlSupplier urlSupplier = dayRangeUrlSupplier(SCHEDULE_DEFAULT_FORMAT, ahead, back);
        SimpleHttpClient httpClient = HttpClients.webserviceClient();
        
        RemoteSiteClient<SlashProgrammesContainer> jsonClient = jsonClient(httpClient);
        
        BbcRelatedLinksAdapter linksAdapter = new BbcRelatedLinksAdapter(jsonClient);
        BbcHashTagAdapter hashTagAdapter = new BbcHashTagAdapter(httpRemoteSiteClient(httpClient, htmlNavigatorTransformer()));
        BbcSlashProgrammesJsonTopicsAdapter topicsAdapter = new BbcSlashProgrammesJsonTopicsAdapter(jsonClient, topicStore, log);
        
        BbcIonBroadcastHandler handler = new SocialDataFetchingIonBroadcastHandler(linksAdapter, hashTagAdapter, topicsAdapter, contentResolver, contentWriters, log);
        return new BbcIonScheduleUpdater(urlSupplier, bbcIonScheduleClient(), handler, log);
    }

    public RemoteSiteClient<SlashProgrammesContainer> jsonClient(SimpleHttpClient httpClient) {
        return httpRemoteSiteClient(httpClient, gsonResponseTransformer(new GsonBuilder().setFieldNamingStrategy(new FieldNamingStrategy() {

            @Override
            public String translateName(Field f) {
                return f.getName();
            }
        }), SlashProgrammesContainer.class));
    }
	
	private BbcIonDayRangeUrlSupplier dayRangeUrlSupplier(String urlPattern, int ahead, int back) {
	    return new BbcIonDayRangeUrlSupplier(urlPattern, new DayRangeGenerator(UTC).withLookAhead(ahead).withLookBack(back));
	}
	
	@Bean BbcIonScheduleController bbcIonScheduleController() {
        return new BbcIonScheduleController(bbcIonScheduleClient(), defaultBbcIonBroadcastHandler(), log);
    }

    @Bean HttpBackedBbcIonClient<IonSchedule> bbcIonScheduleClient() {
        return ionClient(HttpClients.webserviceClient(), new TypeToken<IonSchedule>(){});
    }
	
    @Bean DefaultBbcIonBroadcastHandler defaultBbcIonBroadcastHandler() {
        return new DefaultBbcIonBroadcastHandler(contentResolver, contentWriters, log)
            .withItemFetcherClient(bbcIonEpisodeDetailItemAdapter())
            .withContainerFetcherClient(new BbcIonContainerFetcherClient(log))
            .withItemPeopleWriter(itemsPeopleWriter);
    }
    
    private BbcIonEpisodeDetailItemAdapter bbcIonEpisodeDetailItemAdapter() {
        return new BbcIonEpisodeDetailItemAdapter(
                ionClient(HttpClients.webserviceClient(), IonEpisodeDetailFeed.class), 
                new BbcIonEpisodeDetailItemContentExtractor(log, ionClient(HttpClients.webserviceClient(), IonContainerFeed.class)));
    }

    @Bean Runnable bbcFeedsUpdater() {
		return new BbcSlashProgrammesAtoZUpdater(contentWriters,  new ProgressStore(mongo), topicStore, log);
	}
	
	@Bean BbcSlashProgrammesController bbcFeedsController() {
	    return new BbcSlashProgrammesController(contentWriters, topicStore, log);
	}
	
	@Bean BbcIonOndemandChangeUpdater bbcIonOndemandChangeUpdater() {
	    return new BbcIonOndemandChangeUpdater(bbcIonOndemandChangeUpdateBuilder(), log);
	}

    private BbcIonOndemandChangeUpdateBuilder bbcIonOndemandChangeUpdateBuilder() {
        return new BbcIonOndemandChangeUpdateBuilder(new BbcIonOndemandChangeTaskBuilder(contentResolver, contentWriters, log), log, ionClient(HttpClients.webserviceClient(),IonOndemandChanges.class));
    }
	
	@Bean BbcIonOndemandChangeUpdateController bbcIonOndemandChangeController() {
	    return new BbcIonOndemandChangeUpdateController(bbcIonOndemandChangeUpdateBuilder());
	}
}
