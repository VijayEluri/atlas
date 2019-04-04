package org.atlasapi.equiv.update.updaters.providers.item;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.atlasapi.equiv.generators.BroadcastMatchingItemEquivalenceGeneratorAndScorer;
import org.atlasapi.equiv.generators.EquivalenceGenerator;
import org.atlasapi.equiv.results.combining.NullScoreAwareAveragingCombiner;
import org.atlasapi.equiv.results.extractors.PercentThresholdAboveNextBestMatchEquivalenceExtractor;
import org.atlasapi.equiv.results.filters.ConjunctiveFilter;
import org.atlasapi.equiv.results.filters.DummyContainerFilter;
import org.atlasapi.equiv.results.filters.ExclusionListFilter;
import org.atlasapi.equiv.results.filters.FilmYearFilter;
import org.atlasapi.equiv.results.filters.MediaTypeFilter;
import org.atlasapi.equiv.results.filters.MinimumScoreFilter;
import org.atlasapi.equiv.results.filters.PublisherFilter;
import org.atlasapi.equiv.results.filters.SpecializationFilter;
import org.atlasapi.equiv.results.filters.UnpublishedContentFilter;
import org.atlasapi.equiv.results.scores.Score;
import org.atlasapi.equiv.scorers.DescriptionMatchingScorer;
import org.atlasapi.equiv.scorers.DescriptionTitleMatchingScorer;
import org.atlasapi.equiv.scorers.SequenceItemScorer;
import org.atlasapi.equiv.scorers.TitleMatchingItemScorer;
import org.atlasapi.equiv.update.ContentEquivalenceResultUpdater;
import org.atlasapi.equiv.update.EquivalenceResultUpdater;
import org.atlasapi.equiv.update.updaters.providers.EquivalenceResultUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.EquivalenceUpdaterProviderDependencies;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.joda.time.Duration;

import java.util.Set;

public class StrictStandardUpdaterProvider implements EquivalenceResultUpdaterProvider<Item> {

    private StrictStandardUpdaterProvider() { }

    public static StrictStandardUpdaterProvider create() {
        return new StrictStandardUpdaterProvider();
    }

    @Override
    public EquivalenceResultUpdater<Item> getUpdater(
            EquivalenceUpdaterProviderDependencies dependencies,
            Set<Publisher> targetPublishers
    ) {
        return ContentEquivalenceResultUpdater.<Item>builder()
                .withExcludedUris(dependencies.getExcludedUris())
                .withExcludedIds(dependencies.getExcludedIds())
                .withGenerators(
                        ImmutableSet.<EquivalenceGenerator<Item>>of(
                                new BroadcastMatchingItemEquivalenceGeneratorAndScorer(
                                        dependencies.getScheduleResolver(),
                                        dependencies.getChannelResolver(),
                                        targetPublishers,
                                        Duration.standardMinutes(5),
                                        Predicates.alwaysTrue()
                                )
                        )
                )
                .withScorers(
                        ImmutableSet.of(
                                new TitleMatchingItemScorer(),
                                new SequenceItemScorer(Score.ONE),
                                new DescriptionTitleMatchingScorer(),
                                DescriptionMatchingScorer.makeScorer()
                        )
                )
                .withCombiner(
                        new NullScoreAwareAveragingCombiner<>(true)
                )
                .withFilter(
                        ConjunctiveFilter.valueOf(ImmutableList.of(
                                new MinimumScoreFilter<>(0.25),
                                new MediaTypeFilter<>(),
                                new SpecializationFilter<>(),
                                new PublisherFilter<>(),
                                ExclusionListFilter.create(
                                        dependencies.getExcludedUris(),
                                        dependencies.getExcludedIds()
                                ),
                                new FilmYearFilter<>(),
                                new DummyContainerFilter<>(),
                                new UnpublishedContentFilter<>()
                        ))
                )
                .withExtractor(
                        PercentThresholdAboveNextBestMatchEquivalenceExtractor
                                .atLeastNTimesGreater(1.5)
                )
//                .withHandler(
//                        //standard
//                        new DelegatingEquivalenceResultHandler<>(ImmutableList.of(
//                                EpisodeFilteringEquivalenceResultHandler.relaxed(
//                                        LookupWritingEquivalenceHandler.create(
//                                                dependencies.getLookupWriter()
//                                        ),
//                                        dependencies.getEquivSummaryStore()
//                                ),
//                                new ResultWritingEquivalenceHandler<>(
//                                        dependencies.getEquivalenceResultStore()
//                                ),
//                                new EquivalenceSummaryWritingHandler<>(
//                                        dependencies.getEquivSummaryStore()
//                                )
//                        ))
//                )
//                .withMessenger(
//                        //standard
//                        QueueingEquivalenceResultMessenger.create(
//                                dependencies.getMessageSender(),
//                                dependencies.getLookupEntryStore()
//                        )
//                )
                .build();
    }
}
