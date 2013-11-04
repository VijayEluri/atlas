package org.atlasapi.application;

import java.util.List;

import javax.annotation.Nullable;

import org.atlasapi.media.common.Id;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.output.NotFoundException;
import org.atlasapi.output.ResourceForbiddenException;
import org.atlasapi.output.useraware.UserAwareQueryResult;
import org.atlasapi.persistence.application.ApplicationStore;
import org.atlasapi.query.common.QueryExecutionException;
import org.atlasapi.query.common.useraware.UserAwareQuery;
import org.atlasapi.query.common.useraware.UserAwareQueryExecutor;
import org.atlasapi.application.users.Role;
import org.atlasapi.application.users.User;
import org.atlasapi.content.criteria.AttributeQuery;
import org.atlasapi.content.criteria.AttributeQuerySet;
import org.atlasapi.content.criteria.IdAttributeQuery;
import org.atlasapi.content.criteria.QueryVisitorAdapter;
import org.atlasapi.content.criteria.attribute.Attributes;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class ApplicationQueryExecutor implements UserAwareQueryExecutor<Application> {

    private final ApplicationStore applicationStore;

    public ApplicationQueryExecutor(ApplicationStore applicationStore) {
        this.applicationStore = applicationStore;
    }

    @Override
    public UserAwareQueryResult<Application> execute(UserAwareQuery<Application> query)
            throws QueryExecutionException {
        return query.isListQuery() ? multipleQuery(query) : singleQuery(query);
    }

    private UserAwareQueryResult<Application> singleQuery(UserAwareQuery<Application> query) throws NotFoundException, ResourceForbiddenException {
        Id id = query.getOnlyId();
        if (!userCanAccessApplication(id, query)) {
            throw new ResourceForbiddenException();
        }
       
        Optional<Application> application = applicationStore.applicationFor(id);
        if (application.isPresent()) {
            return UserAwareQueryResult.singleResult(application.get(), query.getContext());
        } else {
            throw new NotFoundException(id);
        }

    }

    private UserAwareQueryResult<Application> multipleQuery(UserAwareQuery<Application> query) {
        AttributeQuerySet operands = query.getOperands();
        User user = query.getContext().getUser().get();

        Iterable<Id> ids = Iterables.concat(operands.accept(new QueryVisitorAdapter<List<Id>>() {
           @Override
            public List<Id> visit(IdAttributeQuery query) {
                return query.getValue();
            }}));
        Publisher reads = null;
            
        Publisher writes = null;

        for (AttributeQuery<?> operand : operands) {
             if (operand.getAttributeName().equals(Attributes.SOURCE_READS.externalName())) {
                reads = (Publisher) Iterables.getOnlyElement(operand.getValue());
            } else if (operand.getAttributeName().equals(Attributes.SOURCE_WRITES.externalName())) {
                writes = (Publisher) Iterables.getOnlyElement(operand.getValue());
            }
        }
        
        Iterable<Application> results = null;
        
        if (!Iterables.isEmpty(ids)) {
            results = applicationStore.applicationsFor(ids);
        } else if (reads != null) {
            results = applicationStore.allApplications();
        } else if (writes != null) {
            results = applicationStore.writersFor(writes);
        } else {
            if (query.getContext().isAdminUser()) {
                results = applicationStore.allApplications();
            } else {
                results = applicationStore.applicationsFor(user.getApplicationIds());
            }
        }
        
        if (query.getContext().isAdminUser()) {
            return UserAwareQueryResult.listResult(results, query.getContext());
        } else {
            return UserAwareQueryResult.listResult(filterByUserViewable(results, user), query.getContext());
        }
    }
    
    private boolean userCanAccessApplication(Id id, UserAwareQuery<Application> query) {
        Optional<User> user = query.getContext().getUser();
        if (!user.isPresent()) {
            return false;
        } else {
            return user.get().is(Role.ADMIN) || user.get().getApplicationIds().contains(id);
        }
    }
    
    private Iterable<Application> filterByUserViewable(Iterable<Application> applications, final User user) {
        return Iterables.filter(applications, new Predicate<Application>() {

            @Override
            public boolean apply(@Nullable Application input) {
                return user.getApplicationIds().contains(input.getId());
            }});
    }
}
