package org.atlasapi.remotesite.channel4.pmlsd;

import java.io.File;
import java.net.URL;

import javax.annotation.PostConstruct;

import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.remotesite.HttpClients;
import org.atlasapi.remotesite.channel4.epg.ScheduleResolverBroadcastTrimmer;
import org.atlasapi.remotesite.channel4.pmlsd.epg.C4EpgChannelDayUpdater;
import org.atlasapi.remotesite.channel4.pmlsd.epg.C4EpgClient;
import org.atlasapi.remotesite.channel4.pmlsd.epg.C4EpgUpdater;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.time.DayRangeGenerator;

@Configuration
public class C4PmlsdModule {
    
    static final Publisher SOURCE = Publisher.C4_PMLSD;
    
    private static final SourceSpecificContentFactory contentFactory =
        new SourceSpecificContentFactory(SOURCE);
    
    public static final SourceSpecificContentFactory contentFactory() {
        return contentFactory;
    }
    
    private static final Logger log = LoggerFactory.getLogger(C4PmlsdModule.class);

    private static final String ATOZ_BASE = "https://pmlsc.channel4.com/pmlsd/";
    private static final String P06_PLATFORM = "p06";

	private final static RepetitionRule BRAND_UPDATE_TIME = RepetitionRules.daily(new LocalTime(2, 0, 0));
	private final static RepetitionRule XBOX_UPDATE_TIME = RepetitionRules.daily(new LocalTime(1, 0, 0));
	private final static RepetitionRule TWO_HOURS = RepetitionRules.every(Duration.standardHours(2));
	private final static RepetitionRule TWO_HOURS_WITH_OFFSET = RepetitionRules.every(Duration.standardHours(2)).withOffset(Duration.standardHours(1));

	private @Autowired SimpleScheduler scheduler;

	private @Autowired @Qualifier("contentResolver") ContentResolver contentResolver;
	private @Autowired @Qualifier("contentWriter") ContentWriter contentWriter;
	private @Autowired ScheduleResolver scheduleResolver;
	private @Autowired ChannelResolver channelResolver;
	
	private @Value("${updaters.c4pmlsd.enabled}") Boolean tasksEnabled;
	private @Value("${c4.keystore.path}") String keyStorePath;
	private @Value("${c4.keystore.password}") String keyStorePass;
	
    @PostConstruct
    public void startBackgroundTasks() {
        if (tasksEnabled) {
            scheduler.schedule(pcC4PmlsdEpgUpdater().withName("C4 PMLSD Epg PC Updater (15 day)"), TWO_HOURS);
            scheduler.schedule(pcC4PmlsdAtozUpdater().withName("C4 PMLSD 4OD PC Updater"), BRAND_UPDATE_TIME);
            scheduler.schedule(xbox4PmlsdEpgUpdater().withName("C4 PMLSC Epg XBox Updater (15 day)"), TWO_HOURS_WITH_OFFSET);
            scheduler.schedule(xboxC4PmlsdAtozUpdater().withName("C4 PMLSD 4OD XBox Updater"), XBOX_UPDATE_TIME);
            log.info("C4 update scheduled tasks installed");
        }
    }
    
