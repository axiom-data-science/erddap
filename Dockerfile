FROM axiom/docker-erddap:2.02
MAINTAINER Kyle Wilcox <kyle@axiomdatascience.com>

ENV MAVEN_VERSION 3.3.9
ENV JDK_HOME /usr/lib/jvm/java-8-openjdk-amd64

# Install JDK and Maven
RUN \
  apt-get update && \
  apt-get install -y openjdk-8-jdk && \
  apt-get clean && \
  rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
  curl -fSL http://apache.mirrors.pair.com/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip -o mvn.zip && \
  unzip mvn.zip -d /mvn && \
  rm mvn.zip

# Install dependencies
COPY pom.xml /pom.xml
RUN cd / && \
    JAVA_HOME=${JDK_HOME} /mvn/apache-maven-$MAVEN_VERSION/bin/mvn package dependency:go-offline --fail-never --also-make

# Install ERDDAP WAR
ENV AXIOM_ERDDAP_VERSION 2.02_axiom-r1
COPY . /app
RUN cd /app && \
    JAVA_HOME=${JDK_HOME} /mvn/apache-maven-$MAVEN_VERSION/bin/mvn clean compile war:war && \
    rm -rf ${CATALINA_HOME}/webapps/erddap && \
    unzip target/erddap-${AXIOM_ERDDAP_VERSION}.war -d ${CATALINA_HOME}/webapps/erddap/ && \
    cd ${CATALINA_HOME} && \
    rm -rf /app

# Add big files
ADD https://github.com/BobSimons/erddap/raw/master/WEB-INF/ref/etopo1_ice_g_i2.bin ${CATALINA_HOME}/webapps/erddap/WEB-INF/ref/

EXPOSE 8080
CMD ["catalina.sh", "run"]
