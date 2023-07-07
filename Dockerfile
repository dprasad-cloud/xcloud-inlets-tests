FROM adoptopenjdk/openjdk8

ENV APP_NAME=xcloud-inlets-tests
ENV APP_EXT=jar
ENV APP_HOME=/opt/${APP_NAME}
ENV APP_FILE=${APP_HOME}/${APP_NAME}.${APP_EXT}
ENV CONFIG_URI=/aerohive_app/etc/application.properties

ADD entrypoint.sh /usr/local/bin/

RUN chmod 755 /usr/local/bin/entrypoint.sh && \
    mkdir $APP_HOME

COPY target/xcloud-inlets-tests-1.0-SNAPSHOT.jar ${APP_FILE}

EXPOSE 8080 2552 50091

CMD ["/usr/local/bin/entrypoint.sh"]
