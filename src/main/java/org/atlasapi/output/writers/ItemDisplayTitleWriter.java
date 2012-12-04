package org.atlasapi.output.writers;

import java.io.IOException;

import org.atlasapi.media.entity.EntityType;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Item.ContainerSummary;
import org.atlasapi.query.v4.schedule.EntityWriter;
import org.atlasapi.query.v4.schedule.FieldWriter;
import org.atlasapi.query.v4.schedule.OutputContext;

public final class ItemDisplayTitleWriter implements EntityWriter<Item> {

    @Override
    public void write(Item entity, FieldWriter writer, OutputContext ctxt) throws IOException {
        writer.writeField("title", title(entity));
        writer.writeField("subtitle", subtitle(entity));
    }

    private String subtitle(Item entity) {
        ContainerSummary summary = entity.getContainerSummary();
        if (summary != null) {
            return summary.getType().equals(EntityType.SERIES.name())
                ? summary.getTitle() + ":" + entity.getTitle()
                : entity.getTitle();
        } else {
            return null;
        }
    }

    private String title(Item entity) {
        ContainerSummary summary = entity.getContainerSummary();
        return summary == null ? entity.getTitle()
                               : summary.getTitle();
    }

    @Override
    public String fieldName() {
        return "display_title";
    }
}