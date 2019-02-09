package gov.noaa.pfel.erddap.dataset;

import com.amazonaws.util.IOUtils;
import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static gov.noaa.pfel.erddap.dataset.EDDTableFromAxiomStationV2Utils.UTCStringFromSecondsSinceEpoch;
import static gov.noaa.pfel.erddap.dataset.EDDTableFromAxiomStationV2Utils.secondsSinceEpochFromUTCString;
import static org.junit.Assert.*;

public class EDDTableFromAxiomStationUnitTest {

    @Test
    public void testSecondsSinceEpochFromUTCString() throws ParseException {
        assertEquals(1513015500, secondsSinceEpochFromUTCString("2017-12-11T18:05:00Z"));
        assertEquals(1539898499, secondsSinceEpochFromUTCString("2018-10-18T21:34:59Z"));
    }

    @Test
    public void testUTCStringFromSecondsSinceEpoch() throws ParseException {
        assertEquals("2017-12-11T18:05:00Z", UTCStringFromSecondsSinceEpoch(1513015500));
        assertEquals("2018-10-18T21:34:59Z", UTCStringFromSecondsSinceEpoch(1539898499));
    }

    @Test
    public void oikosSensorFilterToString() throws UnsupportedEncodingException {
        OikosSensorFilter filterStationOnly = new OikosSensorFilter(60387);
        assertEquals("{\"stations\":[60387],\"parameterGroups\":[]}", filterStationOnly.toString());
        assertEquals("%7B%22stations%22%3A%5B60387%5D%2C%22parameterGroups%22%3A%5B%5D%7D", filterStationOnly.toUrlEncodedString());

        OikosSensorFilter filterWithParamGroups = new OikosSensorFilter(60387, newArrayList(5, 14, 6));
        assertEquals("{\"stations\":[60387],\"parameterGroups\":[5, 14, 6]}", filterWithParamGroups.toString());
        assertEquals("%7B%22stations%22%3A%5B60387%5D%2C%22parameterGroups%22%3A%5B5%2C14%2C6%5D%7D", filterWithParamGroups.toUrlEncodedString());
    }

    @Test
    public void getOikosLookups_unitTest() throws IOException, URISyntaxException {
        OikosLookups oikosLookups = mapOikosLookups();
        verifyOikosLookups(oikosLookups);
    }

    @Ignore("Integration test. Only run if needed")
    @Test
    public void getOikosLookups_integrationTest() throws IOException {
        OikosLookups oikosLookups = OikosLookups.getOikosLookups();
        verifyOikosLookups(oikosLookups);
    }

    private void verifyOikosLookups(OikosLookups oikosLookups) {
        assertEquals(271, oikosLookups.sensorParameterMap.size());
        assertEquals(216, oikosLookups.parameterMap.size());
        assertEquals(64, oikosLookups.unitMap.size());
        assertEquals(218, oikosLookups.agentMap.size());
        assertEquals(7, oikosLookups.agentAssociationTypeToRoleCodeMap.size());

        OikosSensorParameter cdomSensorParameter = oikosLookups.sensorParameterMap.get(346);

        assertEquals(346, cdomSensorParameter.id);
        assertEquals(153, cdomSensorParameter.parameter.id);
        assertEquals(63, cdomSensorParameter.unit.id);
        assertEquals("time: mean", cdomSensorParameter.cellMethods);
        assertEquals("PT6M", cdomSensorParameter.interval);
        assertEquals("", cdomSensorParameter.verticalDatum);

        OikosParameter relHumParam = oikosLookups.parameterMap.get(4);

        assertEquals(4, relHumParam.id);
        assertEquals(22, relHumParam.groupId);
        assertEquals("http://mmisw.org/ont/cf/parameter/relative_humidity", relHumParam.urn);
        assertEquals("Relative Humidity", relHumParam.label);
        assertEquals("relative_humidity", relHumParam.name);
        assertEquals(1, relHumParam.parameterTypeUnits.size());
        assertEquals(101, relHumParam.parameterTypeUnits.get(0).id);
        assertEquals(1, relHumParam.parameterTypeUnits.get(0).unit.id);
        assertEquals(true, relHumParam.parameterTypeUnits.get(0).unitSystemDefault);
        assertEquals(true, relHumParam.parameterTypeUnits.get(0).parameterTypeDefault);

        OikosUnit pptUnit = oikosLookups.unitMap.get(2);

        assertEquals(2, pptUnit.id);
        assertEquals(".001", pptUnit.unit);
        assertEquals("parts per thousand", pptUnit.label);
        assertEquals("NON_STANDARD", pptUnit.unitSystem);

        OikosAgent agent = oikosLookups.agentMap.get(2011);

        assertEquals(2011, agent.id);
        assertEquals("Scripps Institution of Oceanography, UC San Diego", agent.label);
        assertEquals("scripps-institution-of-oceano", agent.uuid);
        assertEquals("academic", agent.sectorType);
        assertEquals("https://scripps.ucsd.edu", agent.url);
        assertEquals("webmaster@scripps.ucsd.edu", agent.email);

        assertEquals("funder", oikosLookups.agentAssociationTypeToRoleCodeMap.get("supporter"));
        assertEquals("collaborator", oikosLookups.agentAssociationTypeToRoleCodeMap.get("operator"));

    }

