package org.atlasapi.query.v2;

import java.io.IOException;
import java.util.Set;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.application.query.ApiKeyNotFoundException;
import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.application.query.InvalidIpForApiKeyException;
import org.atlasapi.application.query.RevokedApiKeyException;
import org.atlasapi.application.v3.ApplicationAccessRole;
import org.atlasapi.application.v3.ApplicationConfiguration;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.output.AtlasErrorSummary;
import org.atlasapi.output.AtlasModelWriter;
import org.atlasapi.output.MissingApplicationOwlAccessRoleException;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.atlasapi.query.content.parser.QueryStringBackedQueryBuilder;

import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.ids.NumberToShortStringCodec;
import com.metabroadcast.common.ids.SubstitutionTableNumberCodec;
import com.metabroadcast.common.time.DateTimeZones;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.output.Annotation.defaultAnnotations;

public abstract class BaseController<T> {

    protected static final Splitter URI_SPLITTER = Splitter.on(",")
            .omitEmptyStrings()
            .trimResults();

    public final NumberToShortStringCodec idCodec;

    protected final AdapterLog log;
    protected final AtlasModelWriter<? super T> outputter;

    private QueryStringBackedQueryBuilder queryBuilder;
    private final QueryParameterAnnotationsExtractor annotationExtractor;
    private final ApplicationConfigurationFetcher configFetcher;

    protected BaseController(ApplicationConfigurationFetcher configFetcher, AdapterLog log,
            AtlasModelWriter<? super T> outputter,
            NumberToShortStringCodec idCodec) {
        this.configFetcher = checkNotNull(configFetcher);
        this.log = checkNotNull(log);
        this.outputter = checkNotNull(outputter);
        this.queryBuilder = new QueryStringBackedQueryBuilder()
                .withIgnoreParams("apiKey")
                .withIgnoreParams("uri", "id", "event_ids");
        this.annotationExtractor = new QueryParameterAnnotationsExtractor();
        this.idCodec = checkNotNull(idCodec);
    }

    protected BaseController(ApplicationConfigurationFetcher configFetcher, AdapterLog log,
            AtlasModelWriter<? super T> outputter) {
        this(configFetcher, log, outputter, new SubstitutionTableNumberCodec());
    }

    protected void errorViewFor(HttpServletRequest request, HttpServletResponse response,
            AtlasErrorSummary ae) throws IOException {
        log.record(new AdapterLogEntry(
                ae.id(),
                Severity.ERROR,
                new DateTime(DateTimeZones.UTC)
        ).withCause(ae.exception()).withSource(this.getClass()));
        outputter.writeError(request, response, ae);
    }

    protected void modelAndViewFor(
            HttpServletRequest request, HttpServletResponse response,
            @Nullable T queryResult, ApplicationConfiguration config
    ) throws IOException {
        if (queryResult == null) {
            errorViewFor(
                    request,
                    response,
                    AtlasErrorSummary.forException(new NullPointerException("Query result was null"))
            );
        } else {
            outputter.writeTo(
                    request,
                    response,
                    queryResult,
                    annotationExtractor.extract(request).or(defaultAnnotations()),
                    config
            );
        }
    }

    protected ApplicationConfiguration appConfig(HttpServletRequest request)
            throws ApiKeyNotFoundException, RevokedApiKeyException, InvalidIpForApiKeyException {
        Maybe<ApplicationConfiguration> config = possibleAppConfig(request);
        return config.hasValue() ? config.requireValue() : ApplicationConfiguration.forNoApiKey();
    }

    protected Maybe<ApplicationConfiguration> possibleAppConfig(HttpServletRequest request)
            throws ApiKeyNotFoundException, RevokedApiKeyException, InvalidIpForApiKeyException {
        Maybe<ApplicationConfiguration> configuration = configFetcher.configurationFor(request);

        // Use of Owl that does require an API key is still allowed so only check for the
        // appropriate access role if a configuration has been found.
        if (configuration.hasValue()
                && !configuration.requireValue().hasAccessRole(ApplicationAccessRole.OWL_ACCESS)) {
            throw MissingApplicationOwlAccessRoleException.create();
        }

        return configuration;
    }

    protected ContentQuery buildQuery(HttpServletRequest request)
            throws ApiKeyNotFoundException, RevokedApiKeyException, InvalidIpForApiKeyException {
        ContentQuery query = queryBuilder.build(request);

        Maybe<ApplicationConfiguration> configuration = possibleAppConfig(request);

        if (configuration.hasValue()) {
            query = query.copyWithApplicationConfiguration(configuration.requireValue());
        }

        return query;
    }

    protected Set<Publisher> publishers(String publisherString, ApplicationConfiguration config) {
        Set<Publisher> appPublishers = ImmutableSet.copyOf(config.getEnabledSources());
        if (Strings.isNullOrEmpty(publisherString)) {
            return appPublishers;
        }

        ImmutableSet<Publisher> build = publishersFrom(publisherString);

        return Sets.intersection(build, appPublishers);
    }

    protected ImmutableSet<Publisher> publishersFrom(String publisherString) {
        ImmutableSet.Builder<Publisher> publishers = ImmutableSet.builder();
        for (String publisherKey : URI_SPLITTER.split(publisherString)) {
            Maybe<Publisher> publisher = Publisher.fromKey(publisherKey);
            if (publisher.hasValue()) {
                publishers.add(publisher.requireValue());
            }
        }
        ImmutableSet<Publisher> build = publishers.build();
        return build;
    }

    protected Set<Specialization> specializations(@Nullable String specializationString) {
        if (specializationString != null) {
            ImmutableSet.Builder<Specialization> specializations = ImmutableSet.builder();
            for (String s : URI_SPLITTER.split(specializationString)) {
                Maybe<Specialization> specialization = Specialization.fromKey(s);
                if (specialization.hasValue()) {
                    specializations.add(specialization.requireValue());
                }
            }
            return specializations.build();
        } else {
            return Sets.newHashSet();
        }
    }
}
