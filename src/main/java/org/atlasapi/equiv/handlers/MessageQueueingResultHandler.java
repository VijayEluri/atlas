package org.atlasapi.equiv.handlers;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.atlasapi.equiv.results.EquivalenceResult;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.LookupRef;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.messaging.v3.ContentEquivalenceAssertionMessage;
import org.atlasapi.messaging.v3.ContentEquivalenceAssertionMessage.AdjacentRef;
import org.atlasapi.persistence.lookup.entry.LookupEntry;
import org.atlasapi.persistence.lookup.entry.LookupEntryStore;

import com.metabroadcast.columbus.telescope.client.IngestTelescopeClientImpl;
import com.metabroadcast.common.ids.NumberToShortStringCodec;
import com.metabroadcast.common.ids.SubstitutionTableNumberCodec;
import com.metabroadcast.common.queue.MessageSender;
import com.metabroadcast.common.stream.MoreCollectors;
import com.metabroadcast.common.time.SystemClock;
import com.metabroadcast.common.time.Timestamp;
import com.metabroadcast.common.time.Timestamper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class MessageQueueingResultHandler<T extends Content>
        implements EquivalenceResultHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(MessageQueueingResultHandler.class);
    
    private final MessageSender<ContentEquivalenceAssertionMessage> sender;
    private final Timestamper stamper;
    private final ImmutableSet<String> sourceKeys;
    private final LookupEntryStore lookupEntryStore;

    private final NumberToShortStringCodec entityIdCodec;

    private MessageQueueingResultHandler(
            MessageSender<ContentEquivalenceAssertionMessage> sender,
            Iterable<Publisher> sources,
            LookupEntryStore lookupEntryStore,
            Timestamper stamper
    ) {
        this.sender = checkNotNull(sender);
        this.stamper = checkNotNull(stamper);
        this.sourceKeys = StreamSupport.stream(sources.spliterator(), false)
                .map(Publisher::key)
                .collect(MoreCollectors.toImmutableSet());
        this.lookupEntryStore = checkNotNull(lookupEntryStore);

        this.entityIdCodec = SubstitutionTableNumberCodec.lowerCaseOnly();
    }

    public static <T extends Content> MessageQueueingResultHandler<T> create(
            MessageSender<ContentEquivalenceAssertionMessage> sender,
            Iterable<Publisher> sources,
            LookupEntryStore lookupEntryStore
    ) {
        return new MessageQueueingResultHandler<>(
                sender, sources, lookupEntryStore, new SystemClock()
        );
    }

    @VisibleForTesting
    static <T extends Content> MessageQueueingResultHandler<T> create(
            MessageSender<ContentEquivalenceAssertionMessage> sender,
            Iterable<Publisher> sources,
            LookupEntryStore lookupEntryStore,
            Timestamper timestamper
    ) {
        return new MessageQueueingResultHandler<>(
                sender, sources, lookupEntryStore, timestamper
        );
    }

    @Override
    public void handle(
            EquivalenceResult<T> result,
            Optional<String> taskId
    ) {
        try {
            ContentEquivalenceAssertionMessage message = messageFrom(result);
            for (AdjacentRef adjacentRef : message.getAdjacent()) {
                log.trace(
                        "Subject: {} Adjacent: {}",
                        result.subject().getCanonicalUri(),
                        adjacentRef.toString()
                );
            }
            sender.sendMessage(message, getMessagePartitionKey(result.subject()));
        } catch (Exception e) {
            log.error("Failed to send equiv update message: " + result.subject(), e);
        }
    }

    private ContentEquivalenceAssertionMessage messageFrom(EquivalenceResult<T> result) {
        String messageId = UUID.randomUUID().toString();
        Timestamp timestamp = stamper.timestamp();
        T subject = result.subject();
        String subjectId = entityIdCodec.encode(BigInteger.valueOf(subject.getId()));
        String subjectType = subject.getClass().getSimpleName().toLowerCase();
        String subjectSource = subject.getPublisher().key();

        return new ContentEquivalenceAssertionMessage(
                messageId,
                timestamp,
                subjectId,
                subjectType,
                subjectSource,
                adjacents(result),
                sourceKeys
        );
    }

    private List<AdjacentRef> adjacents(EquivalenceResult<T> result) {
        return Lists.newArrayList(Collections2.transform(result.strongEquivalences().values(),
                input -> {
                    @SuppressWarnings("ConstantConditions")
                    T candidate = input.candidate();

                    return new AdjacentRef(
                        entityIdCodec.encode(BigInteger.valueOf(candidate.getId())),
                        candidate.getClass().getSimpleName().toLowerCase(),
                        candidate.getPublisher().key()
                    );
                }
        ));
    }

    private byte[] getMessagePartitionKey(T subject) {
        Iterable<LookupEntry> lookupEntries = lookupEntryStore.entriesForIds(
                ImmutableSet.of(subject.getId())
        );

        Optional<LookupEntry> lookupEntryOptional = StreamSupport.stream(
                lookupEntries.spliterator(),
                false
        )
                .findFirst();

        if (lookupEntryOptional.isPresent()) {
            LookupEntry lookupEntry = lookupEntryOptional.get();

            // Given most of the time the equivalence results do not change the existing graph
            // (due to the fact that we are often rerunning equivalence on the same items with
            // the same results) the underlying graph will remain unchanged. Therefore if we get
            // the smallest lookup entry ID from that graph that ID should be consistent enough
            // to use as a partition key and ensure updates on the same graph end up on the same
            // partition.
            Optional<Long> graphId = ImmutableSet.<LookupRef>builder()
                    .addAll(lookupEntry.equivalents())
                    .addAll(lookupEntry.explicitEquivalents())
                    .addAll(lookupEntry.directEquivalents())
                    .build()
                    .stream()
                    .map(LookupRef::id)
                    .sorted()
                    .findFirst();

            if (graphId.isPresent()) {
                return Longs.toByteArray(graphId.get());
            }
        }

        // Default to returning the subject ID as the partition key
        return Longs.toByteArray(subject.getId());
    }
}
