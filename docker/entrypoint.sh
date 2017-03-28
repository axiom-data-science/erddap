#!/bin/bash
set -e

# preferable to fire up Tomcat via start-tomcat.sh which will start Tomcat with
# security manager, but inheriting containers can also start Tomcat via
# catalina.sh

if [ "$1" = 'start-tomcat.sh' ] || [ "$1" = 'catalina.sh' ]; then

    ###
    # Change CATALINA_HOME ownership to tomcat user and tomcat group
    # Restrict permissions on conf
    ###

    chmod 400 ${CATALINA_HOME}/conf/*
    mkdir -p ${CATALINA_HOME}/conf/Catalina/localhost && chmod 600 ${CATALINA_HOME}/conf/Catalina/localhost
    chown -R tomcat:tomcat ${CATALINA_HOME}
    chown -R tomcat:tomcat /erddapData
    sync
    exec gosu tomcat "$@"
fi

exec "$@"
