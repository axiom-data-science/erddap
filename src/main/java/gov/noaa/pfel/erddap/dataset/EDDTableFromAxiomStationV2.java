package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.variable.EDV;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static gov.noaa.pfel.erddap.dataset.EDDTableFromAxiomStationV2Utils.secondsSinceEpochFromUTCString;


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
    public int groupId;
    public ArrayList<OikosParameterTypeUnit> parameterTypeUnits;
    public String urn;
    public String name;
    public String label;

    public static OikosParameter fromJson(JSONObject j, HashMap<Integer, ArrayList<OikosParameterTypeUnit>> parameterTypeUnitMap) {

        ArrayList<OikosParameterTypeUnit> ptms;

        if (!j.isNull("idParameterType")) {
            ptms = parameterTypeUnitMap.get(j.getInt("idParameterType"));
            if (ptms == null) {
                OikosUnit un = new OikosUnit(-1, "unknown", "unknown", "NON_STANDARD");
                OikosParameterTypeUnit ptm = new OikosParameterTypeUnit(-1, un, true, true);
                ptms = new ArrayList<>();
                ptms.add(ptm);
            }
        } else {
            OikosUnit un = new OikosUnit(-1, "unknown", "unknown", "NON_STANDARD");
            OikosParameterTypeUnit ptm = new OikosParameterTypeUnit(-1, un, true, true);
            ptms = new ArrayList<>();
            ptms.add(ptm);
        }

        return new OikosParameter(j.getInt("id"),
                j.getInt("idParameterGroup"),
                ptms,
                j.getString("urn"),
                j.getString("parameterName"),
                j.getString("label"));
    }

    public OikosParameter(int id, int groupId, ArrayList<OikosParameterTypeUnit> parameterTypeUnits, String urn, String name, String label) {
        this.id = id;
        this.groupId = groupId;
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

class OikosSensorParameter {

    public int id;
    public OikosParameter parameter;
    public OikosUnit unit;
    public String cellMethods;
    public String interval;
    public String verticalDatum;

    public static OikosSensorParameter fromJson(JSONObject j, HashMap<Integer, OikosParameter> parameterMap, HashMap<Integer, OikosUnit> unitMap) {

        return new OikosSensorParameter(j.getInt("id"),
                parameterMap.get(j.getInt("parameterId")),
                unitMap.get(j.getInt("unitId")),
                j.getString("cellMethods"),
                j.getString("timeInterval"),
                j.getString("verticalDatum"));
    }

    public OikosSensorParameter(int id, OikosParameter parameter, OikosUnit unit, String cellMethods, String interval, String verticalDatum) {
        this.id = id;
        this.parameter = parameter;
        this.unit = unit;
        this.cellMethods = cellMethods;
        this.interval = interval;
        this.verticalDatum = verticalDatum;
    }
}

class OikosDeviceFeed {

    public int id;
    public String discriminant;
    public OikosSensorParameter sp;
    public double minZ;
    public double maxZ;

    public static OikosDeviceFeed fromJson(JSONObject j, HashMap<Integer, OikosSensorParameter> sensorParameterMap) {

        double minZ = j.getDouble("minZ");
        double maxZ = j.getDouble("maxZ");
        String discriminant = j.get("discriminant") == JSONObject.NULL ? "" : j.getString("discriminant");

        return new OikosDeviceFeed(j.getInt("id"),
                sensorParameterMap.get(j.getInt("sensorParameterId")),
                discriminant,
                minZ,
                maxZ);
    }

    public OikosDeviceFeed(int id, OikosSensorParameter sp, String discriminant, double minZ, double maxZ) {
        this.id = id;
        this.sp = sp;
        this.discriminant = discriminant;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    private static String cleanup(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.toLowerCase()
                .replaceAll(" ", "_")
                .replaceAll("[^a-zA-Z0-9_]", "_");
    }

    public String prettyString() {
        StringBuilder sb = new StringBuilder();

        if (this.sp.parameter.name.equals("depth")) {
            sb.append("depth_reading");
        } else {
            sb.append(cleanup(this.sp.parameter.name));
        }

        if (!this.sp.cellMethods.isEmpty()) {
            sb.append("_cm_");
            sb.append(cleanup(this.sp.cellMethods));
        }
        if (!this.sp.interval.isEmpty()) {
            sb.append("_over_");
            sb.append(cleanup(this.sp.interval));
        }
        if (!this.sp.verticalDatum.isEmpty()) {
            sb.append("_geoid_");
            sb.append(cleanup(this.sp.verticalDatum));
        }
        if (!this.discriminant.isEmpty()) {
            sb.append("_");
            sb.append(cleanup(this.discriminant));
        }

        return sb.toString();
    }
}

class OikosAgent {
    public int id;
    public String label;
    public String uuid;
    public String sectorType;
    public String url;
    public String email;
    public String country;

    public static OikosAgent fromJson(JSONObject j) {
        String url = j.get("url") == JSONObject.NULL ? "" : j.getString("url");
        String email = j.get("contact") == JSONObject.NULL ? "" : j.getString("contact");
        String country = j.isNull("country") ? "" :
                (j.get("country") == JSONObject.NULL ? "" : j.getString("country"));
        return new OikosAgent(
                j.getInt("id"),
                j.getString("label"),
                j.getString("slug"),
                j.getString("sectorType"),
                url,
                email,
                country);
    }

    public OikosAgent(int id, String label, String uuid, String sectorType, String url, String email, String country) {
        this.id = id;
        this.label = label;
        this.uuid = uuid;
        this.sectorType = sectorType;
        this.url = url;
        this.email = email;
        this.country = country;
    }
}

class OikosStationAgentAffiliation {
    public String type;
    public String role;
    public OikosAgent agent;
    public String foreignName;
    public String foreignUrl;

    public static OikosStationAgentAffiliation fromJson(JSONObject j, OikosAgent agent,
                                                        HashMap<String, String> agentAssociationTypeToRoleCodeMap) {
        String type = j.getString("type");
        String role = agentAssociationTypeToRoleCodeMap.get(type);
        return new OikosStationAgentAffiliation(
                type,
                role,
                agent,
                j.isNull("foreignName") ? "" : j.getString("foreignName"),
                j.isNull("foreignUrl") ? "" : j.getString("foreignUrl")
        );
    }

    public OikosStationAgentAffiliation(String type, String role, OikosAgent agent, String foreignName, String foreignUrl) {
        this.type = type;
        this.role = role;
        this.agent = agent;
        this.foreignName = foreignName;
        this.foreignUrl = foreignUrl;
    }
}

class OikosStation {
    public int id;
    public String label;
    public String urn;
    public String wmoId;
    public String platformType;
    public String qcInfoUrl;
    public double latitude;
    public double longitude;
    public int startDate;
    public int endDate;
    public double minZ;
    public double maxZ;
    public String archivePath;
    public boolean submitToNdbc;

    public OikosStationAgentAffiliation creator;
    public OikosStationAgentAffiliation publisher;
    public List<OikosStationAgentAffiliation> contributors;

    public ArrayList<OikosDeviceFeed> deviceFeeds;

    public static OikosStation fromJson(JSONObject j,
                                        ArrayList<OikosDeviceFeed> deviceFeeds,
                                        HashMap<Integer, OikosAgent> agentMap,
                                        HashMap<String, String> agentAssociationTypeToRoleCodeMap) {

        int id = j.getInt("id");

        JSONArray location = j.getJSONObject("location").getJSONArray("coordinates");
        double latitude = location.getDouble(1);
        double longitude = location.getDouble(0);

        JSONObject stats = j.getJSONObject("feedStats");
        int startDate = secondsSinceEpochFromUTCString(stats.getString("startDate"));
        int endDate = secondsSinceEpochFromUTCString(stats.getString("endDate"));

        String qcInfoUrl = stats.isNull("qcInfoUrl") ? "" : stats.getString("qcInfoUrl");

        String archivePath = stats.getString("archivePath");

        double minZ = deviceFeeds.stream().map(df -> df.minZ).min(Double::compareTo).get();
        double maxZ = deviceFeeds.stream().map(df -> df.maxZ).max(Double::compareTo).get();

        OikosStationAgentAffiliation creator = null, publisher = null;
        String wmoId = null;
        List<OikosStationAgentAffiliation> contributors = new ArrayList<>();
        JSONArray affiliations = j.getJSONArray("affiliations");
        for (int i = 0; i < affiliations.length(); i++) {
            JSONObject affiliationObj = affiliations.getJSONObject(i);

            int agentId = affiliationObj.getInt("agentId");
            OikosAgent agent = agentMap.get(agentId);

            OikosStationAgentAffiliation affiliation = OikosStationAgentAffiliation.fromJson(affiliationObj, agent, agentAssociationTypeToRoleCodeMap);
            if ("owner".equalsIgnoreCase(affiliation.type)) {
                creator = affiliation;
            } else if ("publisher".equalsIgnoreCase(affiliation.type)) {
                publisher = affiliation;
            } else {
                contributors.add(affiliation);
            }

            if (OikosLookups.WMO_AGENT_UUID.equals(agent.uuid)) {
                wmoId = affiliation.foreignName;
            }
        }

        boolean submitToNdbc = false;
        JSONArray tagsArr = j.getJSONArray("tags");
        for (int i = 0; i < tagsArr.length(); i++) {
            String tag = (String) tagsArr.get(i);
            if ("submit_to_ndbc".equals(tag)) {
                submitToNdbc = true;
            }
        }

        return new OikosStation(id,
                j.getString("label"),
                j.getString("uuid"),
                wmoId,
                j.getString("platformType"),
                qcInfoUrl,
                latitude,
                longitude,
                startDate,
                endDate,
                minZ, maxZ,
                archivePath,
                submitToNdbc,
                creator,
                publisher,
                contributors,
                deviceFeeds);

    }

    public OikosStation(int id, String label, String urn, String wmoId, String platformType, String qcInfoUrl,
                        double latitude, double longitude,
                        int startDate, int endDate, double minZ, double maxZ,
                        String archivePath, boolean submitToNdbc, OikosStationAgentAffiliation creator,
                        OikosStationAgentAffiliation publisher,
                        List<OikosStationAgentAffiliation> contributors,
                        ArrayList<OikosDeviceFeed> deviceFeeds) {
        this.id = id;
        this.label = label;
        this.urn = urn;
        this.wmoId = wmoId;
        this.platformType = platformType;
        this.qcInfoUrl = qcInfoUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startDate = startDate;
        this.endDate = endDate;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.archivePath = archivePath;
        this.submitToNdbc = submitToNdbc;
        this.creator = creator;
        this.publisher = publisher;
        this.contributors = contributors;
        this.deviceFeeds = deviceFeeds;
    }

    public boolean hasDeviceFeed(int deviceFeedId) {
        for (OikosDeviceFeed d : this.deviceFeeds) {
            if (d.id == deviceFeedId) {
                return true;
            }
        }
        return false;
    }

    public OikosDeviceFeed getDeviceFeed(int deviceFeedId) {
        for (OikosDeviceFeed d : this.deviceFeeds) {
            if (d.id == deviceFeedId) {
                return d;
            }
        }
        return null;
    }
}

class OikosLookups {
    HashMap<Integer, OikosSensorParameter> sensorParameterMap;
    HashMap<Integer, OikosParameter> parameterMap;
    HashMap<Integer, OikosUnit> unitMap;
    HashMap<Integer, OikosAgent> agentMap;
    HashMap<String, String> agentAssociationTypeToRoleCodeMap;
    static final String WMO_AGENT_UUID = "un.wmo";
    static final Set<String> STANDARD_NAMES_TO_SUBMIT_TO_NDBC = new HashSet<>(Arrays.asList(
            "air_pressure",
            "air_temperature",
            "depth",
            "dew_point_temperature",
            "downwelling_longwave_flux_in_air",
            "eastward_sea_water_velocity",
            "fractional_saturation_of_oxygen_in_sea_water",
            "lwe_thickness_of_precipitation_amount",
            "mass_concentration_of_chlorophyll_in_sea_water",
            "mass_concentration_of_oxygen_in_sea_water",
            "northward_sea_water_velocity",
            "relative_humidity",
            "sea_surface_dominant_wave_period",
            "sea_surface_height_above_sea_level",
            "sea_surface_wave_from_direction",
            "sea_surface_wave_significant_height",
            "sea_water_ph_reported_on_total_scale",
            "sea_water_practical_salinity",
            "sea_water_temperature",
            "sea_water_turbidity",
            "short_wave_radiation",
            "surface_downwelling_photosynthetic_radiative_flux_in_air",
            "turbidity",
            "wind_from_direction",
            "wind_speed",
            "wind_speed_of_gust"
    ));

    public OikosLookups(HashMap<Integer, OikosSensorParameter> sensorParameterMap,
                        HashMap<Integer, OikosParameter> parameterMap,
                        HashMap<Integer, OikosUnit> unitMap,
                        HashMap<Integer, OikosAgent> agentMap,
                        HashMap<String, String> agentAssociationTypeToRoleCodeMap) {
        this.sensorParameterMap = sensorParameterMap;
        this.parameterMap = parameterMap;
        this.unitMap = unitMap;
        this.agentMap = agentMap;
        this.agentAssociationTypeToRoleCodeMap = agentAssociationTypeToRoleCodeMap;
    }

    static OikosLookups getOikosLookups() throws IOException {
        URL url = new URL("http://oikos.axds.co/rest/context");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.addRequestProperty("Accept", "application/json");
        conn.setUseCaches(false);
        InputStream ps = conn.getInputStream();
        JSONObject param_json;
        try {
            BufferedReader bfrd = new BufferedReader(
                    new InputStreamReader(ps, Charset.forName("UTF-8")));
            StringBuilder strbf = new StringBuilder();
            String ln = null;
            while ((ln = bfrd.readLine()) != null) {
                strbf.append(ln);
                strbf.append("\n");
            }
            param_json = new JSONObject(strbf.toString());

        } finally {
            ps.close();
        }

        return mapOikosLookupsFromJson(param_json);
    }

    static OikosLookups mapOikosLookupsFromJson(JSONObject lookups_json) {
        // Units
        HashMap<Integer, OikosUnit> unitsLookup = new HashMap<>();
        JSONArray units_array = lookups_json.getJSONArray("units");
        for (int a = 0; a < units_array.length(); a++) {
            JSONObject unit = units_array.getJSONObject(a);
            unitsLookup.put(unit.getInt("id"), OikosUnit.fromJson(unit));
        }

        // ParameterTypeUnits
        HashMap<Integer, ArrayList<OikosParameterTypeUnit>> parameter_type_lookup = new HashMap<>();
        JSONArray param_type_units_array = lookups_json.getJSONArray("parameterTypeUnits");
        for (int b = 0; b < param_type_units_array.length(); b++) {
            JSONObject param = param_type_units_array.getJSONObject(b);

            OikosParameterTypeUnit ptu = OikosParameterTypeUnit.fromJson(param, unitsLookup);

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
        HashMap<Integer, OikosParameter> paramLookup = new HashMap<>();
        JSONArray param_array = lookups_json.getJSONArray("parameters");
        for (int c = 0; c < param_array.length(); c++) {
            JSONObject param = param_array.getJSONObject(c);
            OikosParameter p = OikosParameter.fromJson(param, parameter_type_lookup);
            paramLookup.put(p.id, p);
        }

        // Sensor Parameters
        HashMap<Integer, OikosSensorParameter> sensorParamLookup = new HashMap<>();
        JSONArray sensor_param_array = lookups_json.getJSONArray("sensorParameters");
        for (int c = 0; c < sensor_param_array.length(); c++) {
            JSONObject param = sensor_param_array.getJSONObject(c);
            OikosSensorParameter sp = OikosSensorParameter.fromJson(param, paramLookup, unitsLookup);
            sensorParamLookup.put(sp.id, sp);
        }

        // Agents
        HashMap<Integer, OikosAgent> agentLookup = new HashMap<>();
        JSONArray agent_array = lookups_json.getJSONArray("agents");
        for (int c = 0; c < agent_array.length(); c++) {
            JSONObject agent = agent_array.getJSONObject(c);
            OikosAgent a = OikosAgent.fromJson(agent);
            agentLookup.put(a.id, a);
        }

        HashMap<String, String> agentTypeToRoleCode = new HashMap<>();
        JSONArray typesArray = lookups_json.getJSONArray("agentAssociationTypes");
        for (int c = 0; c < typesArray.length(); c++) {
            JSONObject type = typesArray.getJSONObject(c);
            String name = type.getString("name");
            // TODO: once oikos is in prod, all of these should have roleCode defined
            String roleCode = type.isNull("roleCode") ? name : type.getString("roleCode");
            agentTypeToRoleCode.put(name, roleCode);
        }

        return new OikosLookups(sensorParamLookup, paramLookup, unitsLookup, agentLookup, agentTypeToRoleCode);
    }
}

class OikosSensorFilter {
    List<Integer> stations;
    List<Integer> parameterGroupIds;

    public OikosSensorFilter(int stationId) {
        this(stationId, newArrayList());
    }

    public OikosSensorFilter(int stationId, List<Integer> parameterGroupIds) {
        this.stations = newArrayList(stationId);
        this.parameterGroupIds = parameterGroupIds;
    }

    @Override
    public String toString() {
        return "{" +
                "\"stations\":" + this.stations +
                ",\"parameterGroups\":" + this.parameterGroupIds +
                '}';
    }

    public String toUrlEncodedString() throws UnsupportedEncodingException {
        String s = this.toString();
        s = s.replaceAll(" ", "");
        return URLEncoder.encode(s, "UTF-8");
    }
}

class EDDTableFromAxiomStationV2Utils {

    static int secondsSinceEpochFromUTCString(String string) {
        return (int) Instant.parse(string).getEpochSecond();
    }

    static String UTCStringFromSecondsSinceEpoch(int secs) {
        return Instant.ofEpochSecond(secs).toString();
    }

    /**
     * This function maps metadata from sensor service to the dataset.
     * It follows IOOS Metadata Profile v1.2.
     *
     * @param tGlobalAttributes This variable will be populated with global attributes
     * @param tStationId        Station ID
     * @param tDataVariables    This variable will be populated with all the data variables
     * @param oikosLookups
     * @param json              Metadata from sensor service
     * @return Metadata for this station
     */
    static OikosStation mapMetadataForOikosStation(Attributes tGlobalAttributes, int tStationId, ArrayList<Object[]> tDataVariables, OikosLookups oikosLookups, JSONObject json) throws Exception {

        JSONObject metadata = json.getJSONObject("data");

        // Extract station
        JSONArray stations_array = metadata.getJSONArray("stations");
        if (stations_array.length() == 0) {
            throw new Exception("No Station with id=" + tStationId + " was found in Axiom Sensor Service.");
        }
        JSONObject stationJson = stations_array.getJSONObject(0);

        // Extract device feeds
        ArrayList<OikosDeviceFeed> df_list = new ArrayList<>();
        JSONArray device_feeds = metadata.getJSONArray("deviceFeeds");
        for (int j = 0; j < device_feeds.length(); j++) {
            JSONObject df = device_feeds.getJSONObject(j);
            OikosDeviceFeed device_feed = OikosDeviceFeed.fromJson(df, oikosLookups.sensorParameterMap);
            df_list.add(device_feed);
        }

        OikosStation station = OikosStation.fromJson(stationJson, df_list, oikosLookups.agentMap, oikosLookups.agentAssociationTypeToRoleCodeMap);

        // DATA VARIABLES

        // Time
        Attributes tatts = new Attributes();
        tatts.set("units", "seconds since 1970-01-01T00:00:00");
        tatts.set("axis", "T");
        tatts.set("ioos_category", EDV.TIME_CATEGORY);
        tatts.set("actual_range", new IntArray(new int[]{station.startDate, station.endDate}));
        tDataVariables.add(new Object[]{"time", "time", tatts, "double"});
        // Latitude
        Attributes latats = new Attributes();
        latats.set("axis", "Y");
        latats.set("ioos_category", EDV.LOCATION_CATEGORY);
        latats.set("actual_range", new DoubleArray(new double[]{station.latitude, station.latitude}));
        tDataVariables.add(new Object[]{"lat", "latitude", latats, "double"});
        // Longitude
        Attributes lonats = new Attributes();
        lonats.set("axis", "X");
        lonats.set("ioos_category", EDV.LOCATION_CATEGORY);
        lonats.set("actual_range", new DoubleArray(new double[]{station.longitude, station.longitude}));
        tDataVariables.add(new Object[]{"lon", "longitude", lonats, "double"});
        // Z
        Attributes zatts = new Attributes();
        zatts.set("axis", "Z");
        zatts.set("ioos_category", EDV.LOCATION_CATEGORY);
        zatts.set("units", "m");
        zatts.set("positive", "up");
        zatts.set("actual_range", new DoubleArray(new double[]{station.minZ, station.maxZ}));
        tDataVariables.add(new Object[]{"z", "z", zatts, "double"});

        // Device feeds
        ArrayList<String> cdm_timeseries_variables = new ArrayList<>();
        Attributes dvaatts;
        for (OikosDeviceFeed d : df_list) {
            dvaatts = new Attributes();
            String variableStandardName = d.sp.parameter.name;
            dvaatts.set("standard_name", variableStandardName);
            dvaatts.set("long_name", d.sp.parameter.label);
            dvaatts.set("id", String.valueOf(d.id));
            dvaatts.set("units", d.sp.unit.unit);
            dvaatts.set("ioos_category", "Other");
            dvaatts.set("platform", "station");
            dvaatts.set("urn", d.sp.parameter.urn);
            dvaatts.set("missing_value", -9999);
            dvaatts.set("_FillValue", -9999);
            if (!d.sp.cellMethods.isEmpty()) {
                dvaatts.set("cell_methods", d.sp.cellMethods);
            }
            if (!d.sp.interval.isEmpty()) {
                dvaatts.set("interval", d.sp.interval);
            }
            if (!d.sp.verticalDatum.isEmpty()) {
                dvaatts.set("vertical_datum", d.sp.verticalDatum);
            }
            if (!d.discriminant.isEmpty()) {
                dvaatts.set("discriminant", d.discriminant);
            }
            if (station.submitToNdbc && OikosLookups.STANDARD_NAMES_TO_SUBMIT_TO_NDBC.contains(variableStandardName)) {
                dvaatts.set("gts_ingest", "true");
            }

            tDataVariables.add(new Object[]{"value_" + d.id, d.prettyString(), dvaatts, "double"});
            cdm_timeseries_variables.add(d.prettyString());

            // QARTOD
            // See Appendix B in https://cdn.ioos.noaa.gov/media/2017/12/QARTOD-Data-Flags-Manual_Final_version1.1.pdf

            Attributes qcAggAtts = new Attributes();
            qcAggAtts.set("standard_name", variableStandardName + " status_flag");
            qcAggAtts.set("long_name", d.sp.parameter.label + " QARTOD Aggregate Flag");
            qcAggAtts.set("ioos_category", "Other");
            qcAggAtts.set("flag_values", "1, 2, 3, 4, 9");
            qcAggAtts.set("flag_meanings", "PASS NOT_EVALUATED SUSPECT FAIL MISSING");
            qcAggAtts.set("missing_value", 9);
            qcAggAtts.set("_FillValue", 9);
            String qcAggVarName = d.prettyString() + "_qc_agg";
            tDataVariables.add(new Object[]{"qc_agg_" + d.id, qcAggVarName, qcAggAtts, "int"});

            Attributes qcTestAtts = new Attributes();
            qcTestAtts.set("standard_name", variableStandardName + " status_flag");
            qcTestAtts.set("long_name", d.sp.parameter.label + " QARTOD Individual Tests");
            qcTestAtts.set("ioos_category", "Other");
            qcTestAtts.set("flag_values", "1, 2, 3, 4, 9");
            qcTestAtts.set("flag_meanings", "PASS NOT_EVALUATED SUSPECT FAIL MISSING");
            qcTestAtts.set("comment", "11-character string with results of individual QARTOD tests. " +
                    "1: Gap Test, 2: Syntax Test, 3: Location Test, 4: Gross Range Test, 5: Climatology Test, " +
                    "6: Spike Test, 7: Rate of Change Test, 8: Flat-line Test, 9: Multi-variate Test, " +
                    "10: Attenuated Signal Test, 11: Neighbor Test");
            qcTestAtts.set("missing_value", "");
            qcTestAtts.set("_FillValue", "");
            String qcTestsVarName = d.prettyString() + "_qc_tests";
            tDataVariables.add(new Object[]{"qc_tests_" + d.id, qcTestsVarName, qcTestAtts, "String"});

            dvaatts.set("ancillary_variables", qcAggVarName + " " + qcTestsVarName);
        }


        // GLOBAL ATTRIBUTES

        if (tGlobalAttributes.get("title") == null) {
            tGlobalAttributes.set("title", station.label);
        }
        tGlobalAttributes.set("summary", "Timeseries data from '" + station.label + "' (" + station.urn + ")");

        tGlobalAttributes.set("naming_authority", "com.axiomdatascience");
        tGlobalAttributes.set("id", station.id);

        tGlobalAttributes.set("platform", station.platformType);
        tGlobalAttributes.set("platform_vocabulary", "http://mmisw.org/ont/ioos/platform");

        tGlobalAttributes.set("featureType", "timeSeries");

        tGlobalAttributes.set("Conventions", "IOOS-1.2, CF-1.6, ACDD-1.3");

        tGlobalAttributes.set("standard_name_vocabulary", "NetCDF Climate and Forecast (CF) Metadata Convention Standard Name");

        Attributes stationatts = new Attributes();
        stationatts.set("ioos_code", "urn:ioos:station:com.axiomdatascience:" + station.id);
        stationatts.set("ioos_category", "Identifier");
        stationatts.set("cf_role", "timeseries_id");
        stationatts.set("long_name", station.label);
        stationatts.set("short_name", station.urn);
        stationatts.set("type", station.platformType);
        tDataVariables.add(new Object[]{"station", "station", stationatts, "String"});

        String infoUrl = "https://sensors.ioos.us/?sensor_version=v2#metadata/" + station.id + "/station";
        tGlobalAttributes.set("infoUrl", infoUrl);
        tGlobalAttributes.set("info_url", infoUrl);

        tGlobalAttributes.set("geospatial_lon_min", station.longitude);
        tGlobalAttributes.set("geospatial_lon_max", station.longitude);
        tGlobalAttributes.set("geospatial_lon_units", "degrees_east");
        tGlobalAttributes.set("geospatial_lat_min", station.latitude);
        tGlobalAttributes.set("geospatial_lat_max", station.latitude);
        tGlobalAttributes.set("geospatial_lat_units", "degrees_north");
        tGlobalAttributes.set("geospatial_vertical_min", station.minZ);
        tGlobalAttributes.set("geospatial_vertical_max", station.maxZ);
        tGlobalAttributes.set("geospatial_vertical_units", "m");
        tGlobalAttributes.set("geospatial_vertical_positive", "up");

        tGlobalAttributes.set("institution", station.creator.agent.label);
        tGlobalAttributes.set("creator_name", station.creator.agent.label);
        tGlobalAttributes.set("creator_institution", station.creator.agent.label);
        tGlobalAttributes.set("creator_email", station.creator.agent.email);
        tGlobalAttributes.set("creator_country", station.creator.agent.country);
        tGlobalAttributes.set("creator_sector", station.creator.agent.sectorType);
        tGlobalAttributes.set("creator_url", station.creator.agent.url);
        tGlobalAttributes.set("creator_type", "institution");

        tGlobalAttributes.set("publisher_name", station.publisher.agent.label);
        tGlobalAttributes.set("publisher_institution", station.publisher.agent.label);
        tGlobalAttributes.set("publisher_email", station.publisher.agent.email);
        tGlobalAttributes.set("publisher_country", station.publisher.agent.country);
        tGlobalAttributes.set("publisher_sector", station.publisher.agent.sectorType);
        tGlobalAttributes.set("publisher_url", station.publisher.agent.url);
        tGlobalAttributes.set("publisher_type", "institution");

        List<String> contributorNames = new ArrayList<>();
        List<String> contributorRoles = new ArrayList<>();
        List<String> contributorUrls = new ArrayList<>();
        List<String> contributorEmails = new ArrayList<>();
        for (OikosStationAgentAffiliation contributor : station.contributors) {
            contributorNames.add(contributor.agent.label);
            contributorRoles.add(contributor.role);
            contributorUrls.add(contributor.agent.url);
            contributorEmails.add(contributor.agent.email);
        }
        contributorNames.add("Axiom Data Science");
        contributorRoles.add("processor");
        contributorUrls.add("https://www.axiomdatascience.com");
        contributorEmails.add("feedback@axiomdatascience.com");
        tGlobalAttributes.set("contributor_name", String.join(",", contributorNames));
        tGlobalAttributes.set("contributor_role", String.join(",", contributorRoles));
        tGlobalAttributes.set("contributor_url", String.join(",", contributorUrls));
        tGlobalAttributes.set("contributor_email", String.join(",", contributorEmails));
        tGlobalAttributes.set("contributor_role_vocabulary", "CI_RoleCode");
        tGlobalAttributes.set("contributor_role_vocabulary", "CI_RoleCode");

        List<String> references = new ArrayList<>();
        references.add(station.creator.foreignUrl);
        references.add(station.publisher.foreignUrl);
        references.addAll(station.contributors.stream().filter(c -> !c.foreignUrl.isEmpty()).map(c -> c.foreignUrl).collect(Collectors.toList()));
        if (station.qcInfoUrl != null) {
            references.add(station.qcInfoUrl);
        }
        tGlobalAttributes.set("references", String.join(",", references));
        String sourceUrl = station.publisher.foreignUrl;
        if (sourceUrl == null || sourceUrl.isEmpty()) {
            sourceUrl = station.creator.foreignUrl;
        }
        tGlobalAttributes.set("sourceUrl", sourceUrl);

        tGlobalAttributes.set("history", "Downloaded from " + station.publisher.agent.label + " at " + station.publisher.foreignUrl);

        if (station.wmoId != null) {
            tGlobalAttributes.set("wmo_platform_code", station.wmoId);
        }
        if (station.submitToNdbc) {
            tGlobalAttributes.set("gts_ingest", "true");
        }

        // TODO: keywords? license (other than the default ERDDAP one)? acknowledgement?

        // for timeseries display; see https://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#cdm_data_type
        tGlobalAttributes.set("cdm_data_type", EDD.CDM_TIMESERIES);
        tGlobalAttributes.set("cdm_timeseries_variables", String.join(",", cdm_timeseries_variables));

        // TODO: date_modified
        // set to the current time? in theory this dataset gets reloaded only if it's been modified?
        // maybe we can check the file time?
        // or should this just be metadata in station stats that's provided by oikos?

        return station;
    }

    static Table mapDataJsonToTable(JSONObject results, OikosStation station) throws Throwable {
        JSONArray feeds = results.getJSONObject("data").getJSONArray("groupedFeeds");

        Table table = new Table();

        // Fill the station_urn column
        StringArray station_array = new StringArray();
        table.addColumn("station", station_array);

        int maxRows = 0;

        for (int c = 0; c < feeds.length(); c++) {
            JSONObject feed = feeds.getJSONObject(c);
            JSONObject metadata = feed.getJSONObject("metadata");

            int timeIdx = metadata.getJSONObject("time").getInt("index");
            int lonIdx = metadata.get("lon") == JSONObject.NULL ? -1 : metadata.getJSONObject("lon").getInt("index");
            int latIdx = metadata.get("lat") == JSONObject.NULL ? -1 : metadata.getJSONObject("lat").getInt("index");
            int zIdx = metadata.get("z") == JSONObject.NULL ? -1 : metadata.getJSONObject("z").getInt("index");

            IntArray times_array = new IntArray();
            table.addColumn("time", times_array);

            DoubleArray lons_array = new DoubleArray();
            table.addColumn("longitude", lons_array);

            DoubleArray lats_array = new DoubleArray();
            table.addColumn("latitude", lats_array);

            DoubleArray z_array = new DoubleArray();
            table.addColumn("z", z_array);

            // multiple feeds can be grouped together
            JSONArray values = metadata.getJSONArray("values");
            for (int vi = 0; vi < values.length(); vi++) {
                JSONObject value = values.getJSONObject(vi);
                int deviceFeedId = value.getJSONArray("deviceFeedIds").getInt(0);

                OikosDeviceFeed df = station.getDeviceFeed(deviceFeedId);

                int valueIdx = value.getInt("index");
                DoubleArray values_array = new DoubleArray();
                String valueName = df.prettyString();
                table.addColumn(valueName, values_array);

                int qcAggIdx = metadata.getJSONArray("qartod").getJSONObject(vi).getInt("index");
                IntArray qc_agg_array = new IntArray();
                table.addColumn(valueName + "_qcagg", z_array);

                // add data for each feed
                JSONArray data = feed.getJSONArray("data");
                int nRows = data.length();
                for (int di = 0; di < nRows; di++) {
                    JSONArray d = data.getJSONArray(di);

                    String time = d.getString(timeIdx);
                    times_array.add(secondsSinceEpochFromUTCString(time));
                    values_array.add(d.getDouble(valueIdx));
                    qc_agg_array.add(d.getInt(qcAggIdx));

                    if (zIdx >= 0) {
                        z_array.add(d.getDouble(zIdx) * -1);
                    } else {
                        z_array.add(0.0);
                    }
                    if (lonIdx >= 0) {
                        lons_array.add(d.getDouble(lonIdx));
                        lats_array.add(d.getDouble(latIdx));
                    } else {
                        lons_array.add(station.longitude);
                        lats_array.add(station.latitude);
                    }
                }

                maxRows = (nRows > maxRows) ? nRows : maxRows;

            }
        }

        // Fill the station_urn column
        station_array.addN(maxRows, station.label);

        return table;
    }

}

public class EDDTableFromAxiomStationV2 extends EDDTableFromNcFiles {

    public static EDDTableFromAxiomStationV2 fromXml(Erddap erddap, SimpleXMLReader xmlReader) throws Throwable {
        String tDatasetID = xmlReader.attributeValue("datasetID");
        Attributes tGlobalAttributes = null;
        String tLocalSourceUrl = null;
        int tStationId = -1;
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        int tUpdateEveryNMillis = Integer.MAX_VALUE;
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
            if (xmlReader.stackSize() == startOfTagsN) {
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
            } else if (localTags.equals("<updateEveryNMillis>")) {
            } else if (localTags.equals("</updateEveryNMillis>")) {
                tUpdateEveryNMillis = String2.parseInt(content);
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
            throw new Exception("You must specify a <stationId> tag within a EDDTableFromAxiomStationV2 dataset");
        }

        OikosLookups oikosLookups = OikosLookups.getOikosLookups();

        // Query Sensor Service to get basic metadata
        String url = tLocalSourceUrl + "metadata/filter/custom?filter=" + new OikosSensorFilter(tStationId).toUrlEncodedString();
        String2.log("\n" + url);
        InputStream is = new URL(url).openStream();
        JSONObject json;
        try {

            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            json = new JSONObject(sb.toString());

        } finally {
            is.close();
        }

        OikosStation station = EDDTableFromAxiomStationV2Utils.mapMetadataForOikosStation(tGlobalAttributes, tStationId, tDataVariables, oikosLookups, json);

        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++) {
            ttDataVariables[i] = tDataVariables.get(i);
        }

        return new EDDTableFromAxiomStationV2(tDatasetID,
                null, null,
                null, null, null,
                tSosOfferingPrefix,
                tDefaultDataQuery, tDefaultGraphQuery,
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes,
                tUpdateEveryNMillis, station.archivePath, null, false,
                null, "last", null,
                0, 1, null,
                null, null, null, null, null, null, false,
                false, true, false);

    }

    public EDDTableFromAxiomStationV2(String tDatasetID,
                                      String tAccessibleTo, String tGraphsAccessibleTo,
                                      StringArray tOnChange, String tFgdcFile, String tIso19115File,
                                      String tSosOfferingPrefix,
                                      String tDefaultDataQuery, String tDefaultGraphQuery,
                                      Attributes tAddGlobalAttributes,
                                      Object[][] tDataVariables,
                                      int tReloadEveryNMinutes, int tUpdateEveryNMillis,
                                      String tFileDir, String tFileNameRegex, boolean tRecursive, String tPathRegex,
                                      String tMetadataFrom, String tCharset,
                                      int tColumnNamesRow, int tFirstDataRow,
                                      String tColumnSeparator, String tPreExtractRegex,
                                      String tPostExtractRegex, String tExtractRegex,
                                      String tColumnNameForExtract, String tSortedColumnSourceName,
                                      String tSortFilesBySourceNames, boolean tSourceNeedsExpandedFP_EQ,
                                      boolean tFileTableInMemory, boolean tAccessibleViaFiles,
                                      boolean tRemoveMVRows) throws Throwable {
        super("EDDTableFromAxiomStationV2", tDatasetID,
                tAccessibleTo, tGraphsAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File,
                tSosOfferingPrefix,
                tDefaultDataQuery, tDefaultGraphQuery,
                tAddGlobalAttributes,
                tDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis,
                tFileDir, tFileNameRegex, tRecursive, tPathRegex,
                tMetadataFrom, tCharset,
                tColumnNamesRow, tFirstDataRow,
                tColumnSeparator, tPreExtractRegex,
                tPostExtractRegex, tExtractRegex,
                tColumnNameForExtract, tSortedColumnSourceName,
                tSortFilesBySourceNames, tSourceNeedsExpandedFP_EQ,
                tFileTableInMemory, tAccessibleViaFiles,
                tRemoveMVRows);
    }

    @Override
    public Table lowGetSourceDataFromFile(String fileDir, String fileName, StringArray sourceDataNames, String[] sourceDataTypes, double sortedSpacing, double minSorted, double maxSorted, StringArray sourceConVars, StringArray sourceConOps, StringArray sourceConValues, boolean getMetadata, boolean mustGetData) throws Throwable {
        Table table = super.lowGetSourceDataFromFile(fileDir, fileName, sourceDataNames, sourceDataTypes, sortedSpacing, minSorted, maxSorted, sourceConVars, sourceConOps, sourceConValues, getMetadata, mustGetData);

        // Fill the station column so that we can have the station variable
        StringArray stationColumn = new StringArray();
        for (int i = 0; i < table.nRows(); i++) {
            stationColumn.add("");
        }
        table.addColumn("station", stationColumn);

        return table;
    }
}