    @Test
    public void oikosDeviceV1PrettyString() throws IOException, URISyntaxException {
        OikosLookups oikosLookups = mapOikosLookups();

        OikosEnhancedParameter waterLevel_noDatum = epFrom(oikosLookups.sensorParameterMap.get(46));
        OikosEnhancedParameter waterLevel_navd88 = epFrom(oikosLookups.sensorParameterMap.get(280));
        OikosEnhancedParameter waterLevel_localDatum = epFrom(oikosLookups.sensorParameterMap.get(352));
        OikosEnhancedParameter cdom_mean = epFrom(oikosLookups.sensorParameterMap.get(346));

        assertEquals("sea_surface_height_above_sea_level", devicePrettyString(waterLevel_noDatum, ""));
        assertEquals("sea_surface_height_above_sea_level_pier_1", devicePrettyString(waterLevel_noDatum, "pier 1"));
        assertEquals("sea_surface_height_above_sea_level_geoid_navd88", devicePrettyString(waterLevel_navd88, ""));
        assertEquals("sea_surface_height_above_sea_level_geoid_local_station_datum", devicePrettyString(waterLevel_localDatum, ""));
        assertEquals("colored_dissolved_organic_matter_cm_time_mean_over_pt6m", devicePrettyString(cdom_mean, ""));
    }

    private static OikosEnhancedParameter epFrom(OikosSensorParameter sp) {
        return new OikosEnhancedParameter(sp.id, sp.parameter, sp.cellMethods, sp.interval, sp.verticalDatum);
    }

    private static String devicePrettyString(OikosEnhancedParameter ep, String discriminant) {
        OikosDeviceV1 device = new OikosDeviceV1(123, ep, discriminant, 0.0, 0.0);
        return device.prettyString();
    }

    @Test
    public void oikosDeviceFeedPrettyString() throws IOException, URISyntaxException {
        OikosLookups oikosLookups = mapOikosLookups();

        OikosSensorParameter waterLevel_noDatum = oikosLookups.sensorParameterMap.get(46);
        OikosSensorParameter waterLevel_navd88 = oikosLookups.sensorParameterMap.get(280);
        OikosSensorParameter waterLevel_localDatum = oikosLookups.sensorParameterMap.get(352);
        OikosSensorParameter cdom_mean = oikosLookups.sensorParameterMap.get(346);

        assertEquals("sea_surface_height_above_sea_level", feedPrettyString(waterLevel_noDatum, ""));
        assertEquals("sea_surface_height_above_sea_level_pier_1", feedPrettyString(waterLevel_noDatum, "pier 1"));
        assertEquals("sea_surface_height_above_sea_level_geoid_navd88", feedPrettyString(waterLevel_navd88, ""));
        assertEquals("sea_surface_height_above_sea_level_geoid_local_station_datum", feedPrettyString(waterLevel_localDatum, ""));
        assertEquals("colored_dissolved_organic_matter_cm_time__mean_over_pt6m", feedPrettyString(cdom_mean, ""));
    }

