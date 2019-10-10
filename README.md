# ERDDAP - Development Codebase

This is an alternative codebase of the [ERDDAP project](https://github.com/BobSimons/erddap) optimized for software development. If you have questions, issues or comments about the ERDDAP codebase in general, please use the official repository. If you have questions or commments about an ERDDAP installation please see the excellent [ERDDAP documentation](https://coastwatch.pfeg.noaa.gov/erddap/download/setup.html) and/or the [ERDDAP forum](https://groups.google.com/forum/#!forum/erddap).


## Changes to ERDDAP

This aims to be a complete drop-in replacement for ERDDAP and we ([Axiom Data Science](https://axiomdatascience.com)) have been running this in production since 2017. The only difference is the ERDDAP version will appear different on your installation in the bottom left of the ERDDAP website. If you run the `2.02` version of ERDDAP, instead of the typical `ERDDAP, Version 2.02` you will see `ERDDAP, Version 2.02_axiom-r1` where `r1` is a release from this codebase of a specific version fo ERDDAP.

If you are not creating your own custom ERDDAP access classes or debugging the ERDDAP codebase then this repository probably isn't for you!

This repository contains some extra dataset access classes. If you are developing a dataset access class and would like it included in this repository please submit a PR! We are happy to be a holding place until your class can be flushed out, tested, and integrated upstream into the ERDDAP codebase.
 
* `src/main/java/gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomSensorCSVService.java`
* `src/main/java/gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomStation.java`
* `src/main/java/gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomStationV2.java`


## Developer Setup

Instructions are intended for for IntelliJ IDEA unless otherwise noted.

1. Create the two directories required by ERDDAP:
    ```
    mkdir -p /data/erddap/content
    mkdir -p /data/erddap/data
    ```

2. Clone this codebase
    ```
    git clone https://github.com/axiom-data-science/erddap.git
    ```

3. Import the project into IntelliJ as a `Maven Project`
   * Note: you will get compile errors because there are some files in this project that should be excluded (see the pom.xml). In the Build Messages window, right-click files with errors and choose `Exclude from Compile`. See the `pom.xml` file for the list of those files. Lately ERDDAP has been renaming these files to an extension other than `.java`, which helps but doesn't solve the issue.

4. Install the [DejaVu fonts](https://coastwatch.pfeg.noaa.gov/erddap/download/setup.html#fonts)

5. Setup the default ERDDAP content using `erddapContent.zip` (see http://coastwatch.pfeg.noaa.gov/erddap/download/setup.html).  Copy files into `/data/erddap/content`.

6. Edit `/data/erddap/content/erddap/setup.xml` using `setup.example.xml` as a basis.


## Testing

To test this package, you can run JUnit tests in `src/test/java`.

### Unit Tests

Unit tests can just be run like normal (within your IDE).

### Integration Tests

Integration tests are a bit of a pain. Before running integration tests, make sure you have the project set up as explained above. If you have a better solution to this please advise!
 
1. In `pom.xml`, remove the line `<scope>provided</scope>` under `<groupId>javax.servlet</groupId>`, and the line `<scope>test</scope>` under `<groupId>junit</groupId>`.

2. Run `./run-integration-tests.sh` in a CLI in the root of this codebase

Or you can run (and debug!) in Intellij by going to Run > Edit Configurations, clicking the plus sign to Add New > Application, and filling in all the details. See screenshot:

![intellij-junit-setup.png](intellij-junit-setup.png)

### Run the `main` methods

To test this package, there is a main() method, so you can just run the file like so (replacing paths as needed):

Remove the line `<scope>provided</scope>` from the `pom.xml` under `<groupId>javax.servlet</groupId>`.

Examples of running specific class `main` methods for testing:

```bash
# Template
mvn compile war:exploded && \
pushd . && \
cd target/erddap-[version]]/WEB-INF/ && \
java -DerddapContentDirectory=/data/erddap/content -classpath "./classes:./lib/*" -Xmx1200M -Xms1200M [path_to_class_with_main_method]; \
popd


# EDDTableFromAxiomSensorCSVService
mvn compile war:exploded && \
pushd . && \
cd target/erddap-2.02_axiom-r1/WEB-INF/ && \
java -DerddapContentDirectory=/data/erddap/content -classpath "./classes:./lib/*" -Xmx1200M -Xms1200M gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomSensorCSVService; \
popd

# EDDTableFromAxiomStation
mvn compile war:exploded && \
pushd . && \
cd target/erddap-2.02_axiom-r1/WEB-INF/ && \
java -DerddapContentDirectory=/data/erddap/content -classpath "./classes:./lib/*" -Xmx1200M -Xms1200M gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomStation; \
popd
```

## Building and Running

First, make sure you have the project set up as explained above

1. Build the project
    ```
    docker build -t erddap-axiom .
    ```

2. Run the project
    ```bash
    docker run --name erddap-axiom \
        -p 8080:8080 \
          -v "/data/erddap/content:/usr/local/tomcat/content/erddap" \
          -v "/data/erddap/data:/data/erddap/data" \
      erddap-axiom
    ```

3. Watch the logs and w for it to start up

4. Visit http://localhost:8080/erddap/index.html


## Syncing with upstream ERDDAP

After an ERDDAP release we will make our best effort to sync this repository with the changes. ERDDAP's main codebase isn't updated often and is usually updated all at once for a specific version, so we can't really plan for large and breaking changes. The process we go through to sync the codebases is outlined below for reference.

1. Read ERDDAP release notes carefully. They are good.

2. Get an adult beverage

3. Clone the upstream ERDDAP
    ```
    git clone https://github.com/BobSimons/erddap.git
    cd erddap
    export ERDDAP_UPSTREAM_ROOT=$(pwd)
    ```

4. Look at changes to the `lib/*.jar` directory in ERDDAP since the last release (`git log [previous_version] [new_version]`) and unzip and inspect the `MANIFEST` for each jar that changed. Inside that `MANIFEST` is a version number. Update the `pom.xml` with the new version number. Any new `.jar` files will need a new library record in `pom.xml`.

5. Clone this repo in a different directory
    ```
    cd ..
    git clone https://github.com/axiom-data-science/erddap.git
    cd erddap
    export ERDDAP_DEV_ROOT=$(pwd)
    ```

6. Make a new branch for this repo
    ```
    git checkout -b [new_version]
    ```

7. Edit the `move_changed.py` script

    Change the `previous_version` and `new_version` to either `git` tags or commit hashes you want to migrate from. Usually `new_version` would be `HEAD` and you would change `previous_version` to the release tag you are migrating this repository from.

    For example, when 2.03 is released, one might do this:

    ```python
    previous_version = '2.02'
    new_version = 'HEAD'
    ```

8. Run the python sync script (required python >= 3.5).
    ```
    python move_changed.py
    ```

9. Read the output of `move_changed.py` and **do what it says**!

10. Add back in to `EDD.java` any custom dataset classes this repository contains:
    ```java
    ...
    if (type.equals("EDDTableFromAxiomSensorCSVService")) return EDDTableFromAxiomSensorCSVService.fromXml(erddap, xmlReader);
    if (type.equals("EDDTableFromAxiomStation")) return EDDTableFromAxiomStation.fromXml(erddap, xmlReader);
    if (type.equals("EDDTableFromAxiomStationV1")) return EDDTableFromAxiomStationV1.fromXml(erddap, xmlReader);
    if (type.equals("EDDTableFromAxiomStationV2")) return EDDTableFromAxiomStationV2.fromXml(erddap, xmlReader);
    ...
    ```

11. Copy over the new `erddapContent.zip` from [here](http://coastwatch.pfeg.noaa.gov/erddap/download/setup.html).

12. Make any changes required to get ERDDAP to compile. This used to take me an entire day, but now it's down to an hour or two.

13. Run tests as needed

14. Change the version of ERDDAP in:
    * `pom.xml`
    * `Dockerfile`
    * `README.md`
    * `src/main/java/gov/noaa/pfel/erddap/util/EDStatic.java`

15. Send a Pull Request for the new version!

