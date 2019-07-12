package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.ByteArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.Test;

import static gov.noaa.pfel.erddap.dataset.EDD.testVerboseOn;

public class EDDTableFromAxiomStationV1Test {

    @Test
    public void integrationTest() throws Throwable {
        String2.log("\n****************** EDDTableFromAxiomStationV1.integrationTest() *****************\n");
        testVerboseOn();

        EDD edd = EDD.oneFromXmlFragment(null, "" +
                "<dataset type=\"EDDTableFromAxiomStationV1\" datasetID=\"noaa_nos_co_ops_adka2\">\n" +
                "    <sourceUrl>https://sensors.axds.co/api/</sourceUrl>\n" +
                "    <v1DataSourceUrl>https://sensors.axds.co/stationsensorservice/</v1DataSourceUrl>\n" +
                "    <stationId>13820</stationId>\n" +
                "</dataset>"
        );
        // Test specific station and sensor
        writeDataTable(edd, "sea_water_temperature,time,latitude,longitude,z&time>=2017-12-11T00:00:00Z&time<2017-12-11T08:00:00Z");
        writeDataTable(edd, "wind_speed,time,latitude,longitude,z&time>=2017-12-11T00:00:00Z&time<2017-12-12T00:00:00Z");
        writeDataTable(edd, "sea_water_temperature,wind_speed,time&time>=2017-12-11T12:00:00Z&time<2017-12-12T00:00:00Z");
        writeDataTable(edd, "sea_water_temperature,wind_speed,time&time>=2017-12-11T00:00:00Z&time<2017-12-12T00:00:00Z");
        writeDataTable(edd, "sea_water_temperature,station,time&time>=2017-12-11T00:00:00Z&time<2017-12-12T00:00:00Z");

    }

    private void writeDataTable(EDD edd, String query) throws Throwable {
        String2.log(query);
        String tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory,
                edd.className() + "_station_sensor_" + edd.datasetID(), ".csv");
        String results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(EDStatic.fullTestCacheDirectory + tName);
        String2.log(results);
    }

}
