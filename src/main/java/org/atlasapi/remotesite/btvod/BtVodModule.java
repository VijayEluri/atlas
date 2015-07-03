package org.atlasapi.remotesite.btvod;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentGroupResolver;
import org.atlasapi.persistence.content.ContentGroupWriter;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.topic.TopicCreatingTopicResolver;
import org.atlasapi.persistence.topic.TopicWriter;
import org.atlasapi.remotesite.HttpClients;
import org.atlasapi.remotesite.btvod.contentgroups.BtVodContentGroupPredicates;
import org.atlasapi.remotesite.btvod.contentgroups.BtVodContentGroupUpdater;
import org.atlasapi.remotesite.btvod.portal.PortalClient;
import org.atlasapi.remotesite.btvod.portal.XmlPortalClient;
import org.atlasapi.remotesite.util.OldContentDeactivator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.SimpleHttpClientBuilder;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;

@Configuration
public class BtVodModule {

    private static final int THRESHOLD_FOR_NOT_REMOVING_OLD_CONTENT = 75;
    private static final String PORTAL_BOXSET_GROUP = "03_tv/40_searcha-z/all";
    private static final String PORTAL_BOXOFFICE_GROUP = "01_boxoffice/05_new/all";
    private static final String PORTAL_BUY_TO_OWN_GROUP = "01_boxoffice/Must_Own_Movies_Categories/New_To_Own";
    private static final String BOX_OFFICE_PICKS_GROUP = "50_misc_car_you/Misc_metabroadcast/Misc_metabroadcast_1";
    
    private static final String NEW_CONTENT_MPX_FEED_NAME = "btv-prd-nav-new";
    
    private static final String MUSIC_CATEGORY = "Music";
    private static final String FILM_CATEGORY = "Film";
    private static final String TV_CATEGORY = "TV";
    private static final String KIDS_CATEGORY = "Kids";
    private static final String SPORT_CATEGORY = "Sport";
    private static final String BUY_TO_OWN_CATEGORY = "BuyToOwn";
    private static final String TV_BOX_SETS_CATEGORY = "TvBoxSets";
    private static final String BOX_OFFICE_CATEGORY = "BoxOffice";
    private static final String CZN_CONTENT_PROVIDER_ID = "CHC";
    private static final String BOX_OFFICE_PICKS_CATEGORY = "BoxOfficePicks";
    private static final String NEW_CATEGORY = "New";
    
    private static final String URI_PREFIX = "http://vod.bt.com/";
    private static final String TVE_URI_PREFIX = "http://tve-vod.bt.com";
    
    @Autowired
    private SimpleScheduler scheduler;
    @Autowired
    private ContentResolver contentResolver;
    @Autowired
    private ContentWriter contentWriter;
    @Autowired
    private ContentLister contentLister;
    @Autowired
    private ContentGroupResolver contentGroupResolver;
    @Autowired
    private ContentGroupWriter contentGroupWriter;
    @Autowired
    private TopicCreatingTopicResolver topicResolver;
    @Autowired
    @Qualifier("topicStore")
    private TopicWriter topicWriter;
    @Value("${bt.vod.file}")
    private String filename;
    @Value("${bt.portal.baseUri}")
    private String btPortalBaseUri;
    @Value("${bt.portal.contentGroups.baseUri}")
    private String btPortalContentGroupsBaseUri;
    @Value("${bt.vod.mpx.feed.baseUrl}")
    private String btVodMpxFeedBaseUrl;
   
    
    @Bean
    public BtVodUpdater btVodUpdater() {
        return new BtVodUpdater(contentResolver, 
                contentWriter, btVodData(), URI_PREFIX, btVodContentGroupUpdater(Publisher.BT_VOD, URI_PREFIX), 
                describedFieldsExtractor(), Publisher.BT_VOD, oldContentDeactivator(Publisher.BT_VOD),
                noImageExtractor(), URI_PREFIX, noImageExtractor(), brandUriExtractor(URI_PREFIX));
    }
    
    @Bean
    public BtVodUpdater btTveVodUpdater() {
        return new BtVodUpdater(contentResolver, 
                contentWriter, btVodData(), TVE_URI_PREFIX, btVodContentGroupUpdater(Publisher.BT_TVE_VOD, TVE_URI_PREFIX), 
                describedFieldsExtractor(), Publisher.BT_TVE_VOD, oldContentDeactivator(Publisher.BT_TVE_VOD),
                brandImageExtractor(TVE_URI_PREFIX), TVE_URI_PREFIX, itemImageExtractor(), brandUriExtractor(TVE_URI_PREFIX));
    }
    
