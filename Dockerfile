FROM 448613307115.dkr.ecr.eu-west-1.amazonaws.com/jvm-oracle:latest

ENV SERVER_PORT="8080" \
    PROCESSING_CONFIG="false" \
    BBC_NITRO_RELEASEDATEINGEST_ENABLED="false" \
    EQUIV_STREAM_UPDATER_ENABLED="false" \
    CHANNEL_EQUIV_ENABLED="false" \
    EQUIV_UPDATER_ENABLED="false" \
    EQUIV_UPDATER_YOUVIEWSCHEDULE_ENABLED="false" \
    IDS_GENERATE="false" \
    LAKEVIEW_FEATURE_ADDXBOXONEAVAILABILITY="false" \
    LAKEVIEW_FEATURE_GENERICTITLESENABLED="false" \
    LAKEVIEW_UPLOAD_ENABLED="false" \
    MESSAGING_ENABLED="false" \
    MONGO_AUDIT_ENABLED="true" \
    PA_PEOPLE_ENABLED="false" \
    RP_FTP_ENABLED="false" \
    RP_FTP_MANUALUPLOAD_ENABLED="false" \
    RP_HTTPS_ENABLED="false" \
    RP_HTTPS_MANUALUPLOAD_ENABLED="false" \
    RP_S3_FTP_ENABLED="false" \
    RP_S3_HTTPS_ENABLED="false" \
    SCHEDULE_REPOPULATOR_BBC_SCHEDULED="false" \
    SCHEDULE_REPOPULATOR_C4_SCHEDULED="false" \
    SCHEDULE_REPOPULATOR_FULL_SCHEDULED="false" \
    SCHEDULE_REPOPULATOR_REDUX_SCHEDULED="false" \
    STATUS_CLIENT_HOST="status-service.stage.svc.cluster.local" \
    STATUS_CLIENT_PORT="80" \
    TALKTALK_VALIDATE="false" \
    TELESCOPE_ENVIRONMENT="" \
    TELESCOPE_HOST="" \
    TELESCOPE_CORE_REPORTING_THREADS="" \
    TELESCOPE_MAX_REPORTING_THREADS="" \
    TELESCOPE_QUEUE_SIZE="" \
    TELESCOPE_REPORTING_THREAD_NAME="" \
    TELESCOPE_METRICS_PREFIX="" \
    UPDATERS_BBC_AUDIENCE_DATA_ENABLED="false" \
    UPDATERS_BBC_PRODUCTS_ENABLED="false" \
    UPDATERS_BBC_ENABLED="false" \
    UPDATERS_BBCNITRO_ENABLED="false" \
    UPDATERS_BBCNITRO_OFFSCHEDULE_ENABLED="false" \
    UPDATERS_BT_CHANNELS_ENABLED="false" \
    UPDATERS_BT_EVENTS_ENABLED="false" \
    UPDATERS_BT_ENABLED="false" \
    UPDATERS_BTFEATURED_ENABLED="false" \
    UPDATERS_BTVOD_ENABLED="false" \
    UPDATERS_C4_ENABLED="false" \
    UPDATERS_C4PMLSD_ENABLED="false" \
    UPDATERS_FIVE_ENABLED="false" \
    UPDATERS_GETTY_ENABLED="false" \
    UPDATERS_HBO_ENABLED="false" \
    UPDATERS_HULU_ENABLED="false" \
    UPDATERS_ITUNES_ENABLED="false" \
    UPDATERS_ITVINTERLINKING_ENABLED="false" \
    UPDATERS_ITVWHATSON_ENABLED="false" \
    UPDATERS_KNOWLEDGEMOTION_ENABLED="false" \
    UPDATERS_LAKEVIEW_ENABLED="false" \
    UPDATERS_LOVEFILM_ENABLED="false" \
    UPDATERS_METABROADCAST_ENABLED="false" \
    UPDATERS_METABROADCASTPICKS_ENABLED="false" \
    UPDATERS_OPTA_EVENTS_ENABLED="false" \
    UPDATERS_PA_ENABLED="false" \
    UPDATERS_REDUX_ENABLED="false" \
    UPDATERS_ROVI_ENABLED="false" \
    UPDATERS_RTE_ENABLED="false" \
    UPDATERS_SIMILARCONTENT_ENABLED="false" \
    UPDATERS_TALKTALK_ENABLED="false" \
    UPDATERS_UNBOX_ENABLED="false" \
    UPDATERS_WIKIPEDIA_FILMS_ENABLED="false" \
    UPDATERS_WIKIPEDIA_FOOTBALL_ENABLED="false" \
    UPDATERS_WIKIPEDIA_PEOPLE_ENABLED="false" \
    UPDATERS_WIKIPEDIA_TV_ENABLED="false" \
    UPDATERS_YOUVIEW_ENABLED="false" \
    XMLTV_UPLOAD_ENABLED="false" \
    YOUVIEW_UPLOAD_NITRO_UPLOAD_ENABLED="false" \
    METRICS_GRAPHITE_ENABLED="false" \
    JSSE_ENABLESNIEXTENSION="false" \
    LOG4J_CONFIGURATION="file:////usr/local/jetty/log4j.properties" \
    NIMROD_LOG_LEVEL="INFO" \
    ROOT_LOG_LEVEL="INFO" \
    JVM_MEMORY="5500m" \
    DEBUG_AND_REMOTE_OPTS="" \
    JVM_OPTS="-XX:+UseG1GC \
      -XX:MaxGCPauseMillis=150 \
      -XX:CMSInitiatingOccupancyFraction=60 \
      -XX:+ExitOnOutOfMemoryError"