    private static String feedPrettyString(OikosSensorParameter sp, String discriminant) {
        OikosDeviceFeed feed = new OikosDeviceFeed(123, sp, discriminant, 0.0, 0.0);
        return feed.prettyString();
    }

    @Test
    public void testV1StationMetadataMapping() throws Throwable {
        Attributes tGlobalAttributes = new Attributes();
        String tLocalSourceUrl = "https://sensors.axds.co/stationsensorservice/";

        int tStationId = 60387;

        ArrayList<Object[]> tDataVariables = new ArrayList<>();

        // http://oikos.axds.co/rest/context
        OikosLookups oikosLookups = mapOikosLookups();
        HashMap<Integer, OikosParameter> param_lookup = oikosLookups.parameterMap;

        // https://sensors.axds.co/stationsensorservice/getDataValues?method=GetStationsResultSetRowsJSON&version=3&stationids=60387&region=all&realtimeonly=false&verbose=true&jsoncallback=false
        JSONObject json = jsonObjectFromTestResource("/v1-sensor-service-GetStationsResultSetRowsJSON-60387.json");

        OikosStationV1 station = EDDTableFromAxiomStationUtils.mapMetadataForOikosStation(tGlobalAttributes, tLocalSourceUrl, tStationId, tDataVariables, param_lookup, json);

        // tGlobalAttributes, tDataVariables, and station should all have assertions

        // check tGlobalAttributes
        assertEquals("42013 - C10 - WFS Central Buoy, 25m Isobath", tGlobalAttributes.getString("title"));
        assertEquals("Timeseries data from '42013 - C10 - WFS Central Buoy, 25m Isobath' (urn:ioos:station:edu.usf.marine.comps:C10)", tGlobalAttributes.getString("summary"));
        assertEquals("Axiom Data Science", tGlobalAttributes.getString("institution"));
        assertEquals("http://axiomdatascience.com", tGlobalAttributes.getString("infoUrl"));
        assertEquals("https://sensors.axds.co/stationsensorservice/", tGlobalAttributes.getString("sourceUrl"));
        assertEquals("TimeSeries", tGlobalAttributes.getString("cdm_data_type"));
        assertDoubleEquals(-82.924, tGlobalAttributes.getDouble("geospatial_lon_min"));
        assertDoubleEquals(-82.924, tGlobalAttributes.getDouble("geospatial_lon_max"));
        assertDoubleEquals(27.173, tGlobalAttributes.getDouble("geospatial_lat_min"));
        assertDoubleEquals(27.173, tGlobalAttributes.getDouble("geospatial_lat_max"));
        assertEquals("wind_speed,air_pressure,wind_from_direction,sea_water_temperature,sea_surface_temperature,wind_speed_of_gust,air_temperature,sea_water_practical_salinity,sea_water_velocity_to_direction,short_wave_radiation,relative_humidity,sea_water_speed",
                tGlobalAttributes.getString("cdm_timeseries_variables"));

        // check station
        assertEquals(60387, station.id);
        assertEquals("42013 - C10 - WFS Central Buoy, 25m Isobath", station.label);
        assertEquals("urn:ioos:station:edu.usf.marine.comps:C10", station.urn);
        assertDoubleEquals(27.173, station.latitude);
        assertDoubleEquals(-82.924, station.longitude);
        assertEquals(1449788400, station.startDate);
        assertEquals(1539822900, station.endDate);
        assertEquals(12, station.devices.size());
        OikosDeviceV1 stationDevice = station.devices.get(0);
        assertEquals(441842, stationDevice.id);
        assertEquals("", stationDevice.discriminant);
        assertEquals(5, stationDevice.ep.id);
        assertDoubleEquals(0, stationDevice.depthMin);
        assertDoubleEquals(0, stationDevice.depthMax);

        // check tDataVariables
        assertEquals(17, tDataVariables.size());

        Object[] timeVar = findVariableWithName(tDataVariables, "time");
        Attributes timeVarAtts = (Attributes) timeVar[2];
        assertEquals("seconds since 1970-01-01T00:00:00", timeVarAtts.getString("units"));
        assertEquals("Time", timeVarAtts.getString("ioos_category"));
        assertEquals(1449788400, timeVarAtts.get("actual_range").getInt(0));
        assertEquals(1539822900, timeVarAtts.get("actual_range").getInt(1));

        Object[] latVar = findVariableWithName(tDataVariables, "latitude");
        Attributes latVarAtts = (Attributes) latVar[2];
        assertEquals("Location", latVarAtts.getString("ioos_category"));
        assertDoubleEquals(27.173, latVarAtts.get("actual_range").getDouble(0));
        assertDoubleEquals(27.173, latVarAtts.get("actual_range").getDouble(1));

        Object[] longVar = findVariableWithName(tDataVariables, "longitude");
        Attributes longVarAtts = (Attributes) longVar[2];
        assertEquals("Location", longVarAtts.getString("ioos_category"));
        assertDoubleEquals(-82.924, longVarAtts.get("actual_range").getDouble(0));
        assertDoubleEquals(-82.924, longVarAtts.get("actual_range").getDouble(1));

        Object[] depthVar = findVariableWithName(tDataVariables, "depth");
        Attributes depthVarAtts = (Attributes) depthVar[2];
        assertEquals("Location", depthVarAtts.getString("ioos_category"));
        assertDoubleEquals(0.0, depthVarAtts.get("actual_range").getDouble(0));
        assertDoubleEquals(22.38, depthVarAtts.get("actual_range").getDouble(1));

        Object[] airPressureVar = findVariableWithName(tDataVariables, "air_pressure");
        Attributes airPressureVarAtts = (Attributes) airPressureVar[2];
        assertEquals("air_pressure", airPressureVarAtts.getString("standard_name"));
        assertEquals("Barometric Pressure", airPressureVarAtts.getString("long_name"));
        assertEquals("millibars", airPressureVarAtts.getString("units"));
        assertEquals("Other", airPressureVarAtts.getString("ioos_category"));
        assertEquals("http://mmisw.org/ont/cf/parameter/air_pressure", airPressureVarAtts.getString("urn"));
        assertDoubleEquals(-9999.99, airPressureVarAtts.get("missing_value").getDouble(0));
        assertDoubleEquals(-9999.99, airPressureVarAtts.get("_FillValue").getDouble(0));

    }

