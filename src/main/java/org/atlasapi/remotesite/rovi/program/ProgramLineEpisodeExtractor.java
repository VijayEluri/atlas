package org.atlasapi.remotesite.rovi.program;

import static org.atlasapi.remotesite.rovi.RoviUtils.canonicalUriForProgram;
import static org.atlasapi.remotesite.rovi.RoviUtils.canonicalUriForSeason;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.remotesite.rovi.KeyedFileIndex;
import org.atlasapi.remotesite.rovi.series.RoviEpisodeSequenceLine;
import org.atlasapi.remotesite.rovi.series.RoviSeasonHistoryLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

/*
 * Extracts an {@link Episode} from a {@link RoviProgramLine} with {@link RoviShowType} SE (Series Episode)
 */
public class ProgramLineEpisodeExtractor extends ProgramLineBaseExtractor<RoviProgramLine, Episode> {

    private final KeyedFileIndex<String, RoviEpisodeSequenceLine> episodeSequenceIndex;
    private final KeyedFileIndex<String, RoviSeasonHistoryLine> seasonHistoryIndex;
    
    public ProgramLineEpisodeExtractor(
            KeyedFileIndex<String, RoviProgramDescriptionLine> descriptionIndex,
            KeyedFileIndex<String, RoviEpisodeSequenceLine> episodeSequenceIndex,
            KeyedFileIndex<String, RoviSeasonHistoryLine> seasonHistoryIndex,
            ContentResolver contentResolver) {
        super(descriptionIndex, contentResolver);
        this.episodeSequenceIndex = episodeSequenceIndex;
        this.seasonHistoryIndex = seasonHistoryIndex;
    }

    private final static Logger LOG = LoggerFactory.getLogger(ProgramLineEpisodeExtractor.class);
    
    @Override
    protected Episode createContent() {
        return new Episode();
    }

    @Override
    protected Episode addSpecificData(Episode content, RoviProgramLine programLine) {
        setBrandAndSeriesFromProgramLine(content, programLine);
        setEpisodeNumberIfNumeric(content, programLine);
        setEpisodeTitleIfPresent(content, programLine);
        setDataFromEpisodeSequenceIfPossible(content, programLine);
        setSeasonNumberFromSeasonHistoryIfNeeded(content, programLine);
        
        return content;
    }

    private void setSeasonNumberFromSeasonHistoryIfNeeded(Episode content,
            RoviProgramLine programLine) {
        if (content.getSeriesNumber() == null && programLine.getSeriesId().isPresent()) {
            Collection<RoviSeasonHistoryLine> results = seasonHistoryIndex.getLinesForKey(programLine.getSeasonId()
                    .get());
            RoviSeasonHistoryLine seasonHistory = Iterables.getFirst(results, null);

            if (seasonHistory != null && seasonHistory.getSeasonNumber().isPresent()) {
                content.setSeriesNumber(seasonHistory.getSeasonNumber().get());
            }
        }
    }

    private void setDataFromEpisodeSequenceIfPossible(Episode content,
            RoviProgramLine programLine) {
        Collection<RoviEpisodeSequenceLine> results = episodeSequenceIndex.getLinesForKey(programLine.getKey());
        RoviEpisodeSequenceLine episodeSequence = Iterables.getFirst(results, null);

        // If found episodeSequence from index then override some values
        if (episodeSequence != null) {
            if (episodeSequence.getEpisodeTitle().isPresent()) {
                content.setTitle(episodeSequence.getEpisodeTitle().get());
            }

            if (episodeSequence.getEpisodeSeasonSequence().isPresent()) {
                content.setEpisodeNumber(episodeSequence.getEpisodeSeasonSequence().get());
            }

            if (episodeSequence.getEpisodeSeasonNumber().isPresent()) {
                content.setSeriesNumber(episodeSequence.getEpisodeSeasonNumber().get());
            }

        }
    }
    
    private void setEpisodeTitleIfPresent(Episode content, RoviProgramLine programLine) {
        if (programLine.getEpisodeTitle().isPresent()) {
            content.setTitle(programLine.getEpisodeTitle().get());
        }
    }

    private void setEpisodeNumberIfNumeric(Episode content, RoviProgramLine programLine) {
        if (programLine.getEpisodeNumber().isPresent() && StringUtils.isNumeric(programLine.getEpisodeNumber().get())) {
            try {
                content.setEpisodeNumber(Integer.valueOf(programLine.getEpisodeNumber().get()));
            } catch (NumberFormatException e) {
                // Ignoring episode number
            }
        }
    }

    private void setBrandAndSeriesFromProgramLine(Episode content, RoviProgramLine programLine) {
        if (programLine.getSeriesId().isPresent()) {
            String seriesCanonicalUri = canonicalUriForProgram(programLine.getSeriesId().get());
            content.setParentRef(new ParentRef(seriesCanonicalUri));
        }
        
        if (programLine.getSeasonId().isPresent()) {
            String seasonCanonicalUri = canonicalUriForSeason(programLine.getSeasonId().get());
            content.setSeriesRef(new ParentRef(seasonCanonicalUri));
        }
    }

    @Override
    protected Logger log() {
        return LOG;
    }

}
