FROM axiom/docker-tomcat:8.0
MAINTAINER Kyle Wilcox <kyle@axiomdatascience.com>

RUN \
    apt-get update && \
    apt-get install -y unzip && \
    rm -rf /var/lib/apt/lists/*

# Install BitstreamVeraSans font
ENV ERDDAP_FONTS_URL https://www.dropbox.com/s/utr99izlrzudc5g/BitstreamVeraSans.zip
RUN \
    curl -fSL "$ERDDAP_FONTS_URL" -o BitstreamVeraSans.zip && \
    unzip BitstreamVeraSans.zip -d /usr/lib/jvm/java-8-oracle/jre/lib/fonts/

# Install ERDDAP content zip
ENV ERDDAP_CONTENT_URL https://www.dropbox.com/s/qxzlpfv8logcgso/erddapContent.zip
RUN \
    curl -fSL "$ERDDAP_CONTENT_URL" -o erddapContent.zip && \
    unzip erddapContent.zip -d $CATALINA_HOME

# Install Maven
ENV MAVEN_VERSION 3.3.9
RUN curl -fSL http://apache.mirrors.pair.com/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip -o mvn.zip && \
    unzip mvn.zip -d /mvn

# Install dependencies
COPY pom.xml /pom.xml
RUN cd / && \
    /mvn/apache-maven-$MAVEN_VERSION/bin/mvn dependency:resolve

# Install ERDDAP WAR
COPY . /app
RUN cd /app && \
    /mvn/apache-maven-$MAVEN_VERSION/bin/mvn clean package && \
    cp /app/target/*.war $CATALINA_HOME/webapps/erddap.war

# Move over custom Tocmat config
COPY docker/javaopts.sh $CATALINA_HOME/bin/javaopts.sh
# Move over custom ERDDAP config
COPY docker/setup.xml $CATALINA_HOME/content/erddap/setup.xml

ENV ERDDAP_DATA /erddapData
RUN \
    mkdir -p $ERDDAP_DATA && \
    chown -R tomcat:tomcat "$ERDDAP_DATA" && \
    chown -R tomcat:tomcat "$CATALINA_HOME"

COPY docker/entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]

EXPOSE 8080 8443
CMD ["catalina.sh", "run"]
