package org.atlasapi.equiv.update.updaters.providers.container;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.atlasapi.equiv.generators.TitleSearchGenerator;
import org.atlasapi.equiv.handlers.DelegatingEquivalenceResultHandler;
import org.atlasapi.equiv.handlers.EpisodeMatchingEquivalenceHandler;
import org.atlasapi.equiv.handlers.EquivalenceSummaryWritingHandler;
import org.atlasapi.equiv.handlers.LookupWritingEquivalenceHandler;
import org.atlasapi.equiv.handlers.ResultWritingEquivalenceHandler;
import org.atlasapi.equiv.messengers.QueueingEquivalenceResultMessenger;
import org.atlasapi.equiv.results.combining.AddingEquivalenceCombiner;
import org.atlasapi.equiv.results.extractors.AllOverOrEqThresholdExtractor;
import org.atlasapi.equiv.results.filters.ConjunctiveFilter;
import org.atlasapi.equiv.results.filters.DummyContainerFilter;
import org.atlasapi.equiv.results.filters.ExclusionListFilter;
import org.atlasapi.equiv.results.filters.MediaTypeFilter;
import org.atlasapi.equiv.results.filters.MinimumScoreFilter;
import org.atlasapi.equiv.results.filters.UnpublishedContentFilter;
import org.atlasapi.equiv.results.scores.Score;
import org.atlasapi.equiv.scorers.ContainerYearScorer;
import org.atlasapi.equiv.scorers.TitleMatchingContainerScorer;
import org.atlasapi.equiv.update.ContentEquivalenceUpdater;
import org.atlasapi.equiv.update.EquivalenceUpdater;
import org.atlasapi.equiv.update.updaters.providers.EquivalenceUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.EquivalenceUpdaterProviderDependencies;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Publisher;

import java.util.Set;

public class ImdbApiContainerUpdateProvider implements EquivalenceUpdaterProvider<Container> {

    private ImdbApiContainerUpdateProvider() {

    }

    public static ImdbApiContainerUpdateProvider create() {
        return new ImdbApiContainerUpdateProvider();
    }

    @Override
    public EquivalenceUpdater<Container> getUpdater(
            EquivalenceUpdaterProviderDependencies dependencies,
            Set<Publisher> targetPublishers
    ) {
        return ContentEquivalenceUpdater.<Container>builder()
                .withExcludedUris(dependencies.getExcludedUris())
                .withExcludedIds(dependencies.getExcludedIds())
                .withGenerator(
                        TitleSearchGenerator.create(
                                dependencies.getSearchResolver(),
                                Container.class,
                                targetPublishers,
                                2,
                                false,
                                false
                        ) //scorer name is same as actual title scorer so should be same score to prevent one being overwritten
                )
                .withScorers(
                        ImmutableSet.of(
                                new TitleMatchingContainerScorer(2.0),
                                new ContainerYearScorer(Score.ONE)
                        )
                )
                .withCombiner(
                        new AddingEquivalenceCombiner<>()
                )
                .withFilter(
                        ConjunctiveFilter.valueOf(ImmutableList.of(
                                new MinimumScoreFilter<>(2.9),
                                new MediaTypeFilter<>(),
                                new DummyContainerFilter<>(),
                                new UnpublishedContentFilter<>(),
                                ExclusionListFilter.create(
                                        dependencies.getExcludedUris(),
                                        dependencies.getExcludedIds()
                                )
                        ))
                )
                .withExtractor(
                        AllOverOrEqThresholdExtractor.create(3D)
                )
                .withHandler(
                        new DelegatingEquivalenceResultHandler<>(ImmutableList.of(
                                LookupWritingEquivalenceHandler.create(
                                        dependencies.getLookupWriter()
                                ),
                                new EpisodeMatchingEquivalenceHandler(
                                        dependencies.getContentResolver(),
                                        dependencies.getEquivSummaryStore(),
                                        dependencies.getLookupWriter(),
                                        targetPublishers
                                ),
                                new ResultWritingEquivalenceHandler<>(
                                        dependencies.getEquivalenceResultStore()
                                ),
                                new EquivalenceSummaryWritingHandler<>(
                                        dependencies.getEquivSummaryStore()
                                )
                        ))
                )
                .withMessenger(
                        QueueingEquivalenceResultMessenger.create(
                                dependencies.getMessageSender(),
                                dependencies.getLookupEntryStore()
                        )
                )
                .build();
    }
}