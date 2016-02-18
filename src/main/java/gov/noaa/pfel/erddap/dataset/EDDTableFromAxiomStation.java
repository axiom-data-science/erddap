package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.*;
import com.cohort.util.*;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.Erddap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;


class OikosUnit {

    public int id;
    public String unit;
    public String label;
    public String unitSystem;

    public static OikosUnit fromJson(JSONObject j) {

        return new OikosUnit(j.getInt("id"),
                             j.getString("unit"),
                             j.getString("label"),
                             j.getString("unitSystem"));
    }

    public OikosUnit(int id, String unit, String label, String unitSystem) {
        this.id = id;
        this.unit = unit;
        this.label = label;
        this.unitSystem = unitSystem;
    }
}

class OikosParameterTypeUnit {

    public int id;
    public OikosUnit unit;
    public boolean unitSystemDefault;
    public boolean parameterTypeDefault;

    public static OikosParameterTypeUnit fromJson(JSONObject j, HashMap<Integer, OikosUnit> unitMap) {

        return new OikosParameterTypeUnit(j.getInt("idParameterType"),
                                          unitMap.get(j.getInt("idUnit")),
                                          j.getBoolean("unitSystemDefault"),
                                          j.getBoolean("parameterTypeDefault"));
    }

    public OikosParameterTypeUnit(int id, OikosUnit unit, boolean unitSystemDefault, boolean parameterTypeDefault) {
        this.id = id;
        this.unit = unit;
        this.unitSystemDefault = unitSystemDefault;
        this.parameterTypeDefault = parameterTypeDefault;
    }
}

class OikosParameter {

    public int id;
    public ArrayList<OikosParameterTypeUnit> parameterTypeUnits;
    public String urn;
    public String name;
    public String label;

    public static OikosParameter fromJson(JSONObject j, HashMap<Integer, ArrayList<OikosParameterTypeUnit>> parameterTypeUnitMap) {

        ArrayList<OikosParameterTypeUnit> ptms;
        if (!j.isNull("idParameterType")) {
            ptms = parameterTypeUnitMap.get(j.getInt("idParameterType"));
        } else {
            OikosUnit un = new OikosUnit(-1, "unknown", "unknown", "NON_STANDARD");
            OikosParameterTypeUnit ptm = new OikosParameterTypeUnit(-1, un, true, true);
            ptms = new ArrayList<>();
            ptms.add(ptm);
        }

        return new OikosParameter(j.getInt("id"),
                                  ptms,
                                  j.getString("urn"),
                                  j.getString("parameterName"),
                                  j.getString("label"));
    }

    public OikosParameter(int id, ArrayList<OikosParameterTypeUnit> parameterTypeUnits, String urn, String name, String label) {
        this.id = id;
        this.parameterTypeUnits = parameterTypeUnits;
        this.urn = urn;
        this.name = name;
        this.label = label;
    }

    public OikosUnit defaultUnit() {
        for (OikosParameterTypeUnit ptu : this.parameterTypeUnits) {
            if (ptu.parameterTypeDefault) {
                return ptu.unit;
            }
        }
        return this.parameterTypeUnits.get(0).unit;
    }
}

class OikosEnhancedParameter {

    public int id;
    public OikosParameter parameter;
    public String cellMethods;
    public String interval;
    public String verticalDatum;

    public static OikosEnhancedParameter fromJson(JSONObject j, HashMap<Integer, OikosParameter> parameterMap) {

        return new OikosEnhancedParameter(j.getInt("id"),
                                          parameterMap.get(j.getInt("parameterId")),
                                          j.getString("cellMethods"),
                                          j.getString("interval"),
                                          j.getString("verticalDatum"));
    }

    public OikosEnhancedParameter(int id, OikosParameter parameter, String cellMethods, String interval, String verticalDatum) {
        this.id = id;
        this.parameter = parameter;
        this.cellMethods = cellMethods;
        this.interval = interval;
        this.verticalDatum = verticalDatum;
    }
}