    @Test
    public void testV2StationDatasetMapping() throws Throwable {
        Attributes tGlobalAttributes = new Attributes();

        int tStationId = 60387;

        ArrayList<Object[]> tDataVariables = new ArrayList<>();

        // http://oikos.axds.co/rest/context
        OikosLookups oikosLookups = mapOikosLookups();

        // https://sensors.axds.co/api/metadata/filter/custom?filter=%7B%22stations%22%3A%5B%2260387%22%5D%7D
        JSONObject json = jsonObjectFromTestResource("/v2-sensor-service-metadata-60387.json");

        OikosStation station = EDDTableFromAxiomStationV2Utils.mapMetadataForOikosStation(tGlobalAttributes, tStationId, tDataVariables, oikosLookups, json);

        // check station
        assertEquals(60387, station.id);
        assertEquals("42013 - C10 - WFS Central Buoy, 25m Isobath", station.label);
        assertEquals("42013-c10-wfs-central-buoy-25", station.urn);
        assertEquals("buoy", station.platformType);
        assertEquals("ftp://ocgweb.marine.usf.edu/pub/QC_Code/", station.qcInfoUrl);
        assertDoubleEquals(27.173, station.latitude);
        assertDoubleEquals(-82.924, station.longitude);
        assertEquals(1513015500, station.startDate);
        assertEquals(1539898499, station.endDate);
        assertDoubleEquals(-10.0, station.minZ);
        assertDoubleEquals(20.0, station.maxZ);
        assertEquals(9, station.deviceFeeds.size());
        OikosDeviceFeed stationDeviceFeed = station.deviceFeeds.get(0);
        assertEquals(10, stationDeviceFeed.id);
        assertEquals("", stationDeviceFeed.discriminant);
        assertEquals(5, stationDeviceFeed.sp.id);
        assertDoubleEquals(0, stationDeviceFeed.minZ);
        assertDoubleEquals(0, stationDeviceFeed.maxZ);

        // check station affiliations
        assertNotNull(station.creator);
        assertEquals(144, station.creator.agent.id);
        assertEquals("C10", station.creator.foreignName);
        assertEquals("http://comps.marine.usf.edu/index?view=station&id=C10", station.creator.foreignUrl);
        assertNotNull(station.publisher);
        assertEquals(451, station.publisher.agent.id);
        assertEquals("C10", station.publisher.foreignName);
        assertEquals("http://ocgtds.marine.usf.edu:8080/thredds/catalog/COMPS_C10_Month_MET/catalog.html", station.publisher.foreignUrl);
        assertNotNull(station.contributors);
        assertEquals(2, station.contributors.size());
        assertEquals(newHashSet(234, 178), station.contributors.stream().map(c -> c.agent.id).collect(Collectors.toSet()));
        assertEquals(newHashSet("sponsor", "operator"), station.contributors.stream().map(c -> c.type).collect(Collectors.toSet()));
        assertEquals(newHashSet("sponsor", "collaborator"), station.contributors.stream().map(c -> c.role).collect(Collectors.toSet()));

        // check GLOBAL ATTRIBUTES

        assertEquals("42013 - C10 - WFS Central Buoy, 25m Isobath", tGlobalAttributes.getString("title"));
        assertEquals("Timeseries data from '42013 - C10 - WFS Central Buoy, 25m Isobath' (42013-c10-wfs-central-buoy-25)", tGlobalAttributes.getString("summary"));

        assertEquals("com.axiomdatascience", tGlobalAttributes.getString("naming_authority"));
        assertEquals("60387", tGlobalAttributes.getString("id"));

        assertEquals("buoy", tGlobalAttributes.getString("platform"));
        assertEquals("http://mmisw.org/ont/ioos/platform", tGlobalAttributes.getString("platform_vocabulary"));

        assertEquals("timeSeries", tGlobalAttributes.getString("featureType"));

        assertEquals("IOOS-1.2, CF-1.6, ACDD-1.3", tGlobalAttributes.getString("Conventions"));
        assertEquals("NetCDF Climate and Forecast (CF) Metadata Convention Standard Name", tGlobalAttributes.getString("standard_name_vocabulary"));

        assertEquals("https://sensors.ioos.us/?sensor_version=v2#metadata/60387/station", tGlobalAttributes.getString("info_url"));
        assertEquals("https://sensors.ioos.us/?sensor_version=v2#metadata/60387/station", tGlobalAttributes.getString("infoUrl"));

        assertDoubleEquals(-82.924, tGlobalAttributes.getDouble("geospatial_lon_min"));
        assertDoubleEquals(-82.924, tGlobalAttributes.getDouble("geospatial_lon_max"));
        assertEquals("degrees_east", tGlobalAttributes.getString("geospatial_lon_units"));
        assertDoubleEquals(27.173, tGlobalAttributes.getDouble("geospatial_lat_min"));
        assertDoubleEquals(27.173, tGlobalAttributes.getDouble("geospatial_lat_max"));
        assertEquals("degrees_north", tGlobalAttributes.getString("geospatial_lat_units"));
        assertDoubleEquals(-10.0, tGlobalAttributes.getDouble("geospatial_vertical_min"));
        assertDoubleEquals(20.0, tGlobalAttributes.getDouble("geospatial_vertical_max"));
        assertEquals("m", tGlobalAttributes.getString("geospatial_vertical_units"));
        assertEquals("up", tGlobalAttributes.getString("geospatial_vertical_positive"));

        assertEquals("USF CMS - Coastal Ocean Monitoring and Prediction System", tGlobalAttributes.getString("institution"));
        assertEquals("USF CMS - Coastal Ocean Monitoring and Prediction System", tGlobalAttributes.getString("creator_name"));
        assertEquals("USF CMS - Coastal Ocean Monitoring and Prediction System", tGlobalAttributes.getString("creator_institution"));
        assertEquals("cmerz@usf.edu", tGlobalAttributes.getString("creator_email"));
        assertEquals("USA", tGlobalAttributes.getString("creator_country"));
        assertEquals("academic", tGlobalAttributes.getString("creator_sector"));
        assertEquals("http://comps.marine.usf.edu/", tGlobalAttributes.getString("creator_url"));

        assertEquals("University of Southern Florida (USF)", tGlobalAttributes.getString("publisher_name"));
        assertEquals("University of Southern Florida (USF)", tGlobalAttributes.getString("publisher_institution"));
        assertNull(tGlobalAttributes.getString("publisher_email"));
        assertNull(tGlobalAttributes.getString("publisher_country"));
        assertEquals("academic", tGlobalAttributes.getString("publisher_sector"));
        assertEquals("http://www.usf.edu/", tGlobalAttributes.getString("publisher_url"));

        assertEquals("Southeast Coastal Ocean Observing Regional Association (SECOORA),LimnoTech,Axiom Data Science", tGlobalAttributes.getString("contributor_name"));
        assertEquals("sponsor,collaborator,processor", tGlobalAttributes.getString("contributor_role"));
        assertEquals("CI_RoleCode", tGlobalAttributes.getString("contributor_role_vocabulary"));
        assertEquals("http://secoora.org/,http://www.limno.com/,https://www.axiomdatascience.com", tGlobalAttributes.getString("contributor_url"));
        assertEquals("None,webmaster@limno.com,feedback@axiomdatascience.com", tGlobalAttributes.getString("contributor_email"));

        assertEquals("http://comps.marine.usf.edu/index?view=station&id=C10," +
                "http://ocgtds.marine.usf.edu:8080/thredds/catalog/COMPS_C10_Month_MET/catalog.html," +
                "http://secoora.org/C10," +
                "ftp://ocgweb.marine.usf.edu/pub/QC_Code/", tGlobalAttributes.getString("references"));

        assertEquals("http://ocgtds.marine.usf.edu:8080/thredds/catalog/COMPS_C10_Month_MET/catalog.html", tGlobalAttributes.getString("sourceUrl"));
        assertEquals("Downloaded from University of Southern Florida (USF) " +
                "at http://ocgtds.marine.usf.edu:8080/thredds/catalog/COMPS_C10_Month_MET/catalog.html", tGlobalAttributes.getString("history"));

        assertEquals("TimeSeries", tGlobalAttributes.getString("cdm_data_type"));
        assertEquals("wind_speed,wind_from_direction,longwave_radiation,short_wave_radiation,air_pressure,sea_water_temperature,wind_speed_of_gust,relative_humidity,air_temperature",
                tGlobalAttributes.getString("cdm_timeseries_variables"));

        // check STATION platform data variable

        Object[] stationVar = findVariableWithName(tDataVariables, "station");
        Attributes stationVarAtts = (Attributes) stationVar[2];
        assertEquals("urn:ioos:station:com.axiomdatascience:60387", stationVarAtts.getString("ioos_code"));
        assertEquals("Identifier", stationVarAtts.getString("ioos_category"));
        assertEquals("timeseries_id", stationVarAtts.getString("cf_role"));
        assertEquals("42013 - C10 - WFS Central Buoy, 25m Isobath", stationVarAtts.getString("long_name"));
        assertEquals("42013-c10-wfs-central-buoy-25", stationVarAtts.getString("short_name"));
        assertEquals("buoy", stationVarAtts.getString("type"));

        // check DATA VARIABLES

        // time + lat + lon + z + station + 3*values
        assertEquals(32, tDataVariables.size());

        Object[] timeVar = findVariableWithName(tDataVariables, "time");
        Attributes timeVarAtts = (Attributes) timeVar[2];
        assertEquals("seconds since 1970-01-01T00:00:00", timeVarAtts.getString("units"));
        assertEquals("Time", timeVarAtts.getString("ioos_category"));
        assertEquals(1513015500, timeVarAtts.get("actual_range").getInt(0));
        assertEquals(1539898499, timeVarAtts.get("actual_range").getInt(1));

        Object[] latVar = findVariableWithName(tDataVariables, "lat");
        Attributes latVarAtts = (Attributes) latVar[2];
        assertEquals("Location", latVarAtts.getString("ioos_category"));
        assertDoubleEquals(27.173, latVarAtts.get("actual_range").getDouble(0));
        assertDoubleEquals(27.173, latVarAtts.get("actual_range").getDouble(1));

        Object[] longVar = findVariableWithName(tDataVariables, "lon");
        Attributes longVarAtts = (Attributes) longVar[2];
        assertEquals("Location", longVarAtts.getString("ioos_category"));
        assertDoubleEquals(-82.924, longVarAtts.get("actual_range").getDouble(0));
        assertDoubleEquals(-82.924, longVarAtts.get("actual_range").getDouble(1));

        Object[] zVar = findVariableWithName(tDataVariables, "z");
        Attributes zVarAtts = (Attributes) zVar[2];
        assertEquals("Location", zVarAtts.getString("ioos_category"));
        assertEquals("up", zVarAtts.getString("positive"));
        assertDoubleEquals(-10.0, zVarAtts.get("actual_range").getDouble(0));
        assertDoubleEquals(20.0, zVarAtts.get("actual_range").getDouble(1));

        Object[] airPressureVar = findVariableWithName(tDataVariables, "value_11");
        Attributes airPressureVarAtts = (Attributes) airPressureVar[2];
        assertEquals("air_pressure", airPressureVarAtts.getString("standard_name"));
        assertEquals("Barometric Pressure", airPressureVarAtts.getString("long_name"));
        assertEquals("11", airPressureVarAtts.getString("id"));
        assertEquals("millibars", airPressureVarAtts.getString("units"));
        assertEquals("Other", airPressureVarAtts.getString("ioos_category"));
        assertEquals("station", airPressureVarAtts.getString("platform"));
        assertEquals("air_pressure_qc_agg air_pressure_qc_tests", airPressureVarAtts.getString("ancillary_variables"));
        assertEquals("http://mmisw.org/ont/cf/parameter/air_pressure", airPressureVarAtts.getString("urn"));
        assertEquals(-9999, airPressureVarAtts.get("missing_value").getDouble(0), 0);
        assertEquals(-9999, airPressureVarAtts.get("_FillValue").getDouble(0), 0);

        Object[] airPressureQcAggVar = findVariableWithName(tDataVariables, "qc_agg_11");
        Attributes airPressureQcAggAtts = (Attributes) airPressureQcAggVar[2];
        assertEquals("air_pressure status_flag", airPressureQcAggAtts.getString("standard_name"));
        assertEquals("Barometric Pressure QARTOD Aggregate Flag", airPressureQcAggAtts.getString("long_name"));
        assertEquals("Other", airPressureQcAggAtts.getString("ioos_category"));

        Object[] airPressureQcTestsVar = findVariableWithName(tDataVariables, "qc_tests_11");
        Attributes airPressureQcTestsAtts = (Attributes) airPressureQcTestsVar[2];
        assertEquals("air_pressure status_flag", airPressureQcTestsAtts.getString("standard_name"));
        assertEquals("Barometric Pressure QARTOD Individual Tests", airPressureQcTestsAtts.getString("long_name"));
        assertEquals("Other", airPressureQcTestsAtts.getString("ioos_category"));

    }

