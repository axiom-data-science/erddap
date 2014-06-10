package gov.noaa.pfel.erddap.dataset;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVAlt;

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
    private static int station_urn_column_num = 2;
    private static int station_name_column_num = 3;
    private static int sensor_name_column_num = 4;
    private static int latitude_column_num = 5;
    private static int longitude_column_num = 6;

    protected String noData = "";
    
    public static EDDTableFromAxiomSensorCSVService fromXml(SimpleXMLReader xmlReader) throws Throwable {
        String tDatasetID = xmlReader.attributeValue("datasetID");
        Attributes tGlobalAttributes = null;
        String tLocalSourceUrl = null;
        String tRegionSubset = "all";
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;
    
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
                break; // the </dataset> tag
            String localTags = tags.substring(startOfTagsLength);
            if (localTags.equals("<addAttributes>"))
                tGlobalAttributes = getAttributesFromXml(xmlReader);
            else if (localTags.equals("<sourceUrl>")) {
            } else if (localTags.equals("</sourceUrl>"))
                tLocalSourceUrl = content;
            else if (localTags.equals("<region>")) {
            } else if (localTags.equals("</region>"))
                tRegionSubset = content;
            else if (localTags.equals("<reloadEveryNMinutes>")) {
            } else if (localTags.equals("</reloadEveryNMinutes>"))
                tReloadEveryNMinutes = String2.parseInt(content);
            else if (localTags.equals("<defaultDataQuery>")) {
            } else if (localTags.equals("</defaultDataQuery>"))
                tDefaultDataQuery = content;
            else if (localTags.equals("<defaultGraphQuery>")) {
            } else if (localTags.equals("</defaultGraphQuery>"))
                tDefaultGraphQuery = content;
            else {
                xmlReader.unexpectedTagException();
            }
        }

        if (tGlobalAttributes == null)
            tGlobalAttributes = new Attributes();

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
        tGlobalAttributes.set("cdm_timeseries_variables", "sensor");
        tGlobalAttributes.set("subsetVariables", "latitude,longitude,station,sensor");
        
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
        // Station
        Attributes staatts = new Attributes();
        staatts.set("ioos_category", EDV.LOCATION_CATEGORY);
        staatts.set("cf_role", "timeseries_id");
        tDataVariables.add(new Object[] { "station", "station", staatts, "String" });
        // Sensor
        Attributes senatts = new Attributes();
        senatts.set("ioos_category", "Other");
        tDataVariables.add(new Object[] { "sensor", "sensor", senatts, "String" });       
        // Value
        Attributes valatts = new Attributes();
        valatts.set("ioos_category", "Other");
        tDataVariables.add(new Object[] { "value", "value", valatts, "double" }); 
        
        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++)
            ttDataVariables[i] = (Object[]) tDataVariables.get(i);
        
        makeSubsetFile(tDatasetID, tLocalSourceUrl, tRegionSubset);
        
        return new EDDTableFromAxiomSensorCSVService(tDatasetID, null, new StringArray(),
                null, null, tDefaultDataQuery, tDefaultGraphQuery,
                tGlobalAttributes, ttDataVariables, tReloadEveryNMinutes, tLocalSourceUrl,
                null, null, null);

    }
    
    public EDDTableFromAxiomSensorCSVService(String tDatasetID, String tAccessibleTo,
            StringArray tOnChange, String tFgdcFile, String tIso19115File,
            String tDefaultDataQuery, String tDefaultGraphQuery, Attributes tAddGlobalAttributes,
            Object[][] tDataVariables, int tReloadEveryNMinutes, String tLocalSourceUrl,
            String tBeforeData[], String tAfterData, String tNoData) throws Throwable {

        super("EDDTableFromAxiomSensorCSVService", tDatasetID, tAccessibleTo, tOnChange, tFgdcFile,
                tIso19115File, tDefaultDataQuery, tDefaultGraphQuery, tAddGlobalAttributes,
                tDataVariables, tReloadEveryNMinutes, tLocalSourceUrl, tBeforeData, tAfterData,
                tNoData);
        
        //dataVariableSourceNames = new String[] {"station", "sensor", "longitude", "latitude", "time", "value" };
        
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

    public static void makeSubsetFile(String dataset_id, String source_url, String region) throws Exception {
        String2.log("\nMaking Axiom Sensor Service subset file...");
        
        IntArray station_ids = new IntArray();
        StringArray station_urns = new StringArray();
        StringArray station_names = new StringArray();
        DoubleArray longitudes = new DoubleArray();
        DoubleArray latitudes = new DoubleArray();
        IntArray sensor_ids = new IntArray();
        StringArray sensor_names = new StringArray();
        Table table = new Table();
        table.addColumn("station_id", station_ids);
        table.addColumn("sensor_id", sensor_ids);
        table.addColumn("station", station_urns);
        table.addColumn("station_name", station_names);
        table.addColumn("sensor", sensor_names);
        table.addColumn("latitude", latitudes);
        table.addColumn("longitude", longitudes);
        

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
            Integer station_id = Integer.MIN_VALUE, station_sensor_id = Integer.MIN_VALUE;
            String station_name = null, station_urn = null;
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
                    station_ids.add(station_id);
                    station_names.add(station_name);
                    station_urns.add(station_urn);
                    latitudes.add(latitude);
                    longitudes.add(longitude);
                    station_sensor_id = station_sensors.getJSONObject(j).getInt("id");
                    sensor_ids.add(station_sensor_id);
                    sensor_names.add(sensor_lookup.get(station_sensor_id));
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
                if (conOp0 == '>')
                    beginSeconds = String2.parseDouble(conVal);
                else if (conOp0 == '<')
                    endSeconds = String2.parseDouble(conVal);
                continue;
            }
            
            // Is this constraint for a column that's in the lookupTable?
            int nKeep = lookupTable.tryToApplyConstraint(2, conVar, conOp, conVal, keep);
            if (nKeep == 0)
                throw new SimpleException(MustBe.THERE_IS_NO_DATA +
                   " (There are no matching stations.)");
        }
        
        // Get the data for all of the valid stations
        int stationRow = keep.nextSetBit(0);
        boolean errorPrinted = false;
        while (stationRow >= 0) {
            String station_id  = lookupTable.getStringData(station_id_column_num,  stationRow);
            String sensor_id   = lookupTable.getStringData(sensor_id_column_num,   stationRow);
            String station_urn = lookupTable.getStringData(station_urn_column_num, stationRow);
            String sensor_name = lookupTable.getStringData(sensor_name_column_num, stationRow);
            
            // URL to get data
            String encodedSourceUrl = localSourceUrl + "getExcelSheet?" + 
                "stationid=" + SSR.minimalPercentEncode(station_id) + 
                "&sensorid=" + SSR.minimalPercentEncode(sensor_id) +
                "&start_time=" + (int)beginSeconds +
                "&end_time="   + (int)endSeconds +
                "&version=2&csv_type=raw";
            
            try {
                BufferedReader in = getBufferedReader(encodedSourceUrl);
                String s = in.readLine();              
                ArrayList<String> strings = new ArrayList<String>();
                while (s != null) {
                    strings.add(s);
                    s = in.readLine();
                }
                in.close();
                
                // Rename columns to match what ERDDAP expects
                String new_headers = "station,sensor,latitude,longitude,time,value";
                strings.set(0, new_headers);
                
                Table table = new Table();
                String[] new_strings = new String[strings.size()];
                strings.toArray(new_strings);
                table.readASCII("", new_strings, 0, 1, null, null, null, null, true);
                
                // Figure out nRows (some cols have data, some don't)
                int nRows = 0;
                int nDV = table.nColumns();
                for (int dv = 0; dv < nDV; dv++) {
                    nRows = table.getColumn(dv).size();                    
                    if (nRows > 0)
                        break;
                }

                // Fill the station_urn column
                StringArray r_stations = (StringArray)table.getColumn("station");
                r_stations.clear();
                r_stations.addN(nRows, station_urn);

                // Fill the sensor_urn column
                StringArray r_sensors = (StringArray)table.getColumn("sensor");
                r_sensors.clear();
                r_sensors.addN(nRows, sensor_name);

                // Convert ISO String to epoch seconds
                StringArray r_times = (StringArray)table.getColumn("time");
                table.removeColumn("time");
                DoubleArray epoch_times_array = new DoubleArray();
                table.addColumn("time", epoch_times_array);
                for (String t : r_times.array) {
                    epoch_times_array.add((int)Calendar2.isoStringToEpochSeconds(t));
                }                                 

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
            }

            stationRow = keep.nextSetBit(stationRow + 1);
            if (tableWriter.noMoreDataPlease) {
                tableWriter.logCaughtNoMoreDataPlease(datasetID);
                break;
            }
        }
        tableWriter.finish();
        if (reallyVerbose) String2.log("\nFinished getDataForDapQuery");

    }
    
    public static void test() throws Throwable {
        String2.log("\n****************** EDDTableFromAxiomSensorCSVService.test() *****************\n");
        testVerboseOn();
        
        EDD edd = EDD.oneFromDatasetXml("axiom_sensor_service");
        // Test specific station and sensor
        String query = "&station=\"urn:ioos:station:wmo:46214\"&sensor=\"Water Temperature\"";
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
//        query = "&station=\"urn:ioos:station:wmo:46214\"&sensor=\"Water Temperature\"&time>=2014-04-01T00:00:00Z&time<=2014-06-01T00:00:00Z";
//        tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
//                    edd.className() + "_station_sensor_time_" + edd.datasetID(), ".csv"); 
//        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
//        String2.log(results);
        
        // lat/lon bounds and time bounds
//        query = "&longitude>=-130&longitude<=74&latitude>=36.751&latitude<=36.752&time>=2014-04-01T00:00:00Z&time<=2014-06-01T00:00:00Z";
//        tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
//                    edd.className() + "_lat_lon_time_" + edd.datasetID(), ".csv"); 
//        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
//        String2.log(results);
    }
}
