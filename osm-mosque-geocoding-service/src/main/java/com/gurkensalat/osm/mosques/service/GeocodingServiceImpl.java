package com.gurkensalat.osm.mosques.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gurkensalat.osm.entity.Address;
import com.gurkensalat.osm.entity.DitibPlace;
import com.gurkensalat.osm.mosques.entity.OsmMosquePlace;
import com.gurkensalat.osm.mosques.repository.OsmMosquePlaceRepository;
import com.gurkensalat.osm.repository.DitibPlaceRepository;
import com.tandogan.geostuff.opencagedata.GeocodeRepository;
import com.tandogan.geostuff.opencagedata.entity.GeocodeResponse;
import com.tandogan.geostuff.opencagedata.entity.OpencageRate;
import com.tandogan.geostuff.opencagedata.entity.OpencageResult;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

@Component
public class GeocodingServiceImpl implements GeocodingService
{
    private final static Logger LOGGER = LoggerFactory.getLogger(GeocodingServiceImpl.class);

    private int minimumRequestsToLeave = 100;

    @Autowired
    private GeocodeRepository geocodeRepository;

    @Autowired
    private OsmMosquePlaceRepository osmMosquePlaceRepository;

    @Autowired
    private DitibPlaceRepository ditibPlaceRepository;

    @Value("${ditib.data.location}")
    private String dataLocation;

    @Override
    public GeocodeResponse reverse(String key)
    {
        GeocodeResponse response = null;

        if (belowMinimumNumberOfRequests())
        {
            return response;
        }

        List<OsmMosquePlace> places = osmMosquePlaceRepository.findByKey(key);
        if ((places != null) && (places.size() > 0))
        {
            OsmMosquePlace place = places.get(0);

            try
            {
                // Keep track whether we already tried this place...
                place.setLastGeocodeAttempt(DateTime.now());

                response = geocodeRepository.reverse(place.getLat(), place.getLon());
                int confidence = -1;
                String countryCode = "";
                String city = "";
                String town = "";
                String postcode = "";

                if (place.getAddress() == null)
                {
                    place.setAddress(new Address());
                }

                if ((response.getResults() == null) || (response.getResults().size() == 0))
                {
                    LOGGER.info("  NO RESULTS OBTAINED FOR PLACE {} / {} / {}", new Object[]{place.getKey(), place.getLat(), place.getLon()});
                }
                else
                {
                    for (OpencageResult result : response.getResults())
                    {
                        if (result.getConfidence() > confidence)
                        {
                            if (result.getComponents() != null)
                            {
                                countryCode = result.getComponents().getCountryCode();
                                city = trimToEmpty(result.getComponents().getCity());
                                town = trimToEmpty(result.getComponents().getTown());
                                postcode = trimToEmpty(result.getComponents().getPostcode());
                                confidence = result.getConfidence();
                            }
                        }
                    }

                    countryCode = trimToEmpty(countryCode).toUpperCase(Locale.ENGLISH);

                    place.setCountryFromGeocoding(countryCode);

                    if ("".equals(trimToEmpty(place.getCountryFromOSM())))
                    {
                        place.getAddress().setCountry(countryCode);
                        LOGGER.info("  OBTAINED COUNTRY CODE {}", place.getAddress().getCountry());
                    }

                    if (!("".equals(town)))
                    {
                        LOGGER.info("  MORE SPECIFIC TOWN INFO, USING THAT (C: '{}', T: '{}')", city, town);
                        city = town;
                    }

                    place.setCityFromGeocoding(city);

                    if ("".equals(trimToEmpty(place.getCityFromOSM())))
                    {
                        place.getAddress().setCity(city);
                        LOGGER.info("  OBTAINED CITY NAME {}", place.getAddress().getCity());
                    }

                    if ("".equals(trimToEmpty(place.getPostcodeFromOSM())))
                    {
                        place.getAddress().setPostcode(postcode);
                        LOGGER.info("  OBTAINED POSTCODE {}", place.getAddress().getPostcode());
                    }
                }

                LOGGER.info("  COUNTRY CODES OSM: '{}', OCD: '{}', place: '{}'", new Object[]{
                        place.getCountryFromOSM(),
                        place.getCountryFromGeocoding(),
                        place.getAddress().getCountry()
                });

                LOGGER.info("  CITY NAMES OSM: '{}', OCD: '{}', place: '{}'", new Object[]{
                        place.getCityFromOSM(),
                        place.getCityFromGeocoding(),
                        place.getAddress().getCity()
                });

                LOGGER.info("  POSTCODES OSM: '{}', OCD: '{}', place: '{}'", new Object[]{
                        place.getPostcodeFromOSM(),
                        place.getPostcodeFromGeocoding(),
                        place.getAddress().getPostcode()
                });

                LOGGER.info("https://www.osmmosques.org/unassigned-country/#zoom={}&lat={}&lon={}&layer={}", new Object[]{
                        12,
                        place.getLat(),
                        place.getLon(),
                        "Google%20Hybrid"});

                // Save place
                place = osmMosquePlaceRepository.save(place);
            }
            catch (Exception e)
            {
                LOGGER.error("While reverse geocoding", e);
            }
        }

        reportRemainingQuota(response);

        return response;
    }