class OikosDevice {

    public int id;
    public String discriminant;
    public OikosEnhancedParameter ep;
    public double depthMin;
    public double depthMax;

    public static OikosDevice fromJson(JSONObject j, HashMap<Integer, OikosEnhancedParameter> enhancedParameterMap) {

        return new OikosDevice(j.getInt("id"),
                               enhancedParameterMap.get(j.getInt("enhancedParameterId")),
                               j.getString("discriminant"),
                               j.getDouble("depthMin"),
                               j.getDouble("depthMax"));
    }

    public OikosDevice(int id, OikosEnhancedParameter ep, String discriminant, double depthMin, double depthMax) {
        this.id = id;
        this.ep = ep;
        this.discriminant = discriminant;
        this.depthMin = depthMin;
        this.depthMax = depthMax;
    }

    public String prettyString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.ep.parameter.name);

        if (!this.ep.cellMethods.isEmpty()) {
            sb.append("_");
            sb.append(this.ep.cellMethods);
        }
        if (!this.ep.interval.isEmpty()) {
            sb.append("_over_");
            sb.append(this.ep.interval);
        }
        if (!this.ep.verticalDatum.isEmpty()) {
            sb.append("_geoid_");
            sb.append(this.ep.verticalDatum);
        }
        if (!this.discriminant.isEmpty()) {
            sb.append("_");
            sb.append(this.discriminant);
        }

        return sb.toString();
    }
}


class OikosStation {
    public int id;
    public String label;
    public String urn;
    public double latitude;
    public double longitude;
    public int startDate;
    public int endDate;
    public ArrayList<OikosDevice> devices;

    public OikosStation(int id, String label, String urn, double latitude, double longitude, int startDate, int endDate, ArrayList<OikosDevice> devices) {
        this.id = id;
        this.label = label;
        this.urn = urn;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startDate = startDate;
        this.endDate = endDate;
        this.devices = devices;
    }

    public boolean hasDevice(int deviceId) {
        for (OikosDevice d : this.devices) {
            if (d.id == deviceId) {
                return true;
            }
        }
        return false;
    }

    public OikosDevice getDevice(int deviceId) {
        for (OikosDevice d : this.devices) {
            if (d.id == deviceId) {
                return d;
            }
        }
        return null;
    }
}


public class EDDTableFromAxiomStation extends EDDTableFromAsciiService {

    protected OikosStation station;
    private final static String STANDARD_INFO_URL    = "http://axiomdatascience.com";
    private final static String AXIOM_SENSOR_CONTACT = "data@axiomalaska.com";
    private final static String AXIOM_SENSOR_LICENSE = "Unauthorized access is punishable by the use of sophisticated internet bullying tactics";

