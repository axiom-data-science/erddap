# ERDDAP - Axiom Style

### Development

1. Create the two directories required by ERDDAP:
    ```
    mkdir -p /data/erddap/content
    mkdir -p /data/erddap/data
    ```

2. Import project into Eclipse as a `Maven Project`
3. Setup Run Configurations:
    * `tomcat7:run-war` - Parameters: `erddapContentDirectory = /data/erddap/content`
    * `compile war:exploded` - Parameters: `erddapContentDirectory = /data/erddap/content`
    * `package` - Parameters: `None`
4. Install BitStreamVera fonts (see http://coastwatch.pfeg.noaa.gov/erddap/download/setup.html)
5. Setup the default ERDDAP content using erddapContent.zip (see http://coastwatch.pfeg.noaa.gov/erddap/download/setup.html).  Copy files into `/data/erddap/content`.
6. Edit the setup.xml file.
    ```xml
    <bigParentDirectory>/data/erddap/data/</bigParentDirectory>
    ```

7. Run Tests
    * Run the `compile war:exploded` configuration
    * Change directories into the produced exploded WAR

        ```bash
        cd {BASE_SOURCE_DIR}/target/erddap-*/WEB-INF/
        ```
    * Run test file

        ```bash
        {path_to_java_that_compiled_erddap} -DerddapContentDirectory=/data/erddap/content -classpath "./classes:./lib/*:{your_path_to_tomcat_directory}/lib/servlet-api.jar" -Xmx1200M -Xms1200M gov/noaa/pfel/coastwatch/TestAll
        ```
8. Package WAR - Run the `package` Run Configuration


**To run a local ERDDAP instance, use the `tomcat7:run-war` Run Configurfation.  It uses a built-in Tomcat 7.**

To add the Axiom sensor service as an ERDDAP dataset, add this to your datasets.xml file:
```xml
<dataset type="EDDTableFromAxiomSensorCSVService" datasetID="axiom_sensor_service">
    <sourceUrl>http://pdx.axiomalaska.com/stationsensorservice/</sourceUrl>
    <!-- Region is taken from the "appregion" parameter here: http://docs.stationsensorservice.apiary.io/#getstationrequest -->
    <region>all</region>
    <addAttributes>
        <att name="title">Axiom Sensor Service for ALL regions</att>
    </addAttributes>
</dataset>
```

**Axiom specific code is in `src/main/java/gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomSensorCSVService.java`**

To test this package, there is a main() method, so you can just run the file like so (replacing paths as needed):

```bash
# All stations
$ mvn compile war:exploded && pushd . && cd target/erddap-1.62-axiom-r4/WEB-INF/ && java -DerddapContentDirectory=/data/erddap/content -classpath "./classes:./lib/*:/opt/tomcat/apache-tomcat-8.0.18/lib/servlet-api.jar" -Xmx1200M -Xms1200M gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomSensorCSVService; popd

# Individual stations
$ mvn compile war:exploded && pushd . && cd target/erddap-1.62-axiom-r4/WEB-INF/ && java -DerddapContentDirectory=/data/erddap/content -classpath "./classes:./lib/*:/opt/tomcat/apache-tomcat-8.0.18/lib/servlet-api.jar" -Xmx1200M -Xms1200M gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomStation; popd
```


### Syncing upstream

ugh. Ugh. UGH.

There is really no easy way to do this.  ERRDAP developer(s) do not include commit messages
in Git and usally put an entire release into a single commit.  That being said... as long as none of the external libraries change or additional ones are needed, you should try:

```
git clone https://github.com/BobSimons/erddap.git
cd erddap
export ERDDAP_DEV_ROOT=you_dev_root
cp -r WEB-INF/classes/gov/* $ERDDAP_DEV_ROOT/src/main/java/gov/
cp -r WEB-INF/classes/dods/* $ERDDAP_DEV_ROOT/src/main/java/dods/
cp -r WEB-INF/classes/com/cohort/* $ERDDAP_DEV_ROOT/src/main/java/com/cohort/
cp -r WEB-INF/classes/net/jmge/gif/* $ERDDAP_DEV_ROOT/src/main/java/net/jmge/gif/
cp -r WEB-INF/cptfiles/* $ERDDAP_DEV_ROOT/src/main/webapp/WEB-INF/cptfiles/
cp -r WEB-INF/ref/* $ERDDAP_DEV_ROOT/src/main/webapp/WEB-INF/ref/
cp -r WEB-INF/images/* $ERDDAP_DEV_ROOT/src/main/webapp/images/

```

Now you will need to make a ton of changes to the source to get it to compile.  Pretty much a nightmare, but doable in a few hours.


Add back in to EDD.java:

```java
if (type.equals("EDDTableFromAxiomSensorCSVService")) return EDDTableFromAxiomSensorCSVService.fromXml(xmlReader);
```

