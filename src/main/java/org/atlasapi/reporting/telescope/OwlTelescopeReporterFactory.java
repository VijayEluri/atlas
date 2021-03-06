package org.atlasapi.reporting.telescope;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.metabroadcast.columbus.telescope.api.Event;
import com.metabroadcast.columbus.telescope.client.TelescopeReporterFactory;
import com.metabroadcast.columbus.telescope.client.TelescopeReporterName;
import com.metabroadcast.common.properties.Configurer;
import org.atlasapi.AtlasMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OwlTelescopeReporterFactory extends TelescopeReporterFactory {
    private static final Logger log = LoggerFactory.getLogger(OwlTelescopeReporterFactory.class);

    private OwlTelescopeReporterFactory(
            @Nonnull String env,
            @Nonnull String host,
            @Nonnull ThreadPoolExecutor executor,
            @Nullable MetricRegistry metricsRegistry,
            @Nullable String metricsPrefix) {
        super(env, host, executor, metricsRegistry, metricsPrefix);
    }

    //If fewer than this threads are running, a new thread is created. Else things are queued.
    private static final int CORE_THREADS = Integer.parseInt(Configurer.get("telescope.coreReportingThreads").get());
    //If the queue is full, spawn a new thread up to this number.
    private static final int MAX_THREADS = Integer.parseInt(Configurer.get("telescope.maxReportingThreads").get());
    //If new threads cant be spawned. Things that don't fit go to the RejectedExecutionHandler
    private static final int QUEUE_SIZE = Integer.parseInt(Configurer.get("telescope.queueSize").get());
    private static final String THREAD_NAME = Configurer.get("telescope.reportingThreadName").get();
    private static final String METRICS_PREFIX = Configurer.get("telescope.metricsPrefix").get();


    //Implement this as a Singleton
    private static OwlTelescopeReporterFactory INSTANCE;
    public static synchronized OwlTelescopeReporterFactory getInstance() {
        if (INSTANCE == null) {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    CORE_THREADS,
                    MAX_THREADS,
                    10, TimeUnit.SECONDS, //time to put idle threads to sleep.
                    new ArrayBlockingQueue<>(QUEUE_SIZE),
                    new ThreadFactoryBuilder().setNameFormat(THREAD_NAME + " Thread %d").build(),
                    new TelescopeReporterFactory.RejectedExecutionHandlerImpl()
            );
            executor.allowCoreThreadTimeOut(true);

            INSTANCE = new OwlTelescopeReporterFactory( //this should always produce an instance.
                    Configurer.get("telescope.environment").get(),
                    Configurer.get("telescope.host").get(),
                    executor,
                    AtlasMain.metrics,
                    METRICS_PREFIX
            );
        }

        return INSTANCE;
    }

    public OwlTelescopeReporter getTelescopeReporter(TelescopeReporterName reporterName, Event.Type eventType){
       return new OwlTelescopeReporter(reporterName, eventType, this.getEnvironment(), this.getClient());
    }
}