    private BtVodOldContentDeactivator oldContentDeactivator(Publisher publisher) {
        return new BtVodOldContentDeactivator(
                        publisher, 
                        new OldContentDeactivator(contentLister, contentWriter, contentResolver), 
                        THRESHOLD_FOR_NOT_REMOVING_OLD_CONTENT);
    }
    
    public BtVodDescribedFieldsExtractor describedFieldsExtractor() {
        return new BtVodDescribedFieldsExtractor(new BtVodMpxImageExtractor(btPortalBaseUri), topicResolver, topicWriter);
    }
    
    public DerivingFromItemBrandImageExtractor brandImageExtractor(String baseUrl) {
        return new DerivingFromItemBrandImageExtractor(brandUriExtractor(baseUrl), baseUrl);
    }
    
    public ImageExtractor itemImageExtractor() {
        return new BtVodMpxImageExtractor(btPortalBaseUri);
    }
    
    public BrandUriExtractor brandUriExtractor(String uriPrefix) {
        return new BrandUriExtractor(uriPrefix, titleSanitiser());
    }
    
    @Bean
    public TitleSanitiser titleSanitiser() {
        return new TitleSanitiser();
    }
    public NoImageExtractor noImageExtractor() {
        return new NoImageExtractor();
    }
    
    public BtVodContentGroupUpdater btVodContentGroupUpdater(Publisher publisher, String uriPrefix) {
        return new BtVodContentGroupUpdater(contentGroupResolver, contentGroupWriter, 
                contentGroupsAndCriteria(), uriPrefix, publisher);
    }
    
    private BtVodData btVodData() {
        return new BtVodData(
                mpxVodClient()
        );
    }

    @Bean
    public HttpBtMpxVodClient mpxVodClient() {
        return new HttpBtMpxVodClient(
                new SimpleHttpClientBuilder().withUserAgent(HttpClients.ATLAS_USER_AGENT).build(),
                new HttpBtMpxFeedRequestProvider(btVodMpxFeedBaseUrl)
        );
    }
    
    private Map<String, BtVodContentGroupPredicate> contentGroupsAndCriteria() {
        return ImmutableMap.<String, BtVodContentGroupPredicate> builder()
                .put(MUSIC_CATEGORY.toLowerCase(), BtVodContentGroupPredicates.schedulerChannelPredicate(MUSIC_CATEGORY))
                .put(FILM_CATEGORY.toLowerCase(), BtVodContentGroupPredicates.filmPredicate())
                .put(TV_CATEGORY.toLowerCase(), BtVodContentGroupPredicates.schedulerChannelPredicate(TV_CATEGORY))
                .put(KIDS_CATEGORY.toLowerCase(), BtVodContentGroupPredicates.schedulerChannelPredicate(KIDS_CATEGORY))
                .put(SPORT_CATEGORY.toLowerCase(), BtVodContentGroupPredicates.schedulerChannelPredicate(SPORT_CATEGORY))
                .put(CZN_CONTENT_PROVIDER_ID.toLowerCase(), BtVodContentGroupPredicates.cznPredicate())
                .put(BUY_TO_OWN_CATEGORY.toLowerCase(), BtVodContentGroupPredicates.portalContentGroupPredicate(portalClient(), PORTAL_BUY_TO_OWN_GROUP, null))
                .put(BOX_OFFICE_CATEGORY.toLowerCase(), BtVodContentGroupPredicates.portalContentGroupPredicate(portalClient(), PORTAL_BOXOFFICE_GROUP, null))
                .put(TV_BOX_SETS_CATEGORY.toLowerCase(), BtVodContentGroupPredicates.portalContentGroupPredicate(portalClient(), PORTAL_BOXSET_GROUP, Series.class))
                .put(BOX_OFFICE_PICKS_CATEGORY.toLowerCase(), BtVodContentGroupPredicates.portalContentGroupPredicate(portalClient(), BOX_OFFICE_PICKS_GROUP, null))
                .put(NEW_CATEGORY.toLowerCase(), BtVodContentGroupPredicates.mpxContentGroupPredicate(mpxVodClient(), NEW_CONTENT_MPX_FEED_NAME))
                .build();
    }
    
    @Bean
    public PortalClient portalClient() {
        return new XmlPortalClient(btPortalContentGroupsBaseUri, 
                new SimpleHttpClientBuilder()
                        .withUserAgent(HttpClients.ATLAS_USER_AGENT)
                        .withRetries(3)
                        .build());
    }
    
    @PostConstruct
    public void scheduleTask() {
        scheduler.schedule(btVodUpdater().withName("BT VoD Updater"), RepetitionRules.NEVER);
        scheduler.schedule(btTveVodUpdater().withName("BT TVE VoD Updater"), RepetitionRules.NEVER);
    }
}