    public static EDDTableFromAxiomStation fromXml(Erddap erddap, SimpleXMLReader xmlReader) throws Throwable {
        String tDatasetID = xmlReader.attributeValue("datasetID");
        Attributes tGlobalAttributes = null;
        String tLocalSourceUrl = null;
        int tStationId = -1;
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;
        String tSosOfferingPrefix = null;

        ArrayList<Object[]> tDataVariables = new ArrayList<>();

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
            } else if (localTags.equals("<stationId>")) {
            } else if (localTags.equals("</stationId>")) {
                tStationId = String2.parseInt(content);
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

        if (tStationId == -1) {
            throw new Exception("You must specify a <stationId> tag within a EDDTableFromAxiomStation dataset");
        }

        HashMap<Integer, OikosParameter> param_lookup = EDDTableFromAxiomStation.getParametersFromOikos();

        // Query Sensor Service to get basic metadata
        InputStream is = new URL(tLocalSourceUrl + "getDataValues?method=GetStationsResultSetRowsJSON&version=3&stationids=" + String.valueOf(tStationId)+ "&region=all&realtimeonly=false&verbose=true&jsoncallback=false").openStream();
        try {

            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            JSONObject json = new JSONObject(sb.toString());

            // Enhanced Parameters
            HashMap<Integer, OikosEnhancedParameter> ep_lookup = new HashMap<>();
            JSONObject ep_array = json.getJSONObject("enhancedParameters");
            Iterator ep_keys = ep_array.keys();
            while(ep_keys.hasNext()) {
                String k = (String) ep_keys.next();
                OikosEnhancedParameter ep = OikosEnhancedParameter.fromJson(ep_array.getJSONObject(k), param_lookup);
                ep_lookup.put(Integer.parseInt(k), ep);
            }

            // Stations
            JSONArray stations_array = json.getJSONArray("stations");
            if (stations_array.length() == 0) {
                throw new Exception("No Station with id=" + tStationId + " was found in Axiom Sensor Service.");
            }

            JSONObject stat = stations_array.getJSONObject(0);
            if (tGlobalAttributes.get("title") == null) {
                tGlobalAttributes.set("title", stat.getString("label"));
            }
            if (tGlobalAttributes.get("summary") == null) {
                tGlobalAttributes.set("summary", "Timeseries data from '"+ stat.getString("label") + "' (" + stat.getString("urn") + ")");
            }

            // Devices
            ArrayList<OikosDevice> d_list = new ArrayList<>();

            // Station Parameters
            JSONArray station_parameters = stat.getJSONArray("parameters");
            for (int j = 0 ; j < station_parameters.length() ; j++) {
                JSONObject sp = station_parameters.getJSONObject(j);
                JSONArray devices = sp.getJSONArray("devices");
                for (int k = 0 ; k < devices.length() ; k++) {
                    JSONObject dv = devices.getJSONObject(k);
                    OikosDevice device = OikosDevice.fromJson(dv, ep_lookup);
                    d_list.add(device);
                }
            }

            OikosStation station = new OikosStation(stat.getInt("id"),
                                                    stat.getString("label"),
                                                    stat.getString("urn"),
                                                    stat.getDouble("latitude"),
                                                    stat.getDouble("longitude"),
                                                    stat.getInt("startDate"),
                                                    stat.getInt("endDate"),
                                                    d_list);

            if (tGlobalAttributes.get(EDStatic.INSTITUTION) == null) {
                tGlobalAttributes.set(EDStatic.INSTITUTION, "Axiom Data Science");
            }
            if (tGlobalAttributes.get("infoUrl") == null) {
                tGlobalAttributes.set("infoUrl", STANDARD_INFO_URL);
            }
            tGlobalAttributes.set("sourceUrl", tLocalSourceUrl);
            tGlobalAttributes.set("cdm_data_type", EDD.CDM_TIMESERIES);
            tGlobalAttributes.set("geospatial_lon_min", station.longitude);
            tGlobalAttributes.set("geospatial_lon_max", station.longitude);
            tGlobalAttributes.set("geospatial_lat_min", station.latitude);
            tGlobalAttributes.set("geospatial_lat_max", station.latitude);

            // Time
            Attributes tatts = new Attributes();
            tatts.set("units", "seconds since 1970-01-01T00:00:00");
            tatts.set("ioos_category", EDV.TIME_CATEGORY);
            tatts.set("actual_range", new IntArray(new int[]{station.startDate, station.endDate}));
            tDataVariables.add(new Object[] { "time", "time", tatts, "double" });
            // Latitude
            Attributes latats = new Attributes();
            latats.set("ioos_category", EDV.LOCATION_CATEGORY);
            latats.set("actual_range", new DoubleArray(new double[]{station.latitude, station.latitude}));
            tDataVariables.add(new Object[] { "latitude", "latitude", latats, "double" });
            // Longitude
            Attributes lonats = new Attributes();
            lonats.set("ioos_category", EDV.LOCATION_CATEGORY);
            lonats.set("actual_range", new DoubleArray(new double[]{station.longitude, station.longitude}));
            tDataVariables.add(new Object[] { "longitude", "longitude", lonats, "double" });
            // Station
            Attributes staatts = new Attributes();
            staatts.set("ioos_category", "Identifier");
            staatts.set("cf_role", "timeseries_id");
            tDataVariables.add(new Object[] { "station", "station", staatts, "String" });
            // Devices
            ArrayList<String> cdm_timeseries_variables = new ArrayList<>();
            Attributes dvaatts;
            ArrayList<Double> depths = new ArrayList<>();
            for (OikosDevice d : d_list) {
                dvaatts = new Attributes();
                dvaatts.set("standard_name", d.ep.parameter.name);
                dvaatts.set("long_name", d.ep.parameter.label);
                dvaatts.set("units", d.ep.parameter.defaultUnit().unit);
                dvaatts.set("ioos_category", "Other");
                dvaatts.set("urn", d.ep.parameter.urn);
                dvaatts.set("missing_value", -9999.99);
                dvaatts.set("_FillValue", -9999.99);
                depths.add(Double.valueOf(d.depthMin));
                depths.add(Double.valueOf(d.depthMax));
                if (!d.ep.cellMethods.isEmpty()) {
                    dvaatts.set("cell_methods", d.ep.cellMethods);
                }
                if (!d.ep.interval.isEmpty()) {
                    dvaatts.set("interval", d.ep.interval);
                }
                if (!d.ep.verticalDatum.isEmpty()) {
                    dvaatts.set("vertical_datum", d.ep.verticalDatum);
                }
                if (!d.ep.verticalDatum.isEmpty()) {
                    dvaatts.set("discriminant", d.discriminant);
                }
                tDataVariables.add(new Object[] { d.prettyString(), d.prettyString(), dvaatts, "double" });
                cdm_timeseries_variables.add(d.prettyString());
            }

            // Depth
            Attributes depatts = new Attributes();
            depatts.set("ioos_category", EDV.LOCATION_CATEGORY);
            depatts.set("units", "m");
            depatts.set("positive", "down");
            depatts.set("actual_range", new DoubleArray(new double[]{Collections.min(depths), Collections.max(depths)}));
            tDataVariables.add(new Object[] { "depth", "depth", depatts, "double" });

            String2.log(String.join(",", cdm_timeseries_variables));
            tGlobalAttributes.set("cdm_timeseries_variables", String.join(",", cdm_timeseries_variables));

            int ndv = tDataVariables.size();
            Object ttDataVariables[][] = new Object[ndv][];
            for (int i = 0; i < tDataVariables.size(); i++) {
                ttDataVariables[i] = tDataVariables.get(i);
            }

            return new EDDTableFromAxiomStation(tDatasetID, tSosOfferingPrefix, tDefaultDataQuery, tDefaultGraphQuery,
                    tGlobalAttributes, ttDataVariables, tReloadEveryNMinutes, tLocalSourceUrl, station);

        } finally { is.close(); }
    }

