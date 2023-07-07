#!/bin/bash

LOGTYPE=xcloud-inlets-tests
LOGOWNER=root
ORIGINLOGDIR=/opt/xcloud-inlets-tests/logs

linkLogDir(){
    HOSTNAME=`hostname`

    LOGDIR="/data/log/${LOGTYPE}/$HOSTNAME"
    mkdir -p $LOGDIR
    chown -R $LOGOWNER $LOGDIR
    rm -rf $ORIGINLOGDIR
    ln -s $LOGDIR $ORIGINLOGDIR
}

linkLogDir

java -Dlog4j2.formatMsgNoLookups=true -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9001 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=1024m -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -XX:MaxTenuringThreshold=2 -XX:+ExplicitGCInvokesConcurrent -XX:+ParallelRefProcEnabled -XX:AutoBoxCacheMax=20000 -XX:-UseBiasedLocking -XX:+PerfDisableSharedMem -Djava.security.egd=file:/dev/./urandom -XX:-OmitStackTraceInFastThrow -XX:+PrintCommandLineFlags -Dsun.reflect.inflationThreshold=0 ${JVM_SIZE} -jar ${APP_FILE} --spring.config.location=classpath:/application.properties,file://${CONFIG_URI} --server.port=8080
