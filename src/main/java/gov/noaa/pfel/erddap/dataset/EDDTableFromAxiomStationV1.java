package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;

// Reads metadata from V2 service (sensors.axds.co/api) and reads data from V1 service (sensors.axds.co/stationsensorservice)
public class EDDTableFromAxiomStationV1 extends EDDTableFromAsciiService {

    protected OikosStation station;
    protected String v1DataSourceUrl;

    public static EDDTableFromAxiomStationV1 fromXml(Erddap erddap, SimpleXMLReader xmlReader) throws Throwable {
        EDDTableFromAxiomStationV2Shim shim = new EDDTableFromAxiomStationV2Shim(xmlReader).invoke();
        return new EDDTableFromAxiomStationV1(shim);
    }

    public EDDTableFromAxiomStationV1(EDDTableFromAxiomStationV2Shim d) throws Throwable {
        super("EDDTableFromAxiomStationV1", d.tDatasetID,
                null, null,
                null, null, null,
                d.tSosOfferingPrefix,
                d.tDefaultDataQuery, d.tDefaultGraphQuery,
                d.tAddVariablesWhere,
                d.tGlobalAttributes,
                d.ttDataVariables,
                d.tReloadEveryNMinutes, d.tLocalSourceUrl,
                null, null, null);
        this.station = d.station;
        this.v1DataSourceUrl = d.v1DataSourceUrl;
    }

