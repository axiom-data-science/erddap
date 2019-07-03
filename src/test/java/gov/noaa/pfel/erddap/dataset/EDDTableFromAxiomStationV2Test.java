package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;

import static gov.noaa.pfel.erddap.dataset.EDD.testVerboseOn;
import static gov.noaa.pfel.erddap.dataset.EDDTableFromAxiomStationUnitTest.*;
import static org.junit.Assert.assertEquals;

public class EDDTableFromAxiomStationV2Test {

    @Test
    public void testTestsAreRunning() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testV2StationDataMapping() throws Throwable {
        String2.log("\n****************** EDDTableFromAxiomStationV2.testV2StationDataMapping() *****************\n");

        // Build station metadata
        Attributes tGlobalAttributes = new Attributes();
        ArrayList<Object[]> tDataVariables = new ArrayList<>();
        JSONObject json = jsonObjectFromTestResource("/v2-sensor-service-metadata-60387.json");
        EDDTableFromAxiomStationV2Utils.mapMetadataForOikosStation(tGlobalAttributes, 60387, tDataVariables, mapOikosLookups(), json);

        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++) {
            ttDataVariables[i] = tDataVariables.get(i);
        }

        // Pull data into Table
        String archivePath = testResourcePath("/station_60387.nc");
        EDDTableFromAxiomStationV2 tableFromStation = new EDDTableFromAxiomStationV2("60387",
                null, null,
                null, null, null,
                null,
                null, null,
                tGlobalAttributes,
                ttDataVariables,
                Integer.MAX_VALUE,
                0, archivePath, null, false,
                null, "last", null,
                0, 1, null,
                null, null, null, null, null, null, false,
                false, true, false);
        String query = "sea_water_temperature,wind_speed,station,time&time>=2017-12-11T00:00:00Z&time<2017-12-12T00:00:00Z";
        MockTableWriter tableWriter = new MockTableWriter(null, null, null);
        tableFromStation.getDataForDapQuery(null, null, query, tableWriter);
        Table table = tableWriter.table;

        assertEquals(12, table.nRows());
        assertEquals(4, table.nColumns());

        assertEquals(1513015500, table.getColumn("time").getLong(0));
        assertDoubleEquals(22.36, table.getColumn("sea_water_temperature").getDouble(0));
        assertDoubleEquals(3.33, table.getColumn("wind_speed").getDouble(0));
        assertEquals("", table.getColumn("station").getString(0));
    }

    @Test
    public void integrationTest() throws Throwable {
        String2.log("\n****************** EDDTableFromAxiomStationV2.integrationTest() *****************\n");
        testVerboseOn();

        EDD edd = EDD.oneFromXmlFragment(null, "" +
                "<dataset type=\"EDDTableFromAxiomStationV2\" datasetID=\"60387\">\n" +
                "    <sourceUrl>https://sensors.axds.co/api/</sourceUrl>\n" +
                "    <stationId>60387</stationId>\n" +
                "</dataset>"
        );
        // Test specific station and sensor
        writeDataTable(edd, "sea_water_temperature,time,latitude,longitude,z&time>=2017-12-11T00:00:00Z&time<2017-12-12T00:00:00Z");
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
