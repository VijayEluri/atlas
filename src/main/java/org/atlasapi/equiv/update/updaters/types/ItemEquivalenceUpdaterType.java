package org.atlasapi.equiv.update.updaters.types;

import org.atlasapi.equiv.update.updaters.providers.EquivalenceUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.item.BettyItemUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.item.BroadcastItemUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.item.BtVodItemUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.item.StrictStandardUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.item.MusicItemUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.item.NopItemUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.item.RoviItemUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.item.RtItemUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.item.RtUpcomingItemUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.item.StandardItemUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.item.VodItemUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.item.VodItemWithSeriesSequenceUpdaterProvider;
import org.atlasapi.equiv.update.updaters.providers.item.YouviewItemUpdaterProvider;
import org.atlasapi.media.entity.Item;

import static com.google.common.base.Preconditions.checkNotNull;

public enum ItemEquivalenceUpdaterType {
    NOP_ITEM(
            NopItemUpdaterProvider.create()
    ),
    STANDARD_ITEM(
            StandardItemUpdaterProvider.create()
    ),
    RT_UPCOMING_ITEM(
            RtUpcomingItemUpdaterProvider.create()
    ),
    STRICT_ITEM(
            StrictStandardUpdaterProvider.create()
    ),
    BROADCAST_ITEM(
            BroadcastItemUpdaterProvider.create()
    ),
    ROVI_ITEM(
            RoviItemUpdaterProvider.create()
    ),
    RT_ITEM(
            RtItemUpdaterProvider.create()
    ),
    YOUVIEW_ITEM(
            YouviewItemUpdaterProvider.create()
    ),
    BETTY_ITEM(
            BettyItemUpdaterProvider.create()
    ),
    VOD_ITEM(
            VodItemUpdaterProvider.create()
    ),
    VOD_WITH_SERIES_SEQUENCE_ITEM(
            VodItemWithSeriesSequenceUpdaterProvider.create()
    ),
    BT_VOD_ITEM(
            BtVodItemUpdaterProvider.create()
    ),
    MUSIC_ITEM(
            MusicItemUpdaterProvider.create()
    ),
    ;

    private final EquivalenceUpdaterProvider<Item> provider;

    ItemEquivalenceUpdaterType(EquivalenceUpdaterProvider<Item> provider) {
        this.provider = checkNotNull(provider);
    }

    public EquivalenceUpdaterProvider<Item> getProvider() {
        return provider;
    }
}
