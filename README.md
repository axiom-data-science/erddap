# ERDDAP - Axiom Style

### Development

1. Create the two directories required by ERDDAP:
    ```
    mkdir -p /data/erddap/content
    mkdir -p /data/erddap/data
    ```

2. Import project into IntelliJ as a `Maven Project`
3. Install BitStreamVera fonts (see http://coastwatch.pfeg.noaa.gov/erddap/download/setup.html)
5. Setup the default ERDDAP content using erddapContent.zip (see http://coastwatch.pfeg.noaa.gov/erddap/download/setup.html).  Copy files into `/data/erddap/content`.
6. Edit the setup.xml file.
    ```xml
    <bigParentDirectory>/data/erddap/data/</bigParentDirectory>
    ```

Axiom specific code is in:
 
* `src/main/java/gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomSensorCSVService.java`
* `src/main/java/gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomStation.java`

To test this package, there is a main() method, so you can just run the file like so (replacing paths as needed):

Remove the line `<scope>provided</scope>` from the `pom.xml` under `<groupId>javax.servlet</groupId>`.

You'll need to make changes to your `setup.xml` in `/data/erddap/content` until ERDDAP is happy. Yeah.

```bash
# All stations
$ mvn compile war:exploded && pushd . && cd target/erddap-1.80-axiom-r2/WEB-INF/ && java -DerddapContentDirectory=/data/erddap/content -classpath "./classes:./lib/*:/opt/tomcat/apache-tomcat-8.0.18/lib/servlet-api.jar" -Xmx1200M -Xms1200M gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomSensorCSVService; popd

# Individual stations
$ mvn compile war:exploded && pushd . && cd target/erddap-1.80-axiom-r2/WEB-INF/ && java -DerddapContentDirectory=/data/erddap/content -classpath "./classes:./lib/*:/opt/tomcat/apache-tomcat-8.0.18/lib/servlet-api.jar" -Xmx1200M -Xms1200M gov/noaa/pfel/erddap/dataset/EDDTableFromAxiomStation; popd
```


### Syncing upstream

ugh. Ugh. UGH.

There is really no easy way to do this.  ERRDAP developer(s) do not include commit messages
in Git and usally put an entire release into a single commit.  That being said... as long as none of the external libraries change or additional ones are needed, you should try:

```
$ git clone https://github.com/BobSimons/erddap.git
$ cd erddap
$ export ERDDAP_UPSTREAM_ROOT=$(pwd)

$ cd ..
$ git clone git@git.axiom:axiom/erddap.git
$ cd erddap
$ export ERDDAP_DEV_ROOT=$(pwd)

# Using Python >= 3.5
(python35)$ python move_changed.py

# Do what the script outputs!
```

Now you will need to make a ton of changes to the source to get it to compile.  Pretty much a nightmare, but doable in a few hours.


Add back in to EDD.java:

```java
if (type.equals("EDDTableFromAxiomSensorCSVService")) return EDDTableFromAxiomSensorCSVService.fromXml(erddap, xmlReader);
if (type.equals("EDDTableFromAxiomStation")) return EDDTableFromAxiomStation.fromXml(erddap, xmlReader);
```

