FROM axiom/docker-erddap:1.74
MAINTAINER Kyle Wilcox <kyle@axiomdatascience.com>

ENV MAVEN_VERSION 3.3.9
ENV JDK_HOME /usr/lib/jvm/java-8-oracle
RUN \
  # Install JDK
  echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu xenial main\ndeb-src http://ppa.launchpad.net/webupd8team/java/ubuntu xenial main" > /etc/apt/sources.list.d/webupd8team-java.list && \
  apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886 && \
  apt-get update && \
  echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections && \
  apt-get install -y \
    oracle-java8-installer \
    && \
  apt-get clean && \
  rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
  # Install Maven
  curl -fSL http://apache.mirrors.pair.com/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip -o mvn.zip && \
  unzip mvn.zip -d /mvn && \
  rm mvn.zip

# Install dependencies
COPY pom.xml /pom.xml
RUN cd / && \
    JAVA_HOME=${JDK_HOME} /mvn/apache-maven-$MAVEN_VERSION/bin/mvn dependency:resolve

# Install ERDDAP WAR
ENV AXIOM_ERDDAP_VERSION 1.74-axiom-r3
COPY . /app
RUN cd /app && \
    JAVA_HOME=${JDK_HOME} /mvn/apache-maven-$MAVEN_VERSION/bin/mvn clean compile war:war && \
    rm -rf ${CATALINA_HOME}/webapps/erddap && \
    unzip target/erddap-${AXIOM_ERDDAP_VERSION}.war -d ${CATALINA_HOME}/webapps/erddap/ && \
    cd ${CATALINA_HOME} && \
    rm -rf /app

COPY docker/entrypoint.sh /
ENTRYPOINT ["/entrypoint.sh"]

EXPOSE 8080
CMD ["catalina.sh", "run"]
