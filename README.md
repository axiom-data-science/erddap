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
4. Run Tests
    * Run the `compile war:exploded` configuration
    * Change directories into the produced exploded WAR

        ```bash
        cd {BASE_SOURCE_DIR}/target/erddap-*/WEB-INF/
        ```
    * Run test file

        ```bash
        {path_to_java_that_compiled_erddap} -DerddapContentDirectory=/data/erddap/content -classpath "./classes:./lib/*:{your_path_to_tomcat_directory}/lib/servlet-api.jar" -Xmx1200M -Xms1200M gov/noaa/pfel/coastwatch/TestAll
        ```
5. Package WAR - Run the `package` Run Configuration


**To run a local ERDDAP instance, use the `tomcat7:run-war` Run Configurfation.  It uses a built-in Tomcat 7.**

**Axiom specific code is in `src/main/java/gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomSensorCSVService.java`**
