package org.atlasapi.equiv.generators;

import java.util.List;
import java.util.Set;

import org.atlasapi.equiv.results.description.ResultDescription;
import org.atlasapi.equiv.results.scores.ScoredCandidates;
import org.atlasapi.media.entity.Content;

import com.metabroadcast.common.ids.SubstitutionTableNumberCodec;
import com.metabroadcast.common.stream.MoreCollectors;

import com.google.api.client.util.Lists;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class EquivalenceGenerators<T extends Content> {
    
    public static <T extends Content> EquivalenceGenerators<T> from(Iterable<? extends EquivalenceGenerator<T>> generators, Set<String> excludedUris) {
        return new EquivalenceGenerators<T>(generators, excludedUris);
    }

    private final List<? extends EquivalenceGenerator<T>> generators;
    private final Set<String> excludedUris;
    private final SubstitutionTableNumberCodec codec;

    public EquivalenceGenerators(
            Iterable<? extends EquivalenceGenerator<T>> generators,
            Set<String> excludedUris
    ) {
        this.generators = ImmutableList.copyOf(generators);
        this.excludedUris = excludedUris;
        this.codec = SubstitutionTableNumberCodec.lowerCaseOnly();
    }
    
    public List<ScoredCandidates<T>> generate(T content, ResultDescription desc) {
        desc.startStage("Generating equivalences");
        Builder<ScoredCandidates<T>> generatedScores = ImmutableList.builder();

        ImmutableList<Long> excludedIds = excludedUris.stream()
                .filter(id -> !id.contains("http"))
                .map(id -> codec.decode(id).longValue())
                .collect(MoreCollectors.toImmutableList());

        if (excludedUris.contains(content.getCanonicalUri())
                || excludedIds.contains(content.getId())) {
            desc.appendText("Content %s is in equivalence blacklist and will not be equivalated",
                    content.getCanonicalUri());
            return generatedScores.build();
        }

        if (!content.isActivelyPublished()) {
            return generatedScores.build();
        }

        for (EquivalenceGenerator<T> generator : generators) {
            try {
                desc.startStage(generator.toString());
                generatedScores.add(generator.generate(content, desc));
                desc.finishStage();
            } catch (Exception e) {
                throw new RuntimeException(String.format("Exception running %s for %s", generator, content), e);
            }
        }
        
        desc.finishStage();
        return generatedScores.build();
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("generators", generators)
                .toString();
    }
}
