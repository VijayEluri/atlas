package org.atlasapi.remotesite.knowledgemotion;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Set;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentCategory;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.listing.ContentListingCriteria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import com.metabroadcast.common.ingest.monitorclient.model.Entity;
import com.metabroadcast.common.ingest.s3.process.ProcessingResult;

public class KnowledgeMotionUpdater {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeMotionUpdater.class);

    private final KnowledgeMotionDataRowHandler dataHandler;
    private final Iterable<KnowledgeMotionSourceConfig> allKmPublishers;
    private final ContentLister contentLister;
    private final ObjectMapper mapper;

    private Set<String> seenUris;

    public KnowledgeMotionUpdater(Iterable<KnowledgeMotionSourceConfig> sources,
        KnowledgeMotionContentMerger dataHandler,
        ContentLister contentLister) {
        this.dataHandler = checkNotNull(dataHandler);
        this.allKmPublishers = checkNotNull(sources);
        this.contentLister = checkNotNull(contentLister);

        seenUris = Sets.newHashSet();

        this.mapper = new ObjectMapper();
    }

    protected ProcessingResult.Builder process(Iterator<KnowledgeMotionDataRow> rows,
            ProcessingResult.Builder resultBuilder) {
        if (!rows.hasNext()) {
            log.info("Knowledgemotion Common Ingest received an empty file");
            resultBuilder.error(String.format("Empty input file %s"));
        }

        KnowledgeMotionDataRow firstRow = rows.next();
        String publisherRowHeader = firstRow.getSource();
        Publisher publisher = null;
        for (KnowledgeMotionSourceConfig config : allKmPublishers) {
            if (publisherRowHeader.equals(config.rowHeader())) {
                publisher = config.publisher();
            }
        }
        if (publisher == null) {
            StringBuilder errorText = new StringBuilder();
            errorText.append(
                    "First row did not contain a recognised publisher in the 'Source' column."
            ).append("\n");
            errorText.append("Found: " + publisherRowHeader).append("\n");
            errorText.append("Valid publishers are: ").append("\n");
            for (KnowledgeMotionSourceConfig config : allKmPublishers) {
                errorText.append(config.rowHeader()).append("\n");
            }

            resultBuilder.error(String.format("input file %s",  errorText.toString()));
        }

        boolean allRowsSuccess = true;
        if (!writeRow(firstRow, resultBuilder)) {
            allRowsSuccess = false;
        }
        while (rows.hasNext()) {
            if (!writeRow(rows.next(), resultBuilder)) {
                allRowsSuccess = false;
            }
        }

        /*
         * If all rows of this processing run completed successfully,
         * unpublish everything else by this publisher
         */
        if (allRowsSuccess) {
            Iterator<Content> allStoredKmContent = contentLister.listContent(
                    ContentListingCriteria.defaultCriteria()
                            .forContent(ContentCategory.TOP_LEVEL_ITEM)
                            .forPublisher(publisher).build()
            );
            while (allStoredKmContent.hasNext()) {
                Content item = allStoredKmContent.next();
                if (!seenUris.contains(item.getCanonicalUri())) {
                    item.setActivelyPublished(false);
                    dataHandler.write(item);
                }
            }
        }

        return resultBuilder;
    }

    /**
     * @return success
     */
    private boolean writeRow(KnowledgeMotionDataRow row, ProcessingResult.Builder resultBuilder) {
        try {
            Optional<Content> written = dataHandler.handle(row);
            if (written.isPresent()) {
                seenUris.add(written.get().getCanonicalUri());
            }
            log.debug("Successfully updated row {}", row.getId());
            resultBuilder.addEntity(Entity.success()
                    .withId(row.getId())
                    .withRaw(mapper.writeValueAsString(row))
                    .build()
            );
            return true;
        } catch (RuntimeException e) {
            log.debug("Failed to update row {}", row.getId(), e);
            resultBuilder.addEntity(Entity.failure()
                    .withId(row.getId())
                    .withError(String.format(
                            "While merging content: %s \n%s",
                            row.getId(),
                            e.getMessage()
                    ))
                    .build());
            return false;
        } catch (JsonProcessingException e) {
            log.debug("Failed to convert Knowledge Motion row to a JSON. ", row.getId(), e);
            resultBuilder.addEntity(Entity.failure()
                    .withId(row.getId())
                    .withError(String.format(
                            "While converting Knowledge Motion row to a JSON object: %s \n%s",
                            row.getId(),
                            e.getMessage()
                    ))
                    .build());
            return false;
        }
    }
}