    @Test
    public void testV1StationDataMapping() throws Throwable {

        // Build station metadata
        JSONObject json = jsonObjectFromTestResource("/v1-sensor-service-GetStationsResultSetRowsJSON-60387.json");
        OikosStationV1 station = EDDTableFromAxiomStationUtils.mapMetadataForOikosStation(new Attributes(), "", 60387, new ArrayList<>(), mapOikosLookups().parameterMap, json);

        // Build station data

        // http://sensors.axds.co/stationsensorservice/getDataValues?stationid=60387&parameterids=5%2C14%2C6%2C41%2C191%2C7%2C3%2C50%2C67%2C158%2C4%2C68&units=5%3Bknot%2C14%3Bmillibars%2C6%3Bdegrees%2C41%3Bdegree_Fahrenheit%2C191%3Bdegree_Fahrenheit%2C7%3Bknot%2C3%3Bdegree_Fahrenheit%2C50%3B1e-3%2C67%3Bdegrees%2C158%3Bunknown%2C4%3B%25%2C68%3Bknot&start_time=1526256000&end_time=1526428800&jsoncallback=false&version=3&force_binned_data=false&method=GetSensorObservationsJSON
        JSONArray dataJson = jsonArrayFromTestResource("/v1-sensor-service-GetSensorObservationsJSON-60387.json");

        TableWriter tableWriter = new MockTableWriter(null, null, null);
        Table table = EDDTableFromAxiomStationUtils.mapDataJsonToTable(tableWriter, dataJson, station);

        int expectedNumObservations = 94;

        // station, lat, lon, depth -- constant across time

        PrimitiveArray stationColumn = getColumn(table, "station", expectedNumObservations);
        PrimitiveArray longitudeColumn = getColumn(table, "longitude", expectedNumObservations);
        PrimitiveArray latitudeColumn = getColumn(table, "latitude", expectedNumObservations);
        PrimitiveArray depthColumn = getColumn(table, "depth", expectedNumObservations);

        for (int i = 0; i < expectedNumObservations; i++) {
            assertEquals("42013 - C10 - WFS Central Buoy, 25m Isobath", stationColumn.getString(0));
            assertDoubleEquals(-82.924, longitudeColumn.getDouble(i));
            assertDoubleEquals(27.173, latitudeColumn.getDouble(i));
            assertDoubleEquals(0.0, depthColumn.getDouble(i));
        }

        // variables

        PrimitiveArray timeColumn = getColumn(table, "time", expectedNumObservations);
        PrimitiveArray windSpeedColumn = getColumn(table, "wind_speed", expectedNumObservations);
        PrimitiveArray windDirColumn = getColumn(table, "wind_from_direction", expectedNumObservations);

        assertEquals(1526427300, timeColumn.getInt(0));
        assertDoubleEquals(15.57, windSpeedColumn.getDouble(0));
        assertDoubleEquals(159.48, windDirColumn.getDouble(0));

        assertEquals(1526425500, timeColumn.getInt(1));
        assertDoubleEquals(16.484, windSpeedColumn.getDouble(1));
        assertDoubleEquals(161.27, windDirColumn.getDouble(1));

        getColumn(table, "wind_speed_of_gust", expectedNumObservations);
        getColumn(table, "relative_humidity", expectedNumObservations);
        getColumn(table, "sea_water_temperature", expectedNumObservations);
        getColumn(table, "air_pressure", expectedNumObservations);

    }