    public EDDTableFromAxiomStation(String tDatasetID, String tSosOfferingPrefix, String tDefaultDataQuery,
                                    String tDefaultGraphQuery, Attributes tAddGlobalAttributes,
                                    Object[][] tDataVariables, int tReloadEveryNMinutes, String tLocalSourceUrl,
                                    OikosStation stat) throws Throwable {

        super("EDDTableFromAxiomStation", tDatasetID, null, new StringArray(), null, null,
                tSosOfferingPrefix, tDefaultDataQuery, tDefaultGraphQuery, tAddGlobalAttributes,
                tDataVariables, tReloadEveryNMinutes, tLocalSourceUrl, null, null, null);
        this.station = stat;
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

        String2.log("userDapQuery: " + userDapQuery);
        String2.log("resultsVariables: " + resultsVariables.toCSVString());
        String2.log("constraintVariables: " + constraintVariables.toCSVString());
        String2.log("constraintOps: " + constraintOps.toCSVString());
        String2.log("constraintValues: " + constraintValues.toCSVString());

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
        }

        // Get the data requested
        boolean errorPrinted = false;

        ArrayList<String> parameter_id_builder = new ArrayList<>();
        ArrayList<String> units_builder = new ArrayList<>();
        for ( OikosDevice d : this.station.devices ) {
            OikosUnit u = d.ep.parameter.defaultUnit();
            units_builder.add(u.id + ";" + u.unit);
            parameter_id_builder.add(String.valueOf(d.ep.parameter.id));
        }

