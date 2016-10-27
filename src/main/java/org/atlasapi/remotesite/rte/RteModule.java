package org.atlasapi.remotesite.rte;

import javax.annotation.PostConstruct;

import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.remotesite.ContentMerger;
import org.atlasapi.remotesite.ContentMerger.MergeStrategy;

import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;

import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("PublicConstructor")
@Configuration
public class RteModule {

    private final static RepetitionRule INGEST_FREQUENCY =
            RepetitionRules.every(Duration.standardHours(3));
    
    @Value("${rte.feed.url}") private String feedUrl;

    @Autowired private SimpleScheduler scheduler;
    @Autowired private ContentWriter contentWriter;
    @Autowired private ContentResolver contentResolver;
    @Autowired private ContentLister contentLister;

    @SuppressWarnings("WeakerAccess")
    @Bean
    public RteFeedUpdater feedUpdater() {
        return new RteFeedUpdater(feedSupplier(), feedProcessor());
    }
    
    @SuppressWarnings("WeakerAccess")
    @Bean
    public RteHttpFeedSupplier feedSupplier() {
        return new RteHttpFeedSupplier(feedUrl);
    }
    
    @SuppressWarnings("WeakerAccess")
    @Bean
    public RteFeedProcessor feedProcessor() {
        return new RteFeedProcessor(contentWriter, 
                                    contentResolver, 
                                    new ContentMerger(
                                            MergeStrategy.MERGE,
                                            MergeStrategy.KEEP,
                                            MergeStrategy.REPLACE
                                    ),
                                    contentLister, 
                                    brandExtractor());
    }
    
    @Bean
    public RteBrandExtractor brandExtractor() {
        return RteBrandExtractor.create();
    }
    
    @PostConstruct
    public void init() {
        scheduler.schedule(feedUpdater().withName("RTE AZ Feed Ingest"), INGEST_FREQUENCY);
    }
}
