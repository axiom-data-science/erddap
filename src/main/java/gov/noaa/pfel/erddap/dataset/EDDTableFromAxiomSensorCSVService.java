package gov.noaa.pfel.erddap.dataset;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;

public class EDDTableFromAxiomSensorCSVService extends EDDTableFromAsciiService {

    protected Table             stationTable;
    protected Table             lookupTable;
    private final static String STANDARD_INFO_URL    = "http://docs.stationsensorservice.apiary.io/";
    private final static String AXIOM_SENSOR_TITLE   = "Axiom Sensor Service";
    private final static String AXIOM_SENSOR_SUMMARY = "Axiom Data Science - Station, Sensor, and Observation Web Service";
    private final static String AXIOM_SENSOR_CONTACT = "data@axiomalaska.com";
    private final static String AXIOM_SENSOR_LICENSE = "Unauthorized access is punishable by the use of sophisticated internet bullying tactics";

    private static int station_id_column_num = 0;
    private static int sensor_id_column_num = 1;
    private static int parameter_name_column_num = 2;
    private static int station_urn_column_num = 3;
    private static int station_name_column_num = 4;
    private static int latitude_column_num = 5;
    private static int longitude_column_num = 6;
    private static int unit_label_column_num = 7;
    private static int unit_column_num = 8;
    private static int parameter_id_column_num = 9;

    protected String noData = "";

    public static EDDTableFromAxiomSensorCSVService fromXml(SimpleXMLReader xmlReader) throws Throwable {
        String tDatasetID = xmlReader.attributeValue("datasetID");
        Attributes tGlobalAttributes = null;
        String tLocalSourceUrl = null;
        String tPortalId = "34";
        String tRegionSubset = "all";
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;
        String tSosOfferingPrefix = null;

        ArrayList<Object[]> tDataVariables = new ArrayList<Object[]>();

        // process the tags
        String startOfTags = xmlReader.allTags();
        int startOfTagsN = xmlReader.stackSize();
        int startOfTagsLength = startOfTags.length();
        while (true) {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            String content = xmlReader.content();
            if (xmlReader.stackSize() == startOfTagsN)
            {
                break; // the </dataset> tag
            }
            String localTags = tags.substring(startOfTagsLength);
            if (localTags.equals("<addAttributes>")) {
                tGlobalAttributes = getAttributesFromXml(xmlReader);
            } else if (localTags.equals("<sourceUrl>")) {
            } else if (localTags.equals("</sourceUrl>")) {
                tLocalSourceUrl = content;
            } else if (localTags.equals("<region>")) {
            } else if (localTags.equals("</region>")) {
                tRegionSubset = content;
            } else if (localTags.equals("<portalId>")) {
            } else if (localTags.equals("</portalId>")) {
                tPortalId = content;
            } else if (localTags.equals("<reloadEveryNMinutes>")) {
            } else if (localTags.equals("</reloadEveryNMinutes>")) {
                tReloadEveryNMinutes = String2.parseInt(content);
            } else if (localTags.equals("<defaultDataQuery>")) {
            } else if (localTags.equals("</defaultDataQuery>")) {
                tDefaultDataQuery = content;
            } else if (localTags.equals("<defaultGraphQuery>")) {
            } else if (localTags.equals("</defaultGraphQuery>")) {
                tDefaultGraphQuery = content;
            } else if (localTags.equals("<sosOfferingPrefix>")) {
            } else if (localTags.equals("</sosOfferingPrefix>")) {
                tSosOfferingPrefix = content;
            } else {
                xmlReader.unexpectedTagException();
            }
        }

        if (tGlobalAttributes == null) {
            tGlobalAttributes = new Attributes();
        }

        if (tGlobalAttributes.get("title") == null) {
            tGlobalAttributes.set("title", AXIOM_SENSOR_TITLE);
        }
        if (tGlobalAttributes.get("summary") == null) {
            tGlobalAttributes.set("summary", AXIOM_SENSOR_SUMMARY);
        }
        tGlobalAttributes.set(EDStatic.INSTITUTION, "Axiom Data Science");
        tGlobalAttributes.set("infoUrl", STANDARD_INFO_URL);
        tGlobalAttributes.set("sourceUrl", tLocalSourceUrl);
        tGlobalAttributes.set("cdm_data_type", EDD.CDM_TIMESERIES);
        tGlobalAttributes.set("cdm_timeseries_variables", "parameter");
        tGlobalAttributes.set("subsetVariables", "latitude,longitude,station,parameter,unit");

        // Time
        Attributes tatts = new Attributes();
        tatts.set("units", "seconds since 1970-01-01T00:00:00");
        tatts.set("ioos_category", EDV.TIME_CATEGORY);
        tDataVariables.add(new Object[] { "time", "time", tatts, "double" });
        // Latitude
        Attributes latats = new Attributes();
        latats.set("ioos_category", EDV.LOCATION_CATEGORY);
        tDataVariables.add(new Object[] { "latitude", "latitude", latats, "double" });
        // Longitude
        Attributes lonats = new Attributes();
        lonats.set("ioos_category", EDV.LOCATION_CATEGORY);
        tDataVariables.add(new Object[] { "longitude", "longitude", lonats, "double" });
        // Value
        Attributes depatts = new Attributes();
        depatts.set("ioos_category", EDV.LOCATION_CATEGORY);
        depatts.set("units", "m");
        depatts.set("positive", "down");
        tDataVariables.add(new Object[] { "depth", "depth", depatts, "double" });        
        // Station
        Attributes staatts = new Attributes();
        staatts.set("ioos_category", EDV.LOCATION_CATEGORY);
        staatts.set("cf_role", "timeseries_id");
        tDataVariables.add(new Object[] { "station", "station", staatts, "String" });
        // Parameter
        Attributes senatts = new Attributes();
        senatts.set("ioos_category", "Other");
        tDataVariables.add(new Object[] { "parameter", "parameter", senatts, "String" });
        // Unit
        Attributes unitatts = new Attributes();
        unitatts.set("ioos_category", "Other");
        tDataVariables.add(new Object[] { "unit", "unit", unitatts, "String" });
        // Value
        Attributes valatts = new Attributes();
        valatts.set("ioos_category", "Other");
        valatts.set("missing_value", new Double(-9999.99));
        valatts.set("_FillValue", new Double(-9999.99));
        tDataVariables.add(new Object[] { "value", "value", valatts, "double" });
        

        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++) {
            ttDataVariables[i] = tDataVariables.get(i);
        }

