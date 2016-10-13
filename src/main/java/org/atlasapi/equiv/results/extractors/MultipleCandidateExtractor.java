package org.atlasapi.equiv.results.extractors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.atlasapi.equiv.results.DefaultEquivalenceResultBuilder;
import org.atlasapi.equiv.results.scores.ScoredCandidate;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Series;

import com.metabroadcast.common.stream.MoreCollectors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * This extractor attempts to get multiple candidates for the same publisher. It will only do this
 * if
 * <ul>
 *     <li>the target is not a brand or a series</li>
 *     <li>the top scoring candidate is not a brand or a series</li>
 *     <li>it can find a group of candidates whose score is close (within
 *     {@link MultipleCandidateExtractor#BROADCAST_TIME_FLEXIBILITY}) to the top scoring candidate
 *     and that have at least one broadcast that either contains or are contained by any
 *     target broadcast (+- {@link MultipleCandidateExtractor#BROADCAST_TIME_FLEXIBILITY})</li>
 * </ul>
 */
public class MultipleCandidateExtractor<T extends Content> {

    private static final Duration BROADCAST_TIME_FLEXIBILITY = Duration.standardMinutes(5);
    private static final double PUBLISHER_MATCHING_EQUIV_THRESHOLD = 0.3;

    private MultipleCandidateExtractor() {
    }

    public static <T extends Content> MultipleCandidateExtractor<T> create() {
        return new MultipleCandidateExtractor<>();
    }

    public Optional<Set<ScoredCandidate<T>>> extract(
            List<ScoredCandidate<T>> candidates,
            T target
    ) {
        if (isSeriesOrBrand(target) || candidates.isEmpty()) {
            return Optional.absent();
        }

        ScoredCandidate<T> highestScoringCandidate = candidates.get(0);

        if (isSeriesOrBrand(highestScoringCandidate.candidate())) {
            return Optional.absent();
        }

        Set<ScoredCandidate<T>> allowedCandidates = new HashSet<ScoredCandidate<T>>();

        allowedCandidates.add(highestScoringCandidate);

        ImmutableSet<ScoredCandidate<T>> filteredCandidates = candidates.stream()
                .filter(candidate -> !isSeriesOrBrand(candidate.candidate()))
                .collect(MoreCollectors.toImmutableSet());

        ImmutableSet<ScoredCandidate<T>> matchedCandidates = filteredCandidates
                .stream()
                .filter(candidate -> Math.abs(
                        candidate.score().asDouble() - highestScoringCandidate.score().asDouble()
                ) < PUBLISHER_MATCHING_EQUIV_THRESHOLD)
                .filter(candidate -> broadcastsMatchCheck(target, candidate))
                .collect(MoreCollectors.toImmutableSet());

        allowedCandidates.addAll(matchedCandidates);

        // If we have found a group of candidates return it otherwise return absent and let
        // the configured extractor decide instead
        if (allowedCandidates.size() > 1) {
            return Optional.of(allowedCandidates);
        } else {
            return Optional.absent();
        }
    }

    private boolean isSeriesOrBrand(T target) {
        return (target instanceof Brand || target instanceof Series);
    }

    private boolean broadcastsMatchCheck(
            T target,
            ScoredCandidate<T> candidate
    ) {
        return target.getVersions().stream()
                .flatMap(version -> version.getBroadcasts().stream())
                .anyMatch(broadcast -> broadcastsMatchCheck(candidate.candidate(), broadcast))
                ||
                candidate.candidate().getVersions().stream()
                        .flatMap(version -> version.getBroadcasts().stream())
                        .anyMatch(broadcast -> broadcastsMatchCheck(target, broadcast));
    }

    private boolean broadcastsMatchCheck(T candidate, Broadcast broadcast) {
        return candidate.getVersions().stream()
                .flatMap(version -> version.getBroadcasts().stream())
                .anyMatch(broadcast1 -> broadcastsMatchCheckInclusive(broadcast1, broadcast));
    }

    private boolean broadcastsMatchCheckInclusive(Broadcast broadcastOne, Broadcast broadcastTwo) {
        DateTime broadcastTransmissionStart = broadcastOne.getTransmissionTime();
        DateTime broadcastTransmissionEnd = broadcastOne.getTransmissionEndTime();
        DateTime broadcastTwoTransmissionStart = broadcastTwo.getTransmissionTime();
        DateTime broadcastTwoTransmissionEnd = broadcastTwo.getTransmissionEndTime();

        // Check if first broadcast is contained within the second with some flexibility
        return broadcastTransmissionStart.isAfter(
                broadcastTwoTransmissionStart.minus(BROADCAST_TIME_FLEXIBILITY))
                &&
                broadcastTransmissionEnd.isBefore(
                        broadcastTwoTransmissionEnd.plus(BROADCAST_TIME_FLEXIBILITY));
    }
}