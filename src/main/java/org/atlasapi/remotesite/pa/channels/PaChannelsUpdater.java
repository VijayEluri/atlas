package org.atlasapi.remotesite.pa.channels;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Unmarshaller.Listener;
import javax.xml.parsers.SAXParserFactory;

import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.remotesite.pa.channels.bindings.TvChannelData;
import org.atlasapi.remotesite.pa.data.PaProgrammeDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.XMLReader;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.scheduling.ScheduledTask;

public class PaChannelsUpdater extends ScheduledTask {
    
    private static final String SERVICE = "PA";
    private static final Pattern FILEDATE = Pattern.compile("^.*(\\d{8})_tv_channel_data.xml$");

    private final PaProgrammeDataStore dataStore;
    private final PaChannelsProcessor processor;
    private final Logger log = LoggerFactory.getLogger(PaChannelsUpdater.class);

    public PaChannelsUpdater(PaProgrammeDataStore dataStore, PaChannelsProcessor processor) {
        this.dataStore = dataStore;
        this.processor = processor;
    }
    
    @Override
    public void runTask() {
        processFiles(dataStore.localChannelsFiles(Predicates.<File>alwaysTrue()));
    }
    
    public void processFiles(Iterable<File> files) {
        try { 
            JAXBContext context = JAXBContext.newInstance("org.atlasapi.remotesite.pa.channels.bindings");
            Unmarshaller unmarshaller = context.createUnmarshaller();

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setContentHandler(unmarshaller.getUnmarshallerHandler());

            int filesProcessed = 0;

            for (File file : files) {
                if(!shouldContinue()) {
                    break;
                }
                try {
                    final String filename = file.toURI().toString();
                    Matcher matcher = FILEDATE.matcher(filename);

                    if (matcher.matches()) {
                        log.info("Processing file " + file.toString());
                        final File fileToProcess = dataStore.copyForProcessing(file);

                        unmarshaller.setListener(featuresProcessingListener());
                        reader.parse(fileToProcess.toURI().toString());

                        filesProcessed++;
                        FileUploadResult.successfulUpload(SERVICE, file.getName());
                    }
                    else {
                        log.info("Not processing file " + file.toString() + " as filename format is not recognised");
                        FileUploadResult.failedUpload(SERVICE, file.getName()).withMessage("Format not recognised");
                    }
                } catch (Exception e) {
                    FileUploadResult.failedUpload(SERVICE, file.getName()).withCause(e);
                    log.error("Error processing file " + file.toString(), e);
                }
            }

            reportStatus(String.format("found %s files, processed %s files", Iterables.size(files), filesProcessed));
        } catch (Exception e) {
            log.error("Exception running PA channels updater", e);
            // this will stop the task
            Throwables.propagate(e);
        }
    }

    private Listener featuresProcessingListener() {
        return new Unmarshaller.Listener() {
            public void beforeUnmarshal(Object target, Object parent) {
            }

            public void afterUnmarshal(Object target, Object parent) {
                if (target instanceof TvChannelData) {
                    processor.process((TvChannelData) target);
                }
            }
        };
    }

}