    @Override
    public GeocodeResponse ditibForward(String ditibCode)
    {
        GeocodeResponse response = null;

        if (belowMinimumNumberOfRequests())
        {
            return response;
        }

        List<DitibPlace> places = ditibPlaceRepository.findByKey(ditibCode);
        if ((places != null) && (places.size() > 0))
        {
            DitibPlace place = places.get(0);
            if (place != null)
            {
                String query = "";
                query = query + (trimToEmpty(place.getAddress().getStreet()));
                query = query + " ";
                query = query + (trimToEmpty(place.getAddress().getHousenumber()));
                query = query + ", ";
                query = query + " ";
                query = query + (trimToEmpty(place.getAddress().getPostcode()));
                query = query + " ";
                query = query + (trimToEmpty(place.getAddress().getCity()));

                if ("DE".equals(trimToEmpty(place.getAddress().getCountry()).toUpperCase()))
                {
                    query = query + ",";
                    query = query + " ";
                    query = query + "Germany";
                }

                if ("NL".equals(trimToEmpty(place.getAddress().getCountry()).toUpperCase()))
                {
                    query = query + ",";
                    query = query + " ";
                    query = query + "Netherlands";
                }

                LOGGER.info("Query string is: '{}'", query);

                response = geocodeRepository.query(query);

                File workDir = new File(dataLocation, place.getKey());
                workDir.mkdirs();

                String when = DateTimeFormat.forPattern("YYYY-MM-dd-HH-mm-SS").print(DateTime.now());

                serializeToJSON(workDir, when + "-response.json", response);
                serializeToJSON(workDir, when + "-place-before.json", place);

                if (HttpStatus.OK.toString().equals(response.getStatus().getCode()))
                {
                    OpencageResult bestResult = getBestGeocodingResult(response);
                    if (bestResult != null)
                    {
                        place.setLat(bestResult.getGeometry().getLatitude());
                        place.setLon(bestResult.getGeometry().getLongitude());
                        place.setGeocoded(true);

                        place = ditibPlaceRepository.save(place);
                        serializeToJSON(workDir, when + "-place-after.json", place);
                    }
                }

                place.setLastGeocodeAttempt(DateTime.now());
                place.setModificationTime(DateTime.now());
                place = ditibPlaceRepository.save(place);
            }
        }

        reportRemainingQuota(response);

        return response;
    }

    private void reportRemainingQuota(GeocodeResponse response)
    {
        if (response != null)
        {
            if (response.getRate() == null)
            {
                response.setRate(new OpencageRate());
            }

            response.getRate().setRemaining(geocodeRepository.getRemaining());
            response.getRate().setLimit(geocodeRepository.getLimit());
        }
    }

    private boolean belowMinimumNumberOfRequests()
    {
        if (geocodeRepository.getRemaining() < minimumRequestsToLeave)
        {
            if (DateTime.now().isAfter(geocodeRepository.getReset()))
            {
                LOGGER.info("API rate reset timeout arrived, resetting limit and trying again...");
            }
            else
            {
                LOGGER.info("Too little requests allowed left (less than {}), trying again later...", minimumRequestsToLeave);
                LOGGER.debug("  limit will be reset at {}, now it is {}", geocodeRepository.getReset(), DateTime.now());
                LOGGER.debug("  {} of {} queries remaining", geocodeRepository.getRemaining(), geocodeRepository.getLimit());

                return true;
            }
        }

        return false;
    }

    private OpencageResult getBestGeocodingResult(GeocodeResponse response)
    {
        OpencageResult bestResult = null;
        for (OpencageResult result : response.getResults())
        {
            if (result.getGeometry() != null)
            {
                if (bestResult == null)
                {
                    bestResult = result;
                }

                if (bestResult.getConfidence() < result.getConfidence())
                {
                    bestResult = result;
                }
            }
        }

        // Avoid NullPointerException when no good result can be found
        if (bestResult == null)
        {
            return null;
        }

        // Cornercase to avoid
        if (bestResult.getConfidence() == 0)
        {
            return null;
        }

        // Clamp down to area of Germany
        if (bestResult.getGeometry().getLatitude() < 46)
        {
            return null;
        }

        if (bestResult.getGeometry().getLatitude() > 56)
        {
            return null;
        }

        if (bestResult.getGeometry().getLongitude() < 5)
        {
            return null;
        }

        if (bestResult.getGeometry().getLongitude() > 17)
        {
            return null;
        }

        return bestResult;
    }

    private void serializeToJSON(File workDir, String fileName, Object data)
    {
        try
        {
            ObjectMapper mapper = new ObjectMapper();

            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            String json = mapper.writeValueAsString(data);
            FileOutputStream fos = new FileOutputStream(new File(workDir, fileName));
            fos.write(json.getBytes());
            closeQuietly(fos);
        }
        catch (IOException ioe)
        {
            LOGGER.error("While serializing response", ioe);
        }
    }

}