package org.atlasapi.equiv.scorers;

import org.atlasapi.equiv.results.description.ResultDescription;
import org.atlasapi.equiv.results.scores.DefaultScoredCandidates;
import org.atlasapi.equiv.results.scores.Score;
import org.atlasapi.equiv.results.scores.ScoredCandidates;
import org.atlasapi.equiv.update.metadata.EquivToTelescopeComponent;
import org.atlasapi.equiv.update.metadata.EquivToTelescopeResult;
import org.atlasapi.media.entity.Container;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class ContainerYearScorer implements EquivalenceScorer<Container> {

    private static final String NAME = "Container-Year";

    private final Score matchScore;

    public ContainerYearScorer(Score matchScore) {
        this.matchScore = checkNotNull(matchScore);
    }

    @Override
    public ScoredCandidates<Container> score(
            Container subject,
            Set<? extends Container> candidates,
            ResultDescription desc,
            EquivToTelescopeResult equivToTelescopeResult
    ) {
        EquivToTelescopeComponent scorerComponent = EquivToTelescopeComponent.create();
        scorerComponent.setComponentName("Container Year Scorer");
        DefaultScoredCandidates.Builder<Container> scoredCandidates = DefaultScoredCandidates.fromSource(NAME);

        for (Container candidate : candidates) {
            Score score = score(subject, candidate);

            scoredCandidates.addEquivalent(candidate, score);
            scorerComponent.addComponentResult(
                    candidate.getId(),
                    String.valueOf(score.asDouble())
            );
        }

        equivToTelescopeResult.addScorerResult(scorerComponent);

        return scoredCandidates.build();
    }

    private Score score(Container subject, Container candidate) {
        if (subject.getYear() == null || candidate.getYear() == null) {
            return Score.nullScore();
        }
        return subject.getYear().equals(candidate.getYear()) ? matchScore : Score.ZERO;
    }

    @Override
    public String toString(){
        return NAME;
    }
}
