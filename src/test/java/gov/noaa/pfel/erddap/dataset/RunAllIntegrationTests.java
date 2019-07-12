package gov.noaa.pfel.erddap.dataset;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        EDDTableFromAxiomStationTest.class,
        EDDTableFromAxiomStationV2Test.class,
        EDDTableFromAxiomStationV1Test.class
})
public class RunAllIntegrationTests {
}
