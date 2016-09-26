package org.atlasapi.remotesite.wikipedia;

import javax.annotation.PostConstruct;

import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.organisation.OrganisationWriter;
import org.atlasapi.persistence.content.people.PersonWriter;
import org.atlasapi.remotesite.wikipedia.film.FilmArticleTitleSource;
import org.atlasapi.remotesite.wikipedia.film.FilmExtractor;
import org.atlasapi.remotesite.wikipedia.football.FootballTeamsExtractor;
import org.atlasapi.remotesite.wikipedia.football.TeamsNamesSource;
import org.atlasapi.remotesite.wikipedia.people.PeopleExtractor;
import org.atlasapi.remotesite.wikipedia.people.PeopleNamesSource;
import org.atlasapi.remotesite.wikipedia.television.TvBrandArticleTitleSource;
import org.atlasapi.remotesite.wikipedia.television.TvBrandHierarchyExtractor;
import org.atlasapi.remotesite.wikipedia.updaters.FilmsUpdater;
import org.atlasapi.remotesite.wikipedia.updaters.FootballTeamsUpdater;
import org.atlasapi.remotesite.wikipedia.updaters.PeopleUpdater;
import org.atlasapi.remotesite.wikipedia.updaters.TvBrandHierarchyUpdater;
import org.atlasapi.remotesite.wikipedia.wikiparsers.ArticleFetcher;
import org.atlasapi.remotesite.wikipedia.wikiparsers.FetchMeister;

import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;

import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WikipediaModule {
    private static final Logger log = LoggerFactory.getLogger(WikipediaModule.class);

    private @Autowired SimpleScheduler scheduler;
    private @Value("${updaters.wikipedia.films.enabled}") Boolean filmTaskEnabled;
    private @Value("${updaters.wikipedia.tv.enabled}") Boolean tvTaskEnabled;
    private @Value("${updaters.wikipedia.football.enabled}") Boolean footballTaskEnabled;
    private @Value("${updaters.wikipedia.people.enabled}") Boolean peopleTaskEnabled;

    private @Value("${updaters.wikipedia.films.simultaneousness}") int filmsSimultaneousness;
    private @Value("${updaters.wikipedia.films.threads}") int filmsThreads;
    private @Value("${updaters.wikipedia.tv.simultaneousness}") int tvSimultaneousness;
    private @Value("${updaters.wikipedia.tv.threads}") int tvThreads;
    private @Value("${updaters.wikipedia.football.simultaneousness}") int footballSimultaneousness;
    private @Value("${updaters.wikipedia.football.threads}") int footballThreads;
    private @Value("${updaters.wikipedia.people.simultaneousness}") int peopleSimultaneousness;
    private @Value("${updaters.wikipedia.people.threads}") int peopleThreads;

    private @Autowired @Qualifier("contentWriter") ContentWriter contentWriter;
    private @Autowired @Qualifier("organisationStore") OrganisationWriter organisationWriter;
    private @Autowired @Qualifier("personStore") PersonWriter personStore;

    private final EnglishWikipediaClient ewc = new EnglishWikipediaClient();
    protected final ArticleFetcher fetcher = ewc;
    protected final FetchMeister fetchMeister = new FetchMeister(fetcher);
    
    protected final FilmExtractor filmExtractor = new FilmExtractor();
    protected final FilmArticleTitleSource allFilmsTitleSource = ewc;
    
    protected final TvBrandHierarchyExtractor tvBrandHierarchyExtractor = new TvBrandHierarchyExtractor();
    protected final TvBrandArticleTitleSource allTvBrandsTitleSource = ewc;

    protected final FootballTeamsExtractor footballTeamsExtractor = new FootballTeamsExtractor();
    protected final TeamsNamesSource teamsNamesSource = ewc;

    protected final PeopleExtractor peopleExtractor = new PeopleExtractor();
    protected final PeopleNamesSource peopleNamesSource = ewc;
    
    @PostConstruct
    public void setUp() {
        if (filmTaskEnabled) {
            scheduler.schedule(
                    allFilmsUpdater().withName("Wikipedia films updater"),
                    RepetitionRules.daily(new LocalTime(21, 0, 0))
            );
            log.info("Wikipedia film update scheduled task installed");
        }
        if (tvTaskEnabled) {
            scheduler.schedule(
                    allTvBrandsUpdater().withName("Wikipedia TV updater"),
                    RepetitionRules.daily(new LocalTime(21, 0, 0))
            );
            log.info("Wikipedia TV update scheduled task installed");
        }
        if (footballTaskEnabled) {
            scheduler.schedule(
                    allTeamsUpdater().withName("Wikipedia football updater"),
                    RepetitionRules.daily(new LocalTime(21, 0, 0))
            );
            log.info("Wikipedia football update scheduled task installed");
        }
        if (peopleTaskEnabled) {
            scheduler.schedule(
                    allPeopleUpdater().withName("Wikipedia people updater"),
                    RepetitionRules.daily(new LocalTime(21, 0, 0))
            );
            log.info("Wikipedia people update scheduled task installed");
        }
    }
    
    @Bean
    public WikipediaUpdatesController updatesController() {
        return new WikipediaUpdatesController(this);
    }
    
    public FilmsUpdater allFilmsUpdater() {
        return new FilmsUpdater(allFilmsTitleSource, fetchMeister, filmExtractor, contentWriter, filmsSimultaneousness, filmsThreads);
    }
    
    public FilmsUpdater filmsUpdaterForTitles(FilmArticleTitleSource titleSource) {
        return new FilmsUpdater(titleSource, fetchMeister, filmExtractor, contentWriter, filmsSimultaneousness, filmsThreads);
    }
    
    public TvBrandHierarchyUpdater allTvBrandsUpdater() {
        return new TvBrandHierarchyUpdater(allTvBrandsTitleSource, fetchMeister, tvBrandHierarchyExtractor, contentWriter, tvSimultaneousness, tvThreads);
    }
    
    public TvBrandHierarchyUpdater tvBrandsUpdaterForTitles(TvBrandArticleTitleSource titleSource) {
        return new TvBrandHierarchyUpdater(titleSource, fetchMeister, tvBrandHierarchyExtractor, contentWriter, tvSimultaneousness, tvThreads);
    }

    public FootballTeamsUpdater allTeamsUpdater() {
        return new FootballTeamsUpdater(teamsNamesSource, fetchMeister, footballTeamsExtractor, organisationWriter, footballSimultaneousness, footballThreads);
    }

    public FootballTeamsUpdater teamsUpdaterForTitles(TeamsNamesSource titleSource) {
        return new FootballTeamsUpdater(titleSource, fetchMeister, footballTeamsExtractor, organisationWriter, footballSimultaneousness, footballThreads);
    }

    public PeopleUpdater allPeopleUpdater() {
        return new PeopleUpdater(peopleNamesSource, fetchMeister, peopleExtractor, personStore, peopleSimultaneousness, peopleThreads);
    }

    public PeopleUpdater peopleUpdaterForTitles(PeopleNamesSource titleSource) {
        return new PeopleUpdater(titleSource, fetchMeister, peopleExtractor, personStore, peopleSimultaneousness, peopleThreads);
    }
}
