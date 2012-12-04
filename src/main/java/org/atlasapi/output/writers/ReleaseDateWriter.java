package org.atlasapi.output.writers;

import java.io.IOException;

import org.atlasapi.media.entity.ReleaseDate;
import org.atlasapi.query.v4.schedule.EntityListWriter;
import org.atlasapi.query.v4.schedule.FieldWriter;
import org.atlasapi.query.v4.schedule.OutputContext;

public final class ReleaseDateWriter implements EntityListWriter<ReleaseDate> {

    @Override
    public void write(ReleaseDate entity, FieldWriter writer, OutputContext ctxt) throws IOException {
        writer.writeField("release_date", entity.date());
        writer.writeField("country", entity.country());
        writer.writeField("type", entity.type().toString().toLowerCase());
    }

    @Override
    public String listName() {
        return "release_dates";
    }

    @Override
    public String fieldName() {
        return "release_date";
    }
}