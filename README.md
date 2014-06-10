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
    <!-- Region is taken from the "appregion" parameter here: http://docs.stationsensorservice.apiary.io/#getstationrequest
    <region>all</region>
    <addAttributes>
        <att name="title">Axiom Sensor Service for ALL regions</att>
    </addAttributes>
</dataset>
```

**Axiom specific code is in `src/main/java/gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomSensorCSVService.java`**

