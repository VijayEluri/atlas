package org.atlasapi.remotesite.opta.events.sports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;

import org.atlasapi.persistence.topic.TopicStore;
import org.atlasapi.remotesite.opta.events.OptaEventsUtility;
import org.atlasapi.remotesite.opta.events.OptaSportConfiguration;
import org.atlasapi.remotesite.opta.events.model.OptaSportType;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;


public class OptaEventsUtilityTest {
    
    private final OptaSportConfiguration rugbyConfig = 
            OptaSportConfiguration.builder()
                                  .withCompetition("competition")
                                  .withFeedType("feedType")
                                  .withSeasonId("season")
                                  .withPrefixToStripFromId("t")
                                  .build();
    
    private final OptaSportConfiguration soccerConfig = 
            OptaSportConfiguration.builder()
                                  .withCompetition("competition")
                                  .withFeedType("feedType")
                                  .withSeasonId("season")
                                  .build();
    
    private final Map<OptaSportType, OptaSportConfiguration> config = ImmutableMap.of(
            OptaSportType.RUGBY_AVIVA_PREMIERSHIP, rugbyConfig,
            OptaSportType.FOOTBALL_PREMIER_LEAGUE, soccerConfig);
            
    private TopicStore topicStore = Mockito.mock(TopicStore.class);
    private final OptaEventsUtility utility = new OptaEventsUtility(topicStore, config);

    @Test
    public void testTimeZoneMapping() {
        Optional<DateTimeZone> fetched = utility.fetchTimeZone(OptaSportType.RUGBY_AVIVA_PREMIERSHIP);
        
        assertEquals(DateTimeZone.forID("Europe/London"), fetched.get());
    }

    @Test
    public void testReturnsAbsentForUnmappedValue() {
        Optional<DateTimeZone> fetched = utility.fetchTimeZone(null);
        
        assertFalse(fetched.isPresent());
    }
    
    @Test
    public void testRemovesConfiguredLeadingCharacterFromId() {
        assertEquals("http://optasports.com/teams/12345", 
                utility.createTeamUri(OptaSportType.RUGBY_AVIVA_PREMIERSHIP, "t12345"));
        assertEquals("http://optasports.com/teams/r12345", 
                utility.createTeamUri(OptaSportType.RUGBY_AVIVA_PREMIERSHIP, "r12345"));
        
        assertEquals("http://optasports.com/teams/t12345", 
                utility.createTeamUri(OptaSportType.FOOTBALL_PREMIER_LEAGUE, "t12345"));
    }
    
}
