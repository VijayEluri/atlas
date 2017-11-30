package org.atlasapi.equiv.results.extractors;

import java.util.List;
import java.util.Set;

import org.atlasapi.equiv.results.description.ResultDescription;
import org.atlasapi.equiv.results.scores.ScoredCandidate;
import org.atlasapi.equiv.update.metadata.EquivToTelescopeResults;

import com.google.common.base.Optional;


public interface EquivalenceExtractor<T> {

    /**
     * Extracts the equivalent pieces of content from an ordered list of weighted candidates.
     * @param candidates - equivalence candidates for a single publisher, ordered from highest scoring to lowest.
     * @param subject - the subject content
     * @param desc TODO
     * @return strong equivalent or absent if none of the candidates  
     */
    Set<ScoredCandidate<T>> extract(
            List<ScoredCandidate<T>> candidates,
            T subject,
            ResultDescription desc,
            EquivToTelescopeResults equivToTelescopeResults
    );
    
}