        makeSubsetFile(tDatasetID, tLocalSourceUrl, tRegionSubset, tPortalId);

        return new EDDTableFromAxiomSensorCSVService(tDatasetID, null, new StringArray(),
                null, null, tSosOfferingPrefix, tDefaultDataQuery, tDefaultGraphQuery,
                tGlobalAttributes, ttDataVariables, tReloadEveryNMinutes, tLocalSourceUrl,
                null, null, null);

    }

    public EDDTableFromAxiomSensorCSVService(String tDatasetID, String tAccessibleTo,
            StringArray tOnChange, String tFgdcFile, String tIso19115File, String tSosOfferingPrefix,
            String tDefaultDataQuery, String tDefaultGraphQuery, Attributes tAddGlobalAttributes,
            Object[][] tDataVariables, int tReloadEveryNMinutes, String tLocalSourceUrl,
            String tBeforeData[], String tAfterData, String tNoData) throws Throwable {

        super("EDDTableFromAxiomSensorCSVService", tDatasetID, tAccessibleTo, tOnChange, tFgdcFile,
                tIso19115File, tSosOfferingPrefix, tDefaultDataQuery, tDefaultGraphQuery, tAddGlobalAttributes,
                tDataVariables, tReloadEveryNMinutes, tLocalSourceUrl, tBeforeData, tAfterData,
                tNoData);

        // find a user who is authorized to access this dataset
        String user = (accessibleTo == null || accessibleTo.length == 0) ? null : accessibleTo[0];
        stationTable = subsetVariablesDataTable(user);
        stationTable.removeDuplicates();

        String json_lookup_file = EDStatic.contentDirectory + "subset/" + datasetID + ".json";
        if (File2.isFile(json_lookup_file)) {
            lookupTable = new Table();
            lookupTable.readJson(json_lookup_file);
        }
    }

    public static void makeSubsetFile(String dataset_id, String source_url, String region, String portal_id) throws Exception {
        String2.log("\nMaking Axiom Sensor Service subset file...");

        IntArray station_ids = new IntArray();
        StringArray station_urns = new StringArray();
        StringArray station_names = new StringArray();
        DoubleArray longitudes = new DoubleArray();
        DoubleArray latitudes = new DoubleArray();
        IntArray sensor_ids = new IntArray();
        IntArray parameter_ids = new IntArray();
        StringArray parameter_names = new StringArray();
        StringArray parameter_units_label = new StringArray();
        StringArray parameter_units = new StringArray();
        Table table = new Table();
        table.addColumn("station_id", station_ids);
        table.addColumn("sensor_id", sensor_ids);
        table.addColumn("parameter", parameter_names);
        table.addColumn("station", station_urns);
        table.addColumn("station_name", station_names);
        table.addColumn("latitude", latitudes);
        table.addColumn("longitude", longitudes);
        table.addColumn("unit_label", parameter_units_label);
        table.addColumn("unit", parameter_units);
        table.addColumn("parameter_id", parameter_ids);

        // Parameters (from Oikos...)
        HashMap<Integer,ArrayList<String[]>> parameter_lookup = new HashMap<Integer,ArrayList<String[]>>();
        URL url = new URL("http://pdx0.axiomalaska.com/oikos-service/rest/minimal-portal-data?id="+portal_id);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.addRequestProperty("Accept", "application/json");
        conn.setUseCaches(false);
        InputStream ps = conn.getInputStream();
        try {
            BufferedReader bfrd = new BufferedReader(
                    new InputStreamReader(ps, Charset.forName("UTF-8")));
            StringBuilder strbf = new StringBuilder();
            String ln = null;
            while ((ln = bfrd.readLine()) != null) {
                strbf.append(ln + "\n");
            }
            JSONObject param_json = new JSONObject(strbf.toString());

            // Units
            HashMap<Integer,String[]> units_lookup = new HashMap<Integer,String[]>();
            JSONArray units_array = param_json.getJSONArray("units");
            for (int a = 0 ; a < units_array.length() ; a++) {
                JSONObject unit = units_array.getJSONObject(a);
                String[] unit_array = new String[2];
                unit_array[0] = unit.getString("unit");
                unit_array[1] = unit.getString("label");
                units_lookup.put(unit.getInt("id"), unit_array);
            }

            // ParameterTypeUnits
            HashMap<Integer,ArrayList<String[]>> parameter_type_lookup = new HashMap<Integer,ArrayList<String[]>>();
            JSONArray param_type_units_array = param_json.getJSONArray("parameterTypeUnits");
            for (int b = 0 ; b < param_type_units_array.length() ; b++) {
                JSONObject param = param_type_units_array.getJSONObject(b);
                if (!parameter_type_lookup.containsKey(param.getInt("idParameterType"))) {
                    // Create array
                    parameter_type_lookup.put(param.getInt("idParameterType"), new ArrayList<String[]>());
                }
                // Append to array of units
                ArrayList<String[]> all_unit_info = parameter_type_lookup.get(param.getInt("idParameterType"));
                all_unit_info.add(units_lookup.get(param.getInt("idUnit")));
                parameter_type_lookup.put(param.getInt("idParameterType"), all_unit_info);
            }

            // Parameters
            JSONArray param_array = param_json.getJSONArray("parameters");
            for (int c = 0 ; c < param_array.length() ; c++) {
                JSONObject param = param_array.getJSONObject(c);

                if (!parameter_lookup.containsKey(param.getInt("id"))) {
                    // Create array
                    parameter_lookup.put(param.getInt("id"), new ArrayList<String[]>());
                }
                // Append to array of parameter/unit combos
                ArrayList<String[]> all_parameter_info = parameter_lookup.get(param.getInt("id"));
                String[] name_unit = new String[3];
                name_unit[0] = param.getString("label");
                if (param.get("idParameterType").equals(null)) {
                    name_unit[1] = "Unknown";
                    name_unit[2] = "Unknown";
                    all_parameter_info.add(name_unit);
                } else {
                    ArrayList<String[]> unit_metadata = parameter_type_lookup.get(param.getInt("idParameterType"));
                    for (String[] unit_array : unit_metadata) {
                        name_unit[1] = unit_array[0];
                        name_unit[2] = unit_array[1];
                        all_parameter_info.add(name_unit.clone());
                    }
                }
                parameter_lookup.put(param.getInt("id"), all_parameter_info);
            }

        } finally { ps.close(); }


        // Get all sensors and stations
        InputStream is = new URL(source_url + "getDataValues?method=GetStationsResultSetRowsJSON&version=2&appregion=" + region + "&realtimeonly=false&verbose=true&jsoncallback=false").openStream();
        try {
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = rd.readLine()) != null) {
                sb.append(line + "\n");
            }
            JSONObject json = new JSONObject(sb.toString());

            // Sensors
            JSONObject sensors_array = json.getJSONObject("sensors");
            HashMap<Integer,String> sensor_lookup = new HashMap<Integer,String>();

            Iterator<?> sensor_keys = sensors_array.keys();
            Integer sensor_id = Integer.MIN_VALUE;
            String sensor_name = null;
            while (sensor_keys.hasNext()) {
                String key = (String) sensor_keys.next();
                JSONObject s = (JSONObject) sensors_array.get(key);
                sensor_id = s.getInt("id");
                sensor_name= s.getString("label");
                sensor_lookup.put(sensor_id, sensor_name);
            }

            // Stations
            JSONArray stations_array = json.getJSONArray("stations");
            Integer station_id = Integer.MIN_VALUE, station_sensor_id = Integer.MIN_VALUE, parameter_id = Integer.MIN_VALUE;
            String station_name = null, station_urn = null; sensor_name = null;
            Double latitude = Double.NaN, longitude = Double.NaN;
            for (int i = 0 ; i < stations_array.length() ; i++) {
                JSONObject stat = stations_array.getJSONObject(i);
                station_id = stat.getInt("id");
                station_name = stat.getString("label");
                station_urn = stat.getString("urn");
                latitude = stat.getDouble("latitude");
                longitude = stat.getDouble("longitude");
                JSONArray station_sensors = stat.getJSONArray("sensors");
                for (int j = 0 ; j < station_sensors.length() ; j++) {
                    JSONObject sensor_obj = station_sensors.getJSONObject(j);
                    station_sensor_id = sensor_obj.getInt("id");
                    sensor_name = sensor_lookup.get(station_sensor_id);

                    JSONArray station_sensors_parameters = sensor_obj.getJSONArray("parameterIds");
                    for (int k = 0 ; k < station_sensors_parameters.length() ; k++) {
                        parameter_id = station_sensors_parameters.getInt(k);
                        for (String[] paramter_unit_info : parameter_lookup.get(parameter_id)) {
                            station_ids.add(station_id);
                            station_names.add(station_name);
                            station_urns.add(station_urn);
                            latitudes.add(latitude);
                            longitudes.add(longitude);
                            sensor_ids.add(station_sensor_id);
                            parameter_ids.add(parameter_id);
                            parameter_names.add(paramter_unit_info[0]);
                            parameter_units.add(paramter_unit_info[1]);
                            parameter_units_label.add(paramter_unit_info[2]);
                        }
                    }
                }
            }
        } finally { is.close(); }

        table.leftToRightSort(2);
        String saveDir = EDStatic.contentDirectory + "subset/";
        File2.makeDirectory(saveDir);
        String saveFile = saveDir + dataset_id + ".json";
        String2.log("\nSaving Axiom Sensor Service subset file to " + saveFile);
        table.saveAsJson(saveFile, -1, false);

        String2.log("\n... done");
    }

    @Override
    public void getDataForDapQuery(
            String loggedInAs,
            String requestUrl,
            String userDapQuery,
            TableWriter tableWriter) throws Throwable {

        // make the sourceQuery
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        getSourceQueryFromDapQuery(userDapQuery, resultsVariables, constraintVariables, constraintOps, constraintValues);

        int nStations = lookupTable.nRows();
        BitSet keep = new BitSet();
        keep.set(0, nStations, true);
        double beginSeconds = 0;
        // Default endTime is an hour ahead of now (for safety)
        double endSeconds   = Calendar2.gcToEpochSeconds(Calendar2.newGCalendarZulu()) + 3600;

        int nConstraints = constraintVariables.size();
        for (int c = 0; c < nConstraints; c++) {
            String conVar = constraintVariables.get(c);
            String conOp  = constraintOps.get(c);
            String conVal = constraintValues.get(c);
            // First char in the operation
            char conOp0  = conOp.charAt(0);
            if (conVar.equals("time")) {
                if (conOp0 == '>') {
                    beginSeconds = String2.parseDouble(conVal);
                } else if (conOp0 == '<') {
                    endSeconds = String2.parseDouble(conVal);
                }
                continue;
            }

            // Is this constraint for a column that's in the lookupTable?
            int nKeep = lookupTable.tryToApplyConstraint(2, conVar, conOp, conVal, keep);
            if (nKeep == 0) {
                throw new SimpleException(MustBe.THERE_IS_NO_DATA +
                        " (There are no matching stations.)");
            }
        }

        // Get the data for all of the valid stations
        int stationRow = keep.nextSetBit(0);
        boolean errorPrinted = false;
        while (stationRow >= 0) {
            String station_id     = lookupTable.getStringData(station_id_column_num,  stationRow);
            String sensor_id      = lookupTable.getStringData(sensor_id_column_num,   stationRow);
            String station_urn    = lookupTable.getStringData(station_urn_column_num, stationRow);
            String station_name   = lookupTable.getStringData(station_name_column_num, stationRow);
            Integer parameter_id  = lookupTable.getIntData(parameter_id_column_num, stationRow);
            String parameter_name = lookupTable.getStringData(parameter_name_column_num, stationRow);
            String unit           = lookupTable.getStringData(unit_column_num, stationRow);
            String unit_label     = lookupTable.getStringData(unit_label_column_num, stationRow);
            Double latitude       = lookupTable.getDoubleData(latitude_column_num, stationRow);
            Double longitude      = lookupTable.getDoubleData(longitude_column_num, stationRow);

            // URL to get data
            String encodedSourceUrl = localSourceUrl + "getDataValues" +
                    "?stationid=" + URLEncoder.encode(SSR.minimalPercentEncode(station_id), "UTF-8") +
                    "&sensorid=" + URLEncoder.encode(SSR.minimalPercentEncode(sensor_id), "UTF-8") +
                    "&units=" + URLEncoder.encode((int)parameter_id + ";" + unit, "UTF-8") +
                    "&start_time=" + URLEncoder.encode(String.valueOf((int)beginSeconds), "UTF-8") +
                    "&end_time="   + URLEncoder.encode(String.valueOf((int)endSeconds), "UTF-8") +
                    "&jsoncallback=false" +
                    "&version=2" +
                    "&method=GetSensorObservationsJSON";
            InputStream is = new URL(encodedSourceUrl).openStream();
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(is, Charset.forName("UTF-8")));
                StringBuilder strb = new StringBuilder();
                String str = null;
                while ((str = in.readLine()) != null) {
                    strb.append(str);
                }
                JSONObject results = new JSONArray(strb.toString()).getJSONObject(0);
                JSONArray data = results.getJSONArray("data");

                Table table = new Table();
                IntArray epoch_times_array = new IntArray();  // Holds the single time list
                IntArray actual_times_array = new IntArray(); // Holds N number of time lists, one for each depth this parameter is at
                // Using a Double here resulted in crazy sigfigs.
                FloatArray values_array = new FloatArray();
                DoubleArray depth_array  = new DoubleArray();
                Double depth = new Double(0);

                for (int c = 0 ; c < data.length() ; c++) {
                    JSONObject vari = data.getJSONObject(c);
                    if (vari.getJSONObject("metadata").getString("label").equals("time")) {
                        // TIME
                        table.addColumn("time", actual_times_array);
                        JSONArray values = vari.getJSONObject("variableValueCollection").getJSONArray("values");
                        for (int d = 0 ; d < values.length() ; d++) {
                            epoch_times_array.add(values.getInt(d));
                        }
                        break;
                    }
                }
                
                for (int f = 0 ; f < data.length() ; f++) {
                    JSONObject vari = data.getJSONObject(f);
                    if (vari.getJSONObject("metadata").getInt("parameterId") == parameter_id) {
                        // DESIRED PARAMETER
                        
                        // Add times to ongoing array
                        actual_times_array.add(epoch_times_array.toArray());
                                                
                        try {
                            // Column already exists, so we have multiple depths.
                            table.getColumn("value");
                            table.getColumn("depth");
                        } catch (IllegalArgumentException e) {
                            // Column does not exist yet, so we create it
                            table.addColumn("depth", depth_array);
                            table.addColumn("value", values_array);    
                        }
                                                    
                        depth = vari.getJSONObject("metadata").getDouble("depth");
                        JSONArray values = vari.getJSONObject("variableValueCollection").getJSONArray("values");
                        for (int e = 0 ; e < values.length() ; e++) {
                            try {
                                values_array.add(new Float(values.getDouble(e)));
                            } catch (JSONException ex) {
                                // Most likely the "null" string which is used for a fill value in the sensor service.
                                values_array.add(new Float(-9999.99));
                            }
                            depth_array.add(depth);
                        }
                        break;
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
                StringArray stat = new StringArray();
                table.addColumn("station", stat);
                stat.addN(nRows, station_urn);

                // Fill the sensor_urn column
                StringArray sens = new StringArray();
                table.addColumn("parameter", sens);
                sens.addN(nRows, parameter_name);

                // Fill the longitude column
                DoubleArray lons_array = new DoubleArray();
                table.addColumn("longitude", lons_array);
                lons_array.addNDoubles(nRows, longitude);

                // Fill the latitude column
                DoubleArray lats_array = new DoubleArray();
                table.addColumn("latitude", lats_array);
                lats_array.addNDoubles(nRows, latitude);

                // Fill the units column
                StringArray units_array = new StringArray();
                table.addColumn("unit", units_array);
                units_array.addN(nRows, unit);

                standardizeResultsTable(requestUrl, userDapQuery, table);

                if (table.nRows() > 0) {
                    tableWriter.writeSome(table);
                }

            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);
                if (verbose) {
                    String2.log(String2.ERROR + " for stationID=" + station_id +
                            (errorPrinted? "" :
                                encodedSourceUrl + "\n" + MustBe.throwableToString(t)));
                    errorPrinted = true;
                }
            } finally { is.close(); }

            stationRow = keep.nextSetBit(stationRow + 1);
            if (tableWriter.noMoreDataPlease) {
                tableWriter.logCaughtNoMoreDataPlease(datasetID);
                break;
            }
        }
        tableWriter.finish();
        if (reallyVerbose) {
            String2.log("\nFinished getDataForDapQuery");
        }

    }

    public static void main(String[] args) throws Throwable {
        String2.log("\n****************** EDDTableFromAxiomSensorCSVService.test() *****************\n");
        testVerboseOn();

        EDD edd = EDD.oneFromDatasetXml("axiom_sensor_service");
        // Test specific station and sensor
        String query = "&station=%22urn:ioos:station:wmo:46027%22&parameter=%22Air Temperature%22&time>=2014-11-01T00:00:00Z&time<=2014-12-01T00:00:00Z";
        String  tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory,
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
        query = "&station=%22urn:ioos:station:cencoos:MossLanding%22&time%3Enow-7days";
        tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory,
                    edd.className() + "_station_sensor_time_" + edd.datasetID(), ".csv");
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        // lat/lon bounds and time bounds
        //        query = "&longitude>=-130&longitude<=74&latitude>=36.751&latitude<=36.752&time>=2014-04-01T00:00:00Z&time<=2014-06-01T00:00:00Z";
        //        tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory,
        //                    edd.className() + "_lat_lon_time_" + edd.datasetID(), ".csv");
        //        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //        String2.log(results);
    }
}
