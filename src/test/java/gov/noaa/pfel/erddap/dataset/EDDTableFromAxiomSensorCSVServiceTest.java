package gov.noaa.pfel.erddap.dataset;


import com.cohort.array.ByteArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.Ignore;
import org.junit.Test;

import static gov.noaa.pfel.erddap.dataset.EDD.testVerboseOn;
import static org.junit.Assert.assertEquals;

public class EDDTableFromAxiomSensorCSVServiceTest {

    @Test
    public void testTestsAreRunning() {
        assertEquals(4, 2+2);
    }

    @Test
    public void test() throws Throwable {
        String2.log("\n****************** EDDTableFromAxiomSensorCSVService.test() *****************\n");
        testVerboseOn();

        EDD edd = EDD.oneFromXmlFragment(null, "" +
                "<dataset type=\"EDDTableFromAxiomSensorCSVService\" datasetID=\"sensor_service\">\n" +
                "    <region>eastcoast</region>\n" +
                "    <sourceUrl>http://sensors.axds.co/stationsensorservice/</sourceUrl>\n" +
                "</dataset>"
        );
        // Test specific station and sensor
        String query = "&station=%22urn:ioos:station:gov.noaa.nws:CYAW%22&parameter=%22Air Temperature%22&time>=2017-03-01T00:00:00Z&time<=2017-04-01T00:00:00Z";
        String tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory,
                edd.className() + "_station_sensor_" + edd.datasetID(), ".csv");
        String results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        // lat/lon bounds with no stations or sensor
        // This will need to use the subset file to figure out which stations are within the bbox
        // and then request data for each station/sensor combo
        //        query = "&longitude>=-130&longitude<=74&latitude>=36.751&latitude<=36.752";
        //        tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory,
        //                    edd.className() + "_lat_lon_" + edd.datasetID(), ".csv");
        //        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //        String2.log(results);

        // Specific a specific station and sensor with time bounds
        query = "&station=%22urn:ioos:station:gov.noaa.nws:CYAW%22&time%3Enow-7days";
        tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory,
                edd.className() + "_station_sensor_time_" + edd.datasetID(), ".csv");
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        // lat/lon bounds and time bounds
        //        query = "&longitude>=-130&longitude<=74&latitude>=36.751&latitude<=36.752&time>=2014-04-01T00:00:00Z&time<=2014-06-01T00:00:00Z";
        //        tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory,
        //                    edd.className() + "_lat_lon_time_" + edd.datasetID(), ".csv");
        //        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //        String2.log(results);;
    }
}