    @Bean protected SimpleHttpClient c4HttpsClient() {
        if (Strings.isNullOrEmpty(keyStorePass) || Strings.isNullOrEmpty(keyStorePath)) {
            if (tasksEnabled) {
                throw new RuntimeException("Check c4.keystore.path or c4.keystore.password");
            } else {
                return UnconfiguredSimpleHttpClient.get();
            }
        }
        try {
            URL jksFile = new File(keyStorePath).toURI().toURL();
            return HttpClients.httpsClient(jksFile, keyStorePass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean C4AtomApi atomPmlsdApi() {
        return new C4AtomApi(channelResolver);
    }
    
	@Bean public C4EpgUpdater pcC4PmlsdEpgUpdater() {
	    return new C4EpgUpdater(atomPmlsdApi(), pcC4PlmsdEpgChannelDayUpdater(), new DayRangeGenerator().withLookAhead(7).withLookBack(7));
    }
	
	@Bean public C4EpgChannelDayUpdater pcC4PlmsdEpgChannelDayUpdater() {
	    ScheduleResolverBroadcastTrimmer trimmer = new ScheduleResolverBroadcastTrimmer(SOURCE, scheduleResolver, contentResolver, pmlsdLastUpdatedSettingContentWriter());
	    return new C4EpgChannelDayUpdater(new C4EpgClient(c4HttpsClient()), pmlsdLastUpdatedSettingContentWriter(),
                contentResolver, c4PmlsdBrandFetcher(Optional.<Platform>absent(),Optional.<String>absent()), trimmer, 
                Optional.<String>absent());
	}
	
	@Bean public C4EpgUpdater xbox4PmlsdEpgUpdater() {
        return new C4EpgUpdater(atomPmlsdApi(), xboxC4PlmsdEpgChannelDayUpdater(), new DayRangeGenerator().withLookAhead(7).withLookBack(21));
    }
    
    @Bean public C4EpgChannelDayUpdater xboxC4PlmsdEpgChannelDayUpdater() {
        ScheduleResolverBroadcastTrimmer trimmer = new ScheduleResolverBroadcastTrimmer(SOURCE, scheduleResolver, contentResolver, pmlsdLastUpdatedSettingContentWriter());
        return new C4EpgChannelDayUpdater(new C4EpgClient(c4HttpsClient()), pmlsdLastUpdatedSettingContentWriter(),
                contentResolver, c4PmlsdBrandFetcher(Optional.of(Platform.XBOX),Optional.of(P06_PLATFORM)), trimmer, 
                Optional.of(P06_PLATFORM));
    }
    
	@Bean protected C4AtoZAtomContentUpdateTask pcC4PmlsdAtozUpdater() {
		return new C4AtoZAtomContentUpdateTask(c4HttpsClient(), ATOZ_BASE, c4PmlsdBrandFetcher(Optional.<Platform>absent(),Optional.<String>absent()));
	}
	
	@Bean protected C4AtoZAtomContentUpdateTask xboxC4PmlsdAtozUpdater() {
	    return new C4AtoZAtomContentUpdateTask(c4HttpsClient(), ATOZ_BASE, Optional.of(P06_PLATFORM), c4PmlsdBrandFetcher(Optional.of(Platform.XBOX),Optional.of(P06_PLATFORM)));
	}
	
	protected C4AtomBackedBrandUpdater c4PmlsdBrandFetcher(Optional<Platform> platform, Optional<String> platformParam) {
	    C4AtomApiClient client = new C4AtomApiClient(c4HttpsClient(), ATOZ_BASE, platformParam);
	    C4BrandExtractor extractor = new C4BrandExtractor(client, platform, channelResolver);
		return new C4AtomBackedBrandUpdater(client, platform, contentResolver, pmlsdLastUpdatedSettingContentWriter(), extractor);
	}
	
	@Bean protected C4BrandUpdateController c4BrandUpdateController() {
	    return new C4BrandUpdateController(c4PmlsdBrandFetcher(Optional.<Platform>absent(),Optional.<String>absent()), 
	            ImmutableMap.of(Platform.XBOX, c4PmlsdBrandFetcher(Optional.of(Platform.XBOX),Optional.of(P06_PLATFORM))));
	}
	
	@Bean protected C4EpgChannelDayUpdateController c4EpgChannelDayUpdateController() {
	    return new C4EpgChannelDayUpdateController(atomPmlsdApi(), pcC4PlmsdEpgChannelDayUpdater());
	}
	
    @Bean protected LastUpdatedSettingContentWriter pmlsdLastUpdatedSettingContentWriter() {
        return new LastUpdatedSettingContentWriter(contentResolver, new LastUpdatedCheckingContentWriter(contentWriter));
    }
    
}
