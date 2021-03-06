//package org.atlasapi.remotesite.ictomorrow;
//
//import javax.annotation.PostConstruct;
//
//import org.atlasapi.persistence.logging.AdapterLog;
//import org.atlasapi.persistence.logging.AdapterLogEntry;
//import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
//import org.atlasapi.remotesite.ContentWriters;
//import org.atlasapi.remotesite.archiveorg.ArchiveOrgItemAdapter;
//import org.joda.time.LocalTime;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import com.metabroadcast.common.scheduling.RepetitionRules;
//import com.metabroadcast.common.scheduling.RepetitionRules.Daily;
//import com.metabroadcast.common.scheduling.SimpleScheduler;
//import com.metabroadcast.common.security.UsernameAndPassword;
//import com.metabroadcast.common.social.auth.ictomorrow.ICTomorrowApiHelper;
//
//@Configuration
//public class ICTomorrowModule {
//    
//    private final static Daily AT_NIGHT = RepetitionRules.daily(new LocalTime(5, 0, 0));
//    
//    private @Autowired SimpleScheduler scheduler;
//    private @Value("${ict.username}") String ictUsername;
//    private @Value("${ict.password}") String ictPassword;
//    private @Value("${ict.csa_id}") Integer csaId;
//    private @Autowired ContentWriters contentWriter;
//    private @Autowired AdapterLog log;
//    private @Autowired ArchiveOrgItemAdapter archiveOrgItemAdapter;
//    
//    
//    @PostConstruct
//    public void startBackgroundTasks() {
//        if ("DISABLED".equals(ictUsername) || "DISABLED".equals(ictPassword)) {
//            log.record(new AdapterLogEntry(Severity.INFO).withDescription("Username/Password required for ICTomorrow updater").withSource(getClass()));
//            return;
//        }
//        scheduler.schedule(ictomorrowPlaylistUpdater(), AT_NIGHT);
//        log.record(new AdapterLogEntry(Severity.INFO).withDescription("ICTomorrow update scheduled task installed").withSource(ICTomorrowPlaylistUpdater.class));
//    }
//    
//    public @Bean ICTomorrowPlaylistUpdater ictomorrowPlaylistUpdater() {
//        return new ICTomorrowPlaylistUpdater(new ICTomorrowApiHelper(new UsernameAndPassword(ictUsername, ictPassword)), contentWriter, archiveOrgItemAdapter, csaId);
//    }
//}