RUN mkdir -p /data/accessible-dir \
    mkdir -p /data/bbc-audience-data \
    mkdir -p /data/bt \
    mkdir -p /data/common-ingest \
    mkdir -p /data/emipub \
    mkdir -p /data/equiv-results \
    mkdir -p /data/itunes \
    mkdir -p /data/musicbrainz \
    mkdir -p /data/netflix \
    mkdir -p /data/pa \
    mkdir -p /data/rovi \
    mkdir -p /data/unbox \
    mkdir -p /data/ws \
    mkdir -p /data/youview

COPY target/atlas.war /usr/local/jetty/atlas.war
COPY log4j.properties /usr/local/jetty/log4j.properties

WORKDIR /usr/local/jetty

CMD java \
    -Djetty.home="$JETTY_HOME" \
    -Dsun.net.inetaddr.ttl="$SUN_NET_INETADDR_TTL" \
    -DMBST_PLATFORM="$MBST_PLATFORM" \
    -DMONGO.POOLSIZE="$MONGO_POOLSIZE" \
    -Dapplications.client.host="$APPLICATIONS_CLIENT_HOST" \
    -Dapplications.client.env="$APPLICATIONS_CLIENT_ENV" \
    -Datlas.search.host="$ATLAS_SEARCH_HOST" \
    -Daz="$AZ" \
    -Dbbc.audience-data.filename="$BBC_AUDIENCE_DATA_FILENAME" \
    -Dbbc.nitro.apiKey="$BBC_NITRO_APIKEY" \
    -Dbbc.nitro.releaseDateIngest.enabled="$BBC_NITRO_RELEASEDATEINGEST_ENABLED" \
    -Dbbc.nitro.requestPageSize="$BBC_NITRO_REQUESTPAGESIZE" \
    -Dbbc.nitro.requestsPerSecond.fortnight="$BBC_NITRO_REQUESTSPERSECOND_FORTNIGHT" \
    -Dbbc.nitro.requestsPerSecond.today="$BBC_NITRO_REQUESTSPERSECOND_TODAY" \
    -Dbbc.nitro.threadCount.fortnight="$BBC_NITRO_THREADCOUNT_FORTNIGHT" \
    -Dbbc.nitro.threadCount.today="$BBC_NITRO_THREADCOUNT_TODAY" \
    -Dbt.channels.baseUri.production="$BT_CHANNELS_BASEURI_PRODUCTION" \
    -Dbt.channels.baseUri.reference="$BT_CHANNELS_BASEURI_REFERENCE" \
    -Dbt.channels.baseUri.test1="$BT_CHANNELS_BASEURI_TEST1" \
    -Dbt.channels.baseUri.test2="$BT_CHANNELS_BASEURI_TEST2" \
    -Dbt.channels.freeviewPlatformChannelGroupId="$BT_CHANNELS_FREEVIEWPLATFORMCHANNELGROUPID" \
    -Dbt.channels.ingestAdvertiseFrom.production="$BT_CHANNELS_INGESTADVERTISEFROM_PRODUCTION" \
    -Dbt.channels.ingestAdvertiseFrom.reference="$BT_CHANNELS_INGESTADVERTISEFROM_REFERENCE" \
    -Dbt.channels.ingestAdvertiseFrom.test1="$BT_CHANNELS_INGESTADVERTISEFROM_TEST1" \
    -Dbt.channels.ingestAdvertiseFrom.test2="$BT_CHANNELS_INGESTADVERTISEFROM_TEST2" \
    -Dbt.channels.namespace.production="$BT_CHANNELS_NAMESPACE_PRODUCTION" \
    -Dbt.channels.namespace.reference="$BT_CHANNELS_NAMESPACE_REFERENCE" \
    -Dbt.channels.namespace.test1="$BT_CHANNELS_NAMESPACE_TEST1" \
    -Dbt.channels.namespace.test2="$BT_CHANNELS_NAMESPACE_TEST2" \
    -Dbt.password="$BT_PASSWORD" \
    -Dbt.portal.baseUri="$BT_PORTAL_BASEURI" \
    -Dbt.portal.contentGroups.baseUri="$BT_PORTAL_CONTENTGROUPS_BASEURI" \
    -Dbt.timeout="$BT_TIMEOUT" \
    -Dbt.url="$BT_URL" \
    -Dbt.username="$BT_USERNAME" \
    -Dbtfeatured.productBaseUri="$BTFEATURED_PRODUCTBASEURI" \
    -Dbtfeatured.rootDocumentUri="$BTFEATURED_ROOTDOCUMENTURI" \
    -Dc4.apiKey="$C4_APIKEY" \
    -Dc4.auth.key="$C4_AUTH_KEY" \
    -Dc4.auth.password="$C4_AUTH_PASSWORD" \
    -Dc4.keystore.password="$C4_KEYSTORE_PASSWORD" \
    -Dc4.keystore.path="$C4_KEYSTORE_PATH" \
    -Dc4.lakeviewavailability.apiroot="$C4_LAKEVIEWAVAILABILITY_APIROOT" \
    -Dc4.lakeviewavailability.key="$C4_LAKEVIEWAVAILABILITY_KEY" \
    -Dcannon.host.name="$CANNON_HOST_NAME" \
    -Dcannon.host.port="$CANNON_HOST_PORT" \
    -Dcassandra.connectionTimeout="$CASSANDRA_CONNECTIONTIMEOUT" \
    -Dcassandra.port="$CASSANDRA_PORT" \
    -Dcassandra.requestTimeout="$CASSANDRA_REQUESTTIMEOUT" \
    -Dcassandra.seeds="$CASSANDRA_SEEDS" \
    -Dchannel.equiv.enabled="$CHANNEL_EQUIV_ENABLED" \
    -Demipub.dataFile="$EMIPUB_DATAFILE" \
    -Dequiv.excludedUris="$EQUIV_EXCLUDEDURIS" \
    -Dequiv.excludedIds="$EQUIV_EXCLUDEDIDS" \
    -Dequiv.results.directory="$EQUIV_RESULTS_DIRECTORY" \
    -Dequiv.stream-updater.consumers.default="$EQUIV_STREAM_UPDATER_CONSUMERS_DEFAULT" \
    -Dequiv.stream-updater.consumers.max="$EQUIV_STREAM_UPDATER_CONSUMERS_MAX" \
    -Dequiv.stream-updater.enabled="$EQUIV_STREAM_UPDATER_ENABLED" \
    -Dequiv.updater.enabled="$EQUIV_UPDATER_ENABLED" \
    -Dequiv.updater.youviewschedule.enabled="$EQUIV_UPDATER_YOUVIEWSCHEDULE_ENABLED" \
    -Devents.whitelist.ids="$EVENTS_WHITELIST_IDS" \
    -Dfive.apiBaseUrl="$FIVE_APIBASEURL" \
    -Dfive.timeout.socket="$FIVE_TIMEOUT_SOCKET" \
    -Dgetty.client.id="$GETTY_CLIENT_ID" \
    -Dgetty.client.password="$GETTY_CLIENT_PASSWORD" \
    -Dgetty.client.secret="$GETTY_CLIENT_SECRET" \
    -Dgetty.client.user="$GETTY_CLIENT_USER" \
    -Dgoogle.spreadsheet.access.token="$GOOGLE_SPREADSHEET_ACCESS_TOKEN" \
    -Dgoogle.spreadsheet.client.id="$GOOGLE_SPREADSHEET_CLIENT_ID" \
    -Dgoogle.spreadsheet.client.secret="$GOOGLE_SPREADSHEET_CLIENT_SECRET" \
    -Dgoogle.spreadsheet.refresh.token="$GOOGLE_SPREADSHEET_REFRESH_TOKEN" \
    -Dgoogle.spreadsheet.title="$GOOGLE_SPREADSHEET_TITLE" \
    -Dhttp.nonProxyHosts="$HTTP_NONPROXYHOSTS" \
    -Dhttp.proxyHost="$HTTP_PROXYHOST" \
    -Dhttp.proxyPort="$HTTP_PROXYPORT" \
    -Dhttps.proxyHost="$HTTPS_PROXYHOST" \
    -Dhttps.proxyPort="$HTTPS_PROXYPORT" \
    -Dids.expose="$IDS_EXPOSE" \
    -Dids.generate="$IDS_GENERATE" \
    -Dinterlinking.delta.bucket="$INTERLINKING_DELTA_BUCKET" \
    -Dinterlinking.delta.enabled="$INTERLINKING_DELTA_ENABLED" \
    -Diris.password="$IRIS_PASSWORD" \
    -Diris.url="$IRIS_URL" \
    -Diris.user="$IRIS_USER" \
    -Ditunes.epf.username="$ITUNES_EPF_USERNAME" \
    -Ditunes.epf.password="$ITUNES_EPF_PASSWORD" \
    -Ditunes.epf.feedPath="$ITUNES_EPF_FEEDPATH" \
    -Ditv.whatson.schedule.url="$ITV_WHATSON_SCHEDULE_URL" \
    -Dkm.contentdeals.aws.accessKey="$KM_CONTENTDEALS_AWS_ACCESSKEY" \
    -Dkm.contentdeals.aws.secretKey="$KM_CONTENTDEALS_AWS_SECRETKEY" \
    -Dlakeview.feature.addXBoxOneAvailability="$LAKEVIEW_FEATURE_ADDXBOXONEAVAILABILITY" \
    -Dlakeview.feature.genericTitlesEnabled="$LAKEVIEW_FEATURE_GENERICTITLESENABLED" \
    -Dlakeview.upload.account="$LAKEVIEW_UPLOAD_ACCOUNT" \
    -Dlakeview.upload.container="$LAKEVIEW_UPLOAD_CONTAINER" \
    -Dlakeview.upload.enabled="$LAKEVIEW_UPLOAD_ENABLED" \
    -Dlakeview.upload.key="$LAKEVIEW_UPLOAD_KEY" \
    -Dlocal.host.name="$LOCAL_HOST_NAME" \
    -Dlovefilm.missingThreshold="$LOVEFILM_MISSINGTHRESHOLD" \
    -Dlovefilm.oauth.api.key="$LOVEFILM_OAUTH_API_KEY" \
    -Dlovefilm.oauth.api.secret="$LOVEFILM_OAUTH_API_SECRET" \
    -Dlovefilm.s3.access="$LOVEFILM_S3_ACCESS" \
    -Dlovefilm.s3.bucket="$LOVEFILM_S3_BUCKET" \
    -Dlovefilm.s3.fileName="$LOVEFILM_S3_FILENAME" \
    -Dlovefilm.s3.folder="$LOVEFILM_S3_FOLDER" \
    -Dlovefilm.s3.secret="$LOVEFILM_S3_SECRET" \
    -Dmessaging.broker.url="$MESSAGING_BROKER_URL" \
    -Dmessaging.destination.changes="$MESSAGING_DESTINATION_CHANGES" \
    -Dmessaging.destination.equiv.assert="$MESSAGING_DESTINATION_EQUIV_ASSERT" \
    -Dmessaging.destination.indexer="$MESSAGING_DESTINATION_INDEXER" \
    -Dmessaging.destination.logger="$MESSAGING_DESTINATION_LOGGER" \
    -Dmessaging.destination.replicator="$MESSAGING_DESTINATION_REPLICATOR" \
    -Dmessaging.enabled="$MESSAGING_ENABLED" \
    -Dmessaging.zookeeper="$MESSAGING_ZOOKEEPER" \
    -Dmetabroadcast.picks.priorityChannelGroup="$METABROADCAST_PICKS_PRIORITYCHANNELGROUP" \
    -Dmongo.audit.enabled="$MONGO_AUDIT_ENABLED" \
    -Dmongo.db.tag.fallback="$MONGO_DB_TAG_FALLBACK" \
    -Dmongo.db.tag="$MONGO_DB_TAG" \
    -Dmongo.dbName="$MONGO_DBNAME" \
    -Dmongo.host="$MONGO_HOST" \
    -Dmusicbrainz.dataDir="$MUSICBRAINZ_DATADIR" \
    -Dmy.very.secret.thing="$MY_VERY_SECRET_THING" \
    -Dnetflix.consumerKey="$NETFLIX_CONSUMERKEY" \
    -Dopta.events.http.credentials.rugby.password="$OPTA_EVENTS_HTTP_CREDENTIALS_RUGBY_PASSWORD" \
    -Dopta.events.http.credentials.rugby.username="$OPTA_EVENTS_HTTP_CREDENTIALS_RUGBY_USERNAME" \
    -Dopta.events.http.credentials.soccer.password="$OPTA_EVENTS_HTTP_CREDENTIALS_SOCCER_PASSWORD" \
    -Dopta.events.http.credentials.soccer.username="$OPTA_EVENTS_HTTP_CREDENTIALS_SOCCER_USERNAME" \
    -Dpa.content.updater.threads="$PA_CONTENT_UPDATER_THREADS" \
    -Dpa.film.feedUrl="$PA_FILM_FEEDURL" \
    -Dpa.ftp.host="$PA_FTP_HOST" \
    -Dpa.ftp.password="$PA_FTP_PASSWORD" \
    -Dpa.ftp.path="$PA_FTP_PATH" \
    -Dpa.ftp.username="$PA_FTP_USERNAME" \
    -Dpa.people.enabled="$PA_PEOPLE_ENABLED" \
    -Dplayer.4od.id="$PLAYER_4OD_ID" \
    -Dplayer.demand5.id="$PLAYER_DEMAND5_ID" \
    -Dplayer.iplayer.id="$PLAYER_IPLAYER_ID" \
    -Dplayer.itvplayer.id="$PLAYER_ITVPLAYER_ID" \
    -Dpreview.feedUrl="$PREVIEW_FEEDURL" \
    -Dprocessing.config="$PROCESSING_CONFIG" \
    -Dprocessing.mongo.writeConcern="$PROCESSING_MONGO_WRITECONCERN" \
    -Dredux.host="$REDUX_HOST" \
    -Dredux.password="$REDUX_PASSWORD" \
    -Dredux.username="$REDUX_USERNAME" \
    -Dstatus.client.host="$STATUS_CLIENT_HOST" \
    -Dstatus.client.port="$STATUS_CLIENT_PORT" \
    -Dtelescope.environment="$TELESCOPE_ENVIRONMENT" \
    -Dtelescope.host="$TELESCOPE_HOST" \
    -Dtelescope.coreReportingThreads="$TELESCOPE_CORE_REPORTING_THREADS" \
    -Dtelescope.maxReportingThreads="$TELESCOPE_MAX_REPORTING_THREADS" \
    -Dtelescope.queueSize="$TELESCOPE_QUEUE_SIZE" \
    -Dtelescope.reportingThreadName="$TELESCOPE_REPORTING_THREAD_NAME" \
    -Dtelescope.metricsPrefix="$TELESCOPE_METRICS_PREFIX" \
    -Drequest.threads="$REQUEST_THREADS" \
    -Drp.ftp.enabled="$RP_FTP_ENABLED" \
    -Drp.ftp.manualUpload.enabled="$RP_FTP_MANUALUPLOAD_ENABLED" \
    -Drp.ftp.services="$RP_FTP_SERVICES" \
    -Drp.health.password="$RP_HEALTH_PASSWORD" \
    -Drp.https.baseUrl="$RP_HTTPS_BASEURL" \
    -Drp.https.enabled="$RP_HTTPS_ENABLED" \
    -Drp.https.manualUpload.enabled="$RP_HTTPS_MANUALUPLOAD_ENABLED" \
    -Drp.https.password="$RP_HTTPS_PASSWORD" \
    -Drp.https.services="$RP_HTTPS_SERVICES" \
    -Drp.https.username="$RP_HTTPS_USERNAME" \
    -Drp.password.unique="$RP_PASSWORD_UNIQUE" \
    -Drp.s3.bucket="$RP_S3_BUCKET" \
    -Drp.s3.ftp.enabled="$RP_S3_FTP_ENABLED" \
    -Drp.s3.https.enabled="$RP_S3_HTTPS_ENABLED" \
    -Drp.upload.unique="$RP_UPLOAD_UNIQUE" \
    -Drte.feed.url="$RTE_FEED_URL" \
    -Ds3.access="$S3_ACCESS" \
    -Ds3.secret="$S3_SECRET" \
    -Dschedule.repopulator.bbc.scheduled="$SCHEDULE_REPOPULATOR_BBC_SCHEDULED" \
    -Dschedule.repopulator.c4.scheduled="$SCHEDULE_REPOPULATOR_C4_SCHEDULED" \
    -Dschedule.repopulator.full.scheduled="$SCHEDULE_REPOPULATOR_FULL_SCHEDULED" \
    -Dschedule.repopulator.redux.scheduled="$SCHEDULE_REPOPULATOR_REDUX_SCHEDULED" \
    -Dserver.port="$SERVER_PORT" \
    -Dservice.ios.id="$SERVICE_IOS_ID" \
    -Dservice.web.id="$SERVICE_WEB_ID" \
    -Dsitemaps.c4.brightcove.playerId="$SITEMAPS_C4_BRIGHTCOVE_PLAYERID" \
    -Dsitemaps.c4.brightcove.publisherId="$SITEMAPS_C4_BRIGHTCOVE_PUBLISHERID" \
    -Dsitemaps.c4.flashplayerversion.uri="$SITEMAPS_C4_FLASHPLAYERVERSION_URI" \
    -Dsitemaps.c4.flashplayerversion="$SITEMAPS_C4_FLASHPLAYERVERSION" \
    -Dtalktalk.host="$TALKTALK_HOST" \
    -Dtalktalk.validate="$TALKTALK_VALIDATE" \
    -Dthespace.url="$THESPACE_URL" \
    -Dtwitter.auth.consumerKey="$TWITTER_AUTH_CONSUMERKEY" \
    -Dtwitter.auth.consumerSecret="$TWITTER_AUTH_CONSUMERSECRET" \
    -Dunbox.remote.s3.access="$UNBOX_REMOTE_S3_ACCESS" \
    -Dunbox.remote.s3.bucket="$UNBOX_REMOTE_S3_BUCKET" \
    -Dunbox.remote.s3.fileName="$UNBOX_REMOTE_S3_FILENAME" \
    -Dunbox.remote.s3.secret="$UNBOX_REMOTE_S3_SECRET" \
    -Dunbox.s3.bucket="$UNBOX_S3_BUCKET" \
    -Dunbox.url="$UNBOX_URL" \
    -Dupdaters.bbc-audience-data.enabled="$UPDATERS_BBC_AUDIENCE_DATA_ENABLED" \
    -Dupdaters.bbc-products.enabled="$UPDATERS_BBC_PRODUCTS_ENABLED" \
    -Dupdaters.bbc.enabled="$UPDATERS_BBC_ENABLED" \
    -Dupdaters.bbcnitro.enabled="$UPDATERS_BBCNITRO_ENABLED" \
    -Dupdaters.bbcnitro.offschedule.enabled="$UPDATERS_BBCNITRO_OFFSCHEDULE_ENABLED" \
    -Dupdaters.bt-channels.enabled="$UPDATERS_BT_CHANNELS_ENABLED" \
    -Dupdaters.bt-events.enabled="$UPDATERS_BT_EVENTS_ENABLED" \
    -Dupdaters.bt.enabled="$UPDATERS_BT_ENABLED" \
    -Dupdaters.btfeatured.enabled="$UPDATERS_BTFEATURED_ENABLED" \
    -Dupdaters.btvod.enabled="$UPDATERS_BTVOD_ENABLED" \
    -Dupdaters.c4.enabled="$UPDATERS_C4_ENABLED" \
    -Dupdaters.c4pmlsd.enabled="$UPDATERS_C4PMLSD_ENABLED" \
    -Dupdaters.five.enabled="$UPDATERS_FIVE_ENABLED" \
    -Dupdaters.getty.enabled="$UPDATERS_GETTY_ENABLED" \
    -Dupdaters.hbo.enabled="$UPDATERS_HBO_ENABLED" \
    -Dupdaters.hulu.enabled="$UPDATERS_HULU_ENABLED" \
    -Dupdaters.itunes.enabled="$UPDATERS_ITUNES_ENABLED" \
    -Dupdaters.itvinterlinking.enabled="$UPDATERS_ITVINTERLINKING_ENABLED" \
    -Dupdaters.itvwhatson.enabled="$UPDATERS_ITVWHATSON_ENABLED" \
    -Dupdaters.knowledgemotion.enabled="$UPDATERS_KNOWLEDGEMOTION_ENABLED" \
    -Dupdaters.lakeview.enabled="$UPDATERS_LAKEVIEW_ENABLED" \
    -Dupdaters.lovefilm.enabled="$UPDATERS_LOVEFILM_ENABLED" \
    -Dupdaters.metabroadcast.enabled="$UPDATERS_METABROADCAST_ENABLED" \
    -Dupdaters.metabroadcastpicks.enabled="$UPDATERS_METABROADCASTPICKS_ENABLED" \
    -Dupdaters.opta-events.enabled="$UPDATERS_OPTA_EVENTS_ENABLED" \
    -Dupdaters.pa.enabled="$UPDATERS_PA_ENABLED" \
    -Dupdaters.redux.enabled="$UPDATERS_REDUX_ENABLED" \
    -Dupdaters.rovi.enabled="$UPDATERS_ROVI_ENABLED" \
    -Dupdaters.rte.enabled="$UPDATERS_RTE_ENABLED" \
    -Dupdaters.similarcontent.enabled="$UPDATERS_SIMILARCONTENT_ENABLED" \
    -Dupdaters.talktalk.enabled="$UPDATERS_TALKTALK_ENABLED" \
    -Dupdaters.unbox.enabled="$UPDATERS_UNBOX_ENABLED" \
    -Dupdaters.wikipedia.films.enabled="$UPDATERS_WIKIPEDIA_FILMS_ENABLED" \
    -Dupdaters.wikipedia.football.enabled="$UPDATERS_WIKIPEDIA_FOOTBALL_ENABLED" \
    -Dupdaters.wikipedia.people.enabled="$UPDATERS_WIKIPEDIA_PEOPLE_ENABLED" \
    -Dupdaters.wikipedia.tv.enabled="$UPDATERS_WIKIPEDIA_TV_ENABLED" \
    -Dupdaters.youview.enabled="$UPDATERS_YOUVIEW_ENABLED" \
    -Dwatermark.channelUris="$WATERMARK_CHANNELURIS" \
    -Dxmltv.upload.bucket="$XMLTV_UPLOAD_BUCKET" \
    -Dxmltv.upload.enabled="$XMLTV_UPLOAD_ENABLED" \
    -Dxmltv.upload.folder="$XMLTV_UPLOAD_FOLDER" \
    -Dyouview.prod.url="$YOUVIEW_PROD_URL" \
    -Dyouview.stage.url="$YOUVIEW_STAGE_URL" \
    -Dyouview.timeout.seconds="$YOUVIEW_TIMEOUT_SECONDS" \
    -Dyouview.upload.nitro.password="$YOUVIEW_UPLOAD_NITRO_PASSWORD" \
    -Dyouview.upload.nitro.upload.enabled="$YOUVIEW_UPLOAD_NITRO_UPLOAD_ENABLED" \
    -Dyouview.upload.nitro.url="$YOUVIEW_UPLOAD_NITRO_URL" \
    -Dyouview.upload.nitro.username="$YOUVIEW_UPLOAD_NITRO_USERNAME" \
    -Dyouview.upload.password="$YOUVIEW_UPLOAD_PASSWORD" \
    -Dyouview.upload.username="$YOUVIEW_UPLOAD_USERNAME" \
    -Dmetrics.graphite.enabled="$METRICS_GRAPHITE_ENABLED" \
    -Djsse.enableSNIExtension="$JSSE_ENABLESNIEXTENSION" \
    -Dlog4j.configuration="$LOG4J_CONFIGURATION" \
    -Dnimrod.log.level="$NIMROD_LOG_LEVEL" \
    -Droot.log.level="$ROOT_LOG_LEVEL" \
    -Xmx$JVM_MEMORY \
    -Xms$JVM_MEMORY \
    $DEBUG_AND_REMOTE_OPTS \
    $JVM_OPTS \
    -jar atlas.war