        // URL to get data
        String encodedSourceUrl = localSourceUrl + "getDataValues" +
                "?stationid=" + this.station.id +
                "&parameterids=" + URLEncoder.encode(SSR.minimalPercentEncode(String.join(",", parameter_id_builder)), "UTF-8") +
                "&units=" + URLEncoder.encode(SSR.minimalPercentEncode(String.join(",", units_builder)), "UTF-8") +
                "&start_time=" + URLEncoder.encode(String.valueOf((int)beginSeconds), "UTF-8") +
                "&end_time="   + URLEncoder.encode(String.valueOf((int)endSeconds), "UTF-8") +
                "&jsoncallback=false" +
                "&version=3" +
                "&force_binned_data=false" +
                "&method=GetSensorObservationsJSON";
        InputStream is = new URL(encodedSourceUrl).openStream();
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder strb = new StringBuilder();
            String str;
            while ((str = in.readLine()) != null) {
                strb.append(str);
            }
            JSONObject results = new JSONArray(strb.toString()).getJSONObject(0);
            JSONArray data = results.getJSONArray("data");

            Table table = new Table();

            // Fill the station_urn column
            StringArray stat = new StringArray();
            table.addColumn("station", stat);

            // Fill the longitude column
            DoubleArray lons_array = new DoubleArray();
            table.addColumn("longitude", lons_array);

            // Fill the latitude column
            DoubleArray lats_array = new DoubleArray();
            table.addColumn("latitude", lats_array);

            IntArray actual_times_array = new IntArray();
            DoubleArray depth_array  = new DoubleArray();

