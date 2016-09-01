package org.atlasapi.remotesite.opta.events;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.atlasapi.persistence.content.organisation.OrganisationStore;
import org.atlasapi.persistence.event.EventStore;
import org.atlasapi.persistence.topic.TopicStore;
import org.atlasapi.remotesite.HttpClients;
import org.atlasapi.remotesite.opta.events.model.OptaSportType;
import org.atlasapi.remotesite.opta.events.soccer.OptaSoccerDataTransformer;
import org.atlasapi.remotesite.opta.events.sports.OptaSportsDataHandler;
import org.atlasapi.remotesite.opta.events.sports.OptaSportsDataTransformer;
import org.atlasapi.remotesite.opta.events.sports.model.SportsMatchData;
import org.atlasapi.remotesite.opta.events.sports.model.SportsTeam;

import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.common.properties.Parameter;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;

import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;
import joptsimple.internal.Strings;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

public class OptaEventsModule {

    private @Value("${opta.events.http.baseUrl}") String baseUrl;
    private static final String OPTA_HTTP_SOCCER_CONFIG_PREFIX = "opta.events.http.sports.soccer.";
    private static final String OPTA_HTTP_RUGBY_CONFIG_PREFIX = "opta.events.http.sports.rugby.";

    private @Value("${opta.events.http.credentials.rugby.username}") String rugbyUsername;
    private @Value("${opta.events.http.credentials.rugby.password}") String rugbyPassword;
    private @Value("${opta.events.http.credentials.soccer.username}") String soccerUsername;
    private @Value("${opta.events.http.credentials.soccer.password}") String soccerPassword;

    private static final RepetitionRule FOOTBALL_REPETITION_RULE = RepetitionRules.daily(new LocalTime(14, 0, 0));
    private static final RepetitionRule OTHER_SPORTS_REPETITION_RULE = RepetitionRules.daily(new LocalTime(12, 0, 0));

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private @Autowired SimpleScheduler scheduler;
    private @Autowired EventStore eventStore;
    private @Autowired OrganisationStore organisationStore;
    private @Autowired @Qualifier("topicStore") TopicStore topicStore;

    @PostConstruct
    public void startBackgroundTasks() {
        
        scheduler.schedule(soccerIngestTask().withName("Opta Events (Football) Updater"), FOOTBALL_REPETITION_RULE);
        scheduler.schedule(nonFootballIngestTask().withName("Opta Events (Non Football) Updater"), OTHER_SPORTS_REPETITION_RULE);
    }
    
    private OptaEventsIngestTask<SportsTeam, SportsMatchData> soccerIngestTask() {
        Map<String, String> credentials = getCredentials(OPTA_HTTP_SOCCER_CONFIG_PREFIX);
        Map<OptaSportType, OptaSportConfiguration> sportConfig = sportConfig(OPTA_HTTP_SOCCER_CONFIG_PREFIX);
        return new OptaEventsIngestTask<SportsTeam, SportsMatchData>(httpEventsFetcher(
                sportConfig, soccerTransformer(), credentials),
                dataHandler(sportConfig)
        );
    }

    private OptaEventsIngestTask<SportsTeam, SportsMatchData> nonFootballIngestTask() {
        Map<String, String> credentials = getCredentials(OPTA_HTTP_RUGBY_CONFIG_PREFIX);
        Map<OptaSportType, OptaSportConfiguration> sportConfig = sportConfig(OPTA_HTTP_RUGBY_CONFIG_PREFIX);
        return new OptaEventsIngestTask<SportsTeam, SportsMatchData>(sportsFetcher(sportConfig, credentials), dataHandler(sportConfig));
    }

    private OptaDataTransformer<SportsTeam, SportsMatchData> soccerTransformer() {
        return new OptaSoccerDataTransformer();
    }

    private OptaEventsFetcher<SportsTeam, SportsMatchData> sportsFetcher(
            Map<OptaSportType, OptaSportConfiguration> sportConfig,
            Map<String, String> credentials) {
        return httpEventsFetcher(sportConfig, sportsTransformer(), credentials);
    }
    
