package org.atlasapi.equiv.update;

import java.util.Set;

import org.atlasapi.equiv.update.metadata.EquivalenceUpdaterMetadata;
import org.atlasapi.equiv.update.metadata.NopEquivalenceUpdaterMetadata;
import org.atlasapi.media.entity.Publisher;

public class NullEquivalenceUpdater<T> implements EquivalenceUpdater<T> {

    private enum NullUpdater implements EquivalenceUpdater<Object> {
        INSTANCE {
            @Override
            public boolean updateEquivalences(
                    Object content
            ) {
                return false;
            }

            @Override
            public EquivalenceUpdaterMetadata getMetadata(Set<Publisher> sources) {
                return NopEquivalenceUpdaterMetadata.create();
            }
        };

        @SuppressWarnings("unchecked")

        <T> EquivalenceUpdater<T> withNarrowedType() {
            return (EquivalenceUpdater<T>) this;
        }
    }

    private NullEquivalenceUpdater() {
    }

    public static <T> EquivalenceUpdater<T> get() {
        return NullUpdater.INSTANCE.withNarrowedType();
    }

    @Override
    public boolean updateEquivalences(T content) {
        return false;
    }

    @Override
    public EquivalenceUpdaterMetadata getMetadata(Set<Publisher> sources) {
        return NopEquivalenceUpdaterMetadata.create();
    }
}