            for (int c = 0 ; c < data.length() ; c++) {
                JSONObject vari = data.getJSONObject(c);
                if (vari.getJSONObject("metadata").getString("label").equals("time")) {
                    // TIME
                    table.addColumn("time", actual_times_array);
                    JSONArray values = vari.getJSONObject("variableValueCollection").getJSONArray("values");
                    for (int d = 0 ; d < values.length() ; d++) {
                        actual_times_array.add(values.getInt(d));
                    }
                } else {
                    // DATA
                    if (this.station.hasDevice(vari.getJSONObject("metadata").getInt("device_id"))) {
                        // DESIRED DEVICE
                        OikosDevice od = this.station.getDevice(vari.getJSONObject("metadata").getInt("device_id"));

                        // Using a Double here resulted in crazy sigfigs.
                        FloatArray values_array = new FloatArray();

                        String unitString = vari.getJSONObject("metadata").getString("unit");
                        String columnName = od.prettyString();
                        String columnDepthName = "depth";

                        try {
                            // Column already exists, so we have multiple depths.
                            table.getColumn(columnName);
                            table.getColumn(columnDepthName);
                        } catch (IllegalArgumentException e) {
                            // Column does not exist yet, so we create it
                            table.addColumn(columnName, values_array);
                            table.addColumn(columnDepthName, depth_array);
                        }

                        JSONArray values = vari.getJSONObject("variableValueCollection").getJSONArray("values");
                        for (int e = 0 ; e < values.length() ; e++) {
                            try {
                                values_array.add(new Float(values.getDouble(e)));
                            } catch (JSONException ex) {
                                // Most likely the "null" string which is used for a fill value in the sensor service.
                                values_array.add(new Float(-9999.99));
                            }
                        }
                        // TODO: This will only work if each device is measured at the same depths
                        // If not, the indexes will be all messed up.  We should probably error here, or
                        // Add a column for each device depth value.
                        Double depth = vari.getJSONObject("metadata").getDouble("depth");
                        if (depth_array.indexOf(depth) == -1) {
                            depth_array.addN(values_array.size(), depth);
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
            stat.addN(nRows, this.station.label);

            // Fill the longitude column
            lons_array.addNDoubles(nRows, this.station.longitude);

            // Fill the latitude column
            lats_array.addNDoubles(nRows, this.station.latitude);

            standardizeResultsTable(requestUrl, userDapQuery, table);

            if (table.nRows() > 0) {
                tableWriter.writeSome(table);
            }

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);
            if (verbose) {
                String2.log(String2.ERROR + " for stationID=" + this.station.id +
                        (errorPrinted? "" :
                                encodedSourceUrl + "\n" + MustBe.throwableToString(t)));
                errorPrinted = true;
            }
        } finally { is.close(); }

        if (tableWriter.noMoreDataPlease) {
            tableWriter.logCaughtNoMoreDataPlease(datasetID);
        }

        tableWriter.finish();
        if (reallyVerbose) {
            String2.log("\nFinished getDataForDapQuery");
        }
    }

    private static HashMap<Integer, OikosParameter> getParametersFromOikos() throws IOException{
        URL url = new URL("http://pdx0.axiomalaska.com/oikos-service/rest/minimal-portal-data");
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
                strbf.append(ln);
                strbf.append("\n");
            }
            JSONObject param_json = new JSONObject(strbf.toString());

            // Units
            HashMap<Integer,OikosUnit> units_lookup = new HashMap<>();
            JSONArray units_array = param_json.getJSONArray("units");
            for (int a = 0 ; a < units_array.length() ; a++) {
                JSONObject unit = units_array.getJSONObject(a);
                units_lookup.put(unit.getInt("id"), OikosUnit.fromJson(unit));
            }

            // ParameterTypeUnits
            HashMap<Integer,ArrayList<OikosParameterTypeUnit>> parameter_type_lookup = new HashMap<>();
            JSONArray param_type_units_array = param_json.getJSONArray("parameterTypeUnits");
            for (int b = 0 ; b < param_type_units_array.length() ; b++) {
                JSONObject param = param_type_units_array.getJSONObject(b);

                OikosParameterTypeUnit ptu = OikosParameterTypeUnit.fromJson(param, units_lookup);

                if (!parameter_type_lookup.containsKey(ptu.id)) {
                    // Create array
                    ArrayList<OikosParameterTypeUnit> ptus = new ArrayList<>();
                    ptus.add(ptu);
                    parameter_type_lookup.put(ptu.id, ptus);
                } else {
                    ArrayList<OikosParameterTypeUnit> ptus = parameter_type_lookup.get(ptu.id);
                    ptus.add(ptu);
                    parameter_type_lookup.replace(ptu.id, ptus);
                }
            }

            // Parameters
            HashMap<Integer, OikosParameter> p_lookup = new HashMap<>();
            JSONArray param_array = param_json.getJSONArray("parameters");
            for (int c = 0 ; c < param_array.length() ; c++) {
                JSONObject param = param_array.getJSONObject(c);
                OikosParameter p = OikosParameter.fromJson(param, parameter_type_lookup);
                p_lookup.put(p.id, p);
            }

            return p_lookup;

        } finally { ps.close(); }
    }

    public static void main(String[] args) throws Throwable {
        String2.log("\n****************** EDDTableFromAxiomStation.test() *****************\n");
        testVerboseOn();

        EDD edd = EDD.oneFromXmlFragment(null, "" +
                "<dataset type=\"EDDTableFromAxiomStation\" datasetID=\"station_test\">\n" +
                "    <sourceUrl>http://pdx.axiomalaska.com/stationsensorservice/</sourceUrl>\n" +
                "    <stationId>57422</stationId>\n" +
                "</dataset>"
        );
        // Test specific station and sensor
        String query = "air_temperature,dew_point_temperature,time,latitude,longitude,depth&time>=2015-12-14T00:00:00Z&time<=2015-12-15T00:00:00Z";
        String  tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory,
                edd.className() + "_station_sensor_" + edd.datasetID(), ".csv");
        String results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);
    }
}