    @Override
    public void getDataForDapQuery(
            String loggedInAs,
            String requestUrl,
            String userDapQuery,
            TableWriter tableWriter) throws Throwable {

        // make the sourceQuery
        StringArray resultsVariables = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps = new StringArray();
        StringArray constraintValues = new StringArray();
        getSourceQueryFromDapQuery(userDapQuery, resultsVariables, constraintVariables, constraintOps, constraintValues);

//        String2.log("userDapQuery: " + userDapQuery);
//        String2.log("resultsVariables: " + resultsVariables.toCSVString());
//        String2.log("constraintVariables: " + constraintVariables.toCSVString());
//        String2.log("constraintOps: " + constraintOps.toCSVString());
//        String2.log("constraintValues: " + constraintValues.toCSVString());

        double beginSeconds = 0;
        // Default endTime is an hour ahead of now (for safety)
        double endSeconds = Calendar2.gcToEpochSeconds(Calendar2.newGCalendarZulu()) + 3600;

        int nConstraints = constraintVariables.size();
        for (int c = 0; c < nConstraints; c++) {
            String conVar = constraintVariables.get(c);
            String conOp = constraintOps.get(c);
            String conVal = constraintValues.get(c);
            // First char in the operation
            char conOp0 = conOp.charAt(0);
            if (conVar.equals("time")) {
                if (conOp0 == '>') {
                    beginSeconds = String2.parseDouble(conVal);
                } else if (conOp0 == '<') {
                    endSeconds = String2.parseDouble(conVal);
                }
                continue;
            }
        }

        // Get the data requested
        boolean errorPrinted = false;

        ArrayList<String> parameter_id_builder = new ArrayList<>();
        ArrayList<String> units_builder = new ArrayList<>();
        for (OikosDevice d : this.station.devices) {
            OikosUnit u = d.sp.unit;
            units_builder.add(d.sp.parameter.id + ";" + u.unit);
            parameter_id_builder.add(String.valueOf(d.sp.parameter.id));
        }

        // URL to get data
        String encodedSourceUrl = v1DataSourceUrl + "getDataValues" +
                "?stationid=" + this.station.id +
                "&parameterids=" + SSR.minimalPercentEncode(String.join(",", parameter_id_builder)) +
                "&units=" + SSR.minimalPercentEncode(String.join(",", units_builder)) +
                "&start_time=" + URLEncoder.encode(String.valueOf((int) beginSeconds), "UTF-8") +
                "&end_time=" + URLEncoder.encode(String.valueOf((int) endSeconds), "UTF-8") +
                "&jsoncallback=false" +
                "&version=3" +
                "&force_binned_data=false" +
                "&method=GetSensorObservationsJSON";
//        String2.log(encodedSourceUrl);
        InputStream is = new URL(encodedSourceUrl).openStream();
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder strb = new StringBuilder();
            String str;
            while ((str = in.readLine()) != null) {
                strb.append(str);
            }
            JSONArray data = new JSONArray(strb.toString());

            Table table = mapDataJsonToTable(tableWriter, data, this.station);

            standardizeResultsTable(requestUrl, userDapQuery, table);

            if (table.nRows() > 0) {
                tableWriter.writeSome(table);
            }

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);
            if (verbose) {
                String2.log(String2.ERROR + " for stationID=" + this.station.id +
                        (errorPrinted ? "" :
                                encodedSourceUrl + "\n" + MustBe.throwableToString(t)));
                errorPrinted = true;
            }
        } finally {
            is.close();
        }

        if (tableWriter.noMoreDataPlease) {
            tableWriter.logCaughtNoMoreDataPlease(datasetID);
        }

        tableWriter.finish();
        if (reallyVerbose) {
            String2.log("\nFinished getDataForDapQuery");
        }
    }

    static Table mapDataJsonToTable(TableWriter tableWriter, JSONArray results, OikosStation station) throws Throwable {
        JSONArray data = results.getJSONObject(0).getJSONArray("data");

        Table table = new Table();

        // Fill the station_urn column
        StringArray stat = new StringArray();
        table.addColumn("station", stat);

        // Fill the longitude column
        DoubleArray lons_array = new DoubleArray();
        table.addColumn("lon", lons_array);

        // Fill the latitude column
        DoubleArray lats_array = new DoubleArray();
        table.addColumn("lat", lats_array);

        IntArray actual_times_array = new IntArray();
        DoubleArray z_array = new DoubleArray();

        for (int c = 0; c < data.length(); c++) {
            JSONObject vari = data.getJSONObject(c);
            if (vari.getJSONObject("metadata").getString("label").equals("time")) {
                // TIME
                table.addColumn("time", actual_times_array);
                JSONArray values = vari.getJSONObject("variableValueCollection").getJSONArray("values");
                for (int d = 0; d < values.length(); d++) {
                    actual_times_array.add(values.getInt(d));
                }
            } else {
                // DATA
                if (station.hasDevice(vari.getJSONObject("metadata").getInt("device_id"))) {
                    // DESIRED DEVICE
                    OikosDevice od = station.getDevice(vari.getJSONObject("metadata").getInt("device_id"));

                    // Using a Double here resulted in crazy sigfigs.
                    DoubleArray values_array = new DoubleArray();

                    String unitString = vari.getJSONObject("metadata").getString("unit");
                    String columnName = "value_" + od.id;
                    String columnZName = "z";

                    try {
                        // Column already exists, so we have multiple depths.
                        table.getColumn(columnName);
                        table.getColumn(columnZName);
                    } catch (IllegalArgumentException e) {
                        // Column does not exist yet, so we create it
                        table.addColumn(columnName, values_array);
                        table.addColumn(columnZName, z_array);
                    }

                    JSONArray values = vari.getJSONObject("variableValueCollection").getJSONArray("values");
                    for (int e = 0; e < values.length(); e++) {
                        try {
                            values_array.add(values.getDouble(e));
                        } catch (JSONException ex) {
                            // Most likely the "null" string which is used for a fill value in the sensor service.
                            values_array.add(-9999.99);
                        }
                    }
                    // This will only work if each device is measured at the same depths
                    // If not, the indexes will be all messed up.  We should probably error here, or
                    // Add a column for each device depth value.
                    Double depth = vari.getJSONObject("metadata").getDouble("depth");
                    double z = depth == 0.0 ? 0.0 : -1 * depth;
                    if (z_array.indexOf(depth) == -1) {
                        z_array.addN(values_array.size(), z);
                    }
                    if (table.nRows() > 0) {
                        tableWriter.writeSome(table);
                    }
                }
            }
        }

        // Figure out nRows (some cols have data, some don't)
        int nRows = 0;
        int nDV = table.nColumns();
        for (int dv = 0; dv < nDV; dv++) {
            nRows = table.getColumn(dv).size();
            if (nRows > 0) {
                break;
            }
        }

        // Fill the station_urn column
        stat.addN(nRows, station.label);

        // Fill the longitude column
        lons_array.addNDoubles(nRows, station.longitude);

        // Fill the latitude column
        lats_array.addNDoubles(nRows, station.latitude);

        return table;
    }

}
