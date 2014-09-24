package org.atlasapi.remotesite.bt.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.atlasapi.media.entity.Event;
import org.atlasapi.media.entity.Organisation;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Topic;
import org.atlasapi.persistence.content.organisation.OrganisationStore;
import org.atlasapi.persistence.event.EventStore;
import org.atlasapi.persistence.topic.TopicStore;
import org.atlasapi.remotesite.bt.events.feedModel.BtEvent;
import org.atlasapi.remotesite.bt.events.feedModel.BtEventsFeed;
import org.atlasapi.remotesite.bt.events.feedModel.BtTeam;
import org.atlasapi.remotesite.bt.events.model.BtEventsData;
import org.atlasapi.remotesite.bt.events.model.BtSportType;
import org.atlasapi.remotesite.events.EventTopicResolver;
import org.atlasapi.remotesite.events.EventsUriCreator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.metabroadcast.common.base.Maybe;


public class BtEventsDataHandlerTest {

    private Gson gson = new GsonBuilder().create();
    private OrganisationStore organisationStore = Mockito.mock(OrganisationStore.class);
    private EventStore eventStore = Mockito.mock(EventStore.class);
    private TopicStore topicStore = Mockito.mock(TopicStore.class);
    private BtEventsFieldMapper mapper = new BtEventsFieldMapper();
    private EventTopicResolver topicResolver = new EventTopicResolver(topicStore);
    private EventsUriCreator uriCreator = new BtEventsUriCreator();
    private final BtEventsDataHandler handler = new BtEventsDataHandler(
            organisationStore, eventStore, topicResolver, mapper, uriCreator);
    private BtEventsData feedData;
    
    public BtEventsDataHandlerTest() throws JsonSyntaxException, JsonIOException, IOException {
        this.feedData = readDataFromFile("moto_gp_feed.json");
    }
    
    @Before
    public void setup() {
        Mockito.when(topicStore.topicFor(Mockito.matches("dbpedia"), Mockito.anyString())).then(new Answer<Maybe<Topic>>() {
            @Override
            public Maybe<Topic> answer(InvocationOnMock invocation) throws Throwable {
                Topic topic = new Topic(1234l);
                topic.setNamespace("dbpedia");
                topic.setValue((String) invocation.getArguments()[1]);
                return Maybe.just(topic);
            }
        });
    }
    
    
    @Test
    public void testTeamsIgnored() {
        Optional<Organisation> parsed = handler.parseOrganisation(new BtTeam(), BtSportType.MOTO_GP);
        assertFalse(parsed.isPresent());
    }
    
    @Test
    public void testEventParsing() {
        BtEvent match = Iterables.get(feedData.matches(), 1);
        Optional<Event> parsed = handler.parseEvent(match, BtSportType.MOTO_GP);
        
        Event parsedEvent = parsed.get();

        assertEquals("http://bt.com/events/" + match.id(), parsedEvent.getCanonicalUri());
        assertEquals(match.name(), parsedEvent.title());
        assertEquals(Publisher.BT_EVENTS, parsedEvent.publisher());
        assertEquals(mapper.fetchLocationUrl(match.location()).get(), parsedEvent.venue().getValue());
        assertEquals(new DateTime(2014, 3, 20, 0, 0, 0, 563, DateTimeZone.UTC), parsedEvent.startTime());
        assertEquals(new DateTime(2014, 03, 23, 11, 23, 17, 833, DateTimeZone.UTC), parsedEvent.endTime());
        assertTrue(parsedEvent.organisations().isEmpty());
        assertTrue(parsedEvent.participants().isEmpty());
        Map<String, String> groupUrls = mapper.fetchEventGroupUrls(BtSportType.MOTO_GP);
        Iterable<Topic> expectedEventGroups = topicResolver.createOrResolveEventGroups(groupUrls);
        assertEquals(transformToValues(expectedEventGroups), transformToValues(parsedEvent.eventGroups()));
        assertTrue(parsedEvent.content().isEmpty());
    }

    private Set<String> transformToValues(Iterable<Topic> topics) {
        return ImmutableSet.copyOf(Iterables.transform(topics, new Function<Topic, String>() {
            @Override
            public String apply(Topic input) {
                return input.getValue();
            }
        }));
    }

    private BtEventsData readDataFromFile(String filename) throws JsonSyntaxException, JsonIOException, IOException {
        URL testFile = Resources.getResource(getClass(), filename);
        BtEventsFeed feedData = gson.fromJson(new InputStreamReader(testFile.openStream()), BtEventsFeed.class);
        return new BtEventsData(feedData.response().docs());
    }

}
