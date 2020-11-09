#!/bin/bash

mvn test-compile war:exploded
pushd .
cd target/erddap-2.10_axiom-r1/WEB-INF/
java -DerddapContentDirectory=/data/erddap/content -classpath "./classes:../../test-classes:./lib/*" -Xmx1200M -Xms1200M org.junit.runner.JUnitCore gov.noaa.pfel.erddap.dataset.RunAllIntegrationTests
result=$?
popd

exit $result