    private OptaDataTransformer<SportsTeam, SportsMatchData> sportsTransformer() {
        return new OptaSportsDataTransformer();
    }

    private OptaEventsFetcher<SportsTeam, SportsMatchData> httpEventsFetcher(
            Map<OptaSportType, OptaSportConfiguration> sportConfig, 
            OptaDataTransformer<SportsTeam, SportsMatchData> dataTransformer,
            Map<String, String> credentials) {

        return new HttpOptaEventsFetcher<>(sportConfig, HttpClients.webserviceClient(),
                dataTransformer, baseUrl, credentials);
    }

    private OptaSportsDataHandler dataHandler(Map<OptaSportType, OptaSportConfiguration> config) {
        return new OptaSportsDataHandler(organisationStore, eventStore, utility(config));
    }

    /**
     * Opta Sports are configured through three parameters:-
     * <ul>
     *  <li>feed type
     *  <li>competition id
     *  <li>season
     *  <li>(Optional)prefix to remove from the ID supplied by Opta 
     * <p>
     * Each sport is held in an environment param suffixed with the 
     * sport's enum value, and the three parameters are pipe delimited. 
     * This method reads any environment params with the supplied suffix 
     * and splits out the three config params into a {@link OptaSportConfiguration} 
     * object, and returns a map of sport to configuration.
     * 
     * @param sportPrefix the environment parameter prefix for the sport subset desired
     * @return
     */
    private Map<OptaSportType, OptaSportConfiguration> sportConfig(String sportPrefix) {
        Builder<OptaSportType, OptaSportConfiguration> configMapping = ImmutableMap.<OptaSportType, OptaSportConfiguration>builder();

        for (Entry<String, Parameter> property : Configurer.getParamsWithKeyMatching(Predicates.containsPattern(sportPrefix))) {
            String sportKey = property.getKey().substring(sportPrefix.length());
            String sportConfig = property.getValue().get();
            
            if (!Strings.isNullOrEmpty(sportConfig)) {
                OptaSportType sport = OptaSportType.valueOf(sportKey.toUpperCase());
                OptaSportConfiguration config = parseConfig(sportConfig);
                configMapping.put(sport, config);
            } else {
                log.warn("Opta HTTP configuration for sport {} is missing.", sportKey);
            }
        }
        return configMapping.build();
    }

    /**
     * Checks if sport prefix is either soccer or rugby, then adds username and password to the map.
     * @param sportPrefix the environment parameter prefix for sports
     * @return
     */
    private Map<String, String> getCredentials(String sportPrefix) {
        Builder<String, String> credentialsMap = ImmutableMap.<String, String>builder();
        if (sportPrefix.equals(OPTA_HTTP_SOCCER_CONFIG_PREFIX)) {
            credentialsMap.put("username", soccerUsername);
            credentialsMap.put("password", soccerPassword);
        } else if (sportPrefix.equals(OPTA_HTTP_RUGBY_CONFIG_PREFIX)) {
            credentialsMap.put("username", rugbyUsername);
            credentialsMap.put("password", rugbyPassword);
        } else {
            throw new IllegalArgumentException("Invalid Sport prefix, couldn't get OPTA HTTP credentials.");
        }
        return credentialsMap.build();
    }

    /**
     * Parses a String parameter into a set of three parameters required for the Opta Sports 
     * competition API. The format is [prefix].[sportName]=feedType|competition|seasonId
     * @param sportConfig
     * @return
     */
    private OptaSportConfiguration parseConfig(String sportConfig) {
        Iterable<String> configItems = Splitter.on('|').split(sportConfig);
        OptaSportConfiguration.Builder config 
            = OptaSportConfiguration.builder()
                .withFeedType(Iterables.get(configItems, 0))
                .withCompetition(Iterables.get(configItems, 1))
                .withSeasonId(Iterables.get(configItems, 2));
        
        if (Iterables.size(configItems) == 4) {
            config.withPrefixToStripFromId(Iterables.get(configItems, 3));
        }
        
        return config.build();
    }

    private OptaEventsUtility utility(Map<OptaSportType, OptaSportConfiguration> config) {
        return new OptaEventsUtility(topicStore, config);
    }
}
