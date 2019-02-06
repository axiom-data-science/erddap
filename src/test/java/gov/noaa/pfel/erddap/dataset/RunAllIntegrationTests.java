package gov.noaa.pfel.erddap.dataset;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        EDDTableFromAxiomSensorCSVServiceTest.class,
        EDDTableFromAxiomStationTest.class,
        EDDTableFromAxiomStationV2Test.class
})
public class RunAllIntegrationTests {
}
