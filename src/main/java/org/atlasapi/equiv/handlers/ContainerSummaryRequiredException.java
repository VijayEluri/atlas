package org.atlasapi.equiv.handlers;

import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Item;

public class ContainerSummaryRequiredException extends RuntimeException {

    private Item item;

    public ContainerSummaryRequiredException(Item item) {
        super("Container summary not found so equivalence is being rerun on container with id: "
                + item.getContainer().getId() + "and then item with id: " + item.getId());
        this.item = item;

    }

    public Item getItem() {
        return item;
    }
}