    private static PrimitiveArray getColumn(Table table, String variableName, int expectedNumObservations) {
        PrimitiveArray col = table.getColumn(variableName);
        assertEquals(expectedNumObservations, col.size());
        return col;
    }

    static OikosLookups mapOikosLookups() throws URISyntaxException, IOException {
        // http://oikos.axds.co/rest/context
        JSONObject jsonObject = jsonObjectFromTestResource("/oikos-context.json");
        return OikosLookups.mapOikosLookupsFromJson(jsonObject);
    }

    static void assertDoubleEquals(double d1, double d2) {
        assertEquals(d1, d2, 1E-6);
    }

    private Object[] findVariableWithName(ArrayList<Object[]> tDataVariables, String name) {
        return tDataVariables.stream().filter(objects -> objects[0].equals(name)).findFirst().get();
    }

    static JSONObject jsonObjectFromTestResource(String path) throws URISyntaxException, IOException {
        String json = testResourceAsString(path);
        return new JSONObject(json);
    }

    static JSONArray jsonArrayFromTestResource(String path) throws URISyntaxException, IOException {
        String json = testResourceAsString(path);
        return new JSONArray(json);
    }

    static String testResourceAsString(String path) throws URISyntaxException, IOException {
        URL resource = EDDTableFromAxiomStationUnitTest.class.getResource(path);
        File file = new File(resource.toURI());
        FileInputStream fileInputStream = new FileInputStream(file);
        return IOUtils.toString(fileInputStream);
    }

    static String testResourcePath(String path) throws URISyntaxException {
        URL resource = EDDTableFromAxiomStationUnitTest.class.getResource(path);
        File file = new File(resource.toURI());
        return file.getPath();
    }
}
