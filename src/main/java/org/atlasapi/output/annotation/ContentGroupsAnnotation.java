package org.atlasapi.output.annotation;


import java.io.IOException;
import java.util.List;

import org.atlasapi.media.content.Content;
import org.atlasapi.media.entity.ContentGroup;
import org.atlasapi.media.entity.ContentGroupRef;
import org.atlasapi.output.writers.ChildRefWriter;
import org.atlasapi.persistence.content.ContentGroupResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.query.v4.schedule.EntityListWriter;
import org.atlasapi.query.v4.schedule.FieldWriter;
import org.atlasapi.query.v4.schedule.OutputContext;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class ContentGroupsAnnotation extends OutputAnnotation<Content> {

    public static final class ContentGroupWriter implements EntityListWriter<ContentGroup> {

        @Override
        public void write(ContentGroup entity, FieldWriter writer, OutputContext ctxt) throws IOException {
            writer.writeList(new ChildRefWriter("content"), entity.getContents(), ctxt);
        }

        @Override
        public String listName() {
            return "content_groups";
        }

        @Override
        public String fieldName() {
            return "content_group";
        }
    }

    private final ContentGroupResolver contentGroupResolver;

    public ContentGroupsAnnotation(ContentGroupResolver resolver) {
        super(Content.class);
        this.contentGroupResolver = resolver;
        contentGroupWriter = new ContentGroupWriter();
    }

    @Override
    public void write(Content entity, FieldWriter writer, OutputContext ctxt) throws IOException {
        writer.writeList(contentGroupWriter, resolveRefs(entity.getContentGroupRefs()), ctxt);
    }

    private Iterable<ContentGroup> resolveRefs(List<ContentGroupRef> refs) {
        return resolveContentGroups(Lists.transform(refs, REF_TO_ID));
    }

    private Iterable<ContentGroup> resolveContentGroups(List<Long> contentGroups) {
        if (contentGroups.isEmpty()) {
            return ImmutableList.of();
        }
        ResolvedContent resolved = contentGroupResolver.findByIds(contentGroups);
        return Iterables.filter(resolved.asResolvedMap().values(), ContentGroup.class);
    }

    private static final Function<ContentGroupRef, Long> REF_TO_ID = new Function<ContentGroupRef, Long>() {

        @Override
        public Long apply(ContentGroupRef input) {
            return input.getId();
        }
    };
    private final ContentGroupWriter contentGroupWriter;
}
