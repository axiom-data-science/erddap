package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.ByteArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.Test;

import static gov.noaa.pfel.erddap.dataset.EDD.testVerboseOn;
import static org.junit.Assert.assertEquals;

public class EDDTableFromAxiomStationTest {

    @Test
    public void testTestsAreRunning() {
        assertEquals(4, 2+2);
    }

    @Test
    public void test() throws Throwable {
        String2.log("\n****************** EDDTableFromAxiomStation.test() *****************\n");
        testVerboseOn();

        EDD edd = EDD.oneFromXmlFragment(null, "" +
                "<dataset type=\"EDDTableFromAxiomStation\" datasetID=\"cencoos_humboldt\">\n" +
                "    <sourceUrl>http://sensors.axds.co/stationsensorservice/</sourceUrl>\n" +
                "    <stationId>20363</stationId>\n" +
                "</dataset>"
        );
        // Test specific station and sensor
        String query = "sea_water_temperature,time,latitude,longitude&time>=2016-05-14T00:00:00Z&time<2016-05-16T00:00:00Z";
//        String query = "sea_water_temperature,sea_water_pressure,time,latitude,longitude,depth&time>=2016-05-14T00:00:00Z&time<2016-05-16T00:00:00Z";
        String tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory,
                edd.className() + "_station_sensor_" + edd.datasetID(), ".csv");
        String results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        // Test for missing value masking
        edd = EDD.oneFromXmlFragment(null, "" +
                "<dataset type=\"EDDTableFromAxiomStation\" datasetID=\"wmo_46023\">\n" +
                "    <sourceUrl>http://sensors.axds.co/stationsensorservice/</sourceUrl>\n" +
                "    <stationId>20595</stationId>\n" +
                "</dataset>"
        );
        query = "sea_surface_wave_significant_height,time,latitude,longitude&time>=2010-09-02T00:00:00Z&time<2010-09-03T00:00:00Z";
        tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory,
                edd.className() + "_station_sensor_" + edd.datasetID(), ".csv");
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        // Test station with a cell method
        edd = EDD.oneFromXmlFragment(null, "" +
                "<dataset type=\"EDDTableFromAxiomStation\" datasetID=\"edu_dri_raws_cacana\">\n" +
                "    <sourceUrl>http://sensors.axds.co/stationsensorservice/</sourceUrl>\n" +
                "    <stationId>16175</stationId>\n" +
                "</dataset>"
        );
        query = "air_temperature_cm_time_mean,time,latitude,longitude&time>=2016-05-14T00:00:00Z&time<2016-05-16T00:00:00Z";
        tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory,
                edd.className() + "_station_sensor_" + edd.datasetID(), ".csv");
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        // Test station with a 'depth' variable
        EDD.oneFromXmlFragment(null, "" +
                "<dataset type=\"EDDTableFromAxiomStation\" datasetID=\"cencoos_tiburon\">\n" +
                "    <sourceUrl>http://sensors.axds.co/stationsensorservice/</sourceUrl>\n" +
                "    <stationId>20358</stationId>\n" +
                "</dataset>"
        );

        // Test station with unmapped parameters -> units
        EDD.oneFromXmlFragment(null, "" +
                "<dataset type=\"EDDTableFromAxiomStation\" datasetID=\"edu_ucdavis_bml_fpt_wts_latest\">\n" +
                "    <sourceUrl>http://sensors.axds.co/stationsensorservice/</sourceUrl>\n" +
                "    <stationId>19947</stationId>\n" +
                "</dataset>"
        );

    }
}
