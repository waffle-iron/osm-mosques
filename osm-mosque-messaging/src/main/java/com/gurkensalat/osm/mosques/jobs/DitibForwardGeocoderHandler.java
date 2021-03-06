package com.gurkensalat.osm.mosques.jobs;

import com.gurkensalat.osm.entity.DitibPlace;
import com.gurkensalat.osm.mosques.service.GeocodingService;
import com.gurkensalat.osm.repository.DitibPlaceRepository;
import com.tandogan.geostuff.opencagedata.entity.GeocodeResponse;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@Configuration
@Component
public class DitibForwardGeocoderHandler
{
    private final static Logger LOGGER = LoggerFactory.getLogger(DitibForwardGeocoderHandler.class);

    @Value("${mq.queue.forward-geocode-ditib.minsleep}")
    private Integer minsleep;

    @Value("${mq.queue.forward-geocode-ditib.randomsleep}")
    private Integer randomsleep;

    @Autowired
    private GeocodingService geocodingService;

    @Autowired
    private DitibPlaceRepository ditibPlaceRepository;

    @Autowired
    private SlackNotifier slackNotifier;

    @RabbitListener(queues = "${mq.queue.forward-geocode-ditib.name}")
    public void handleMessage(TaskMessage message)
    {
        LOGGER.info("  received <{}>", message);

        String ditibCode = message.getMessage();

        List<DitibPlace> places = ditibPlaceRepository.findByKey(ditibCode);

        if (places != null)
        {
            for (DitibPlace place : places)
            {
                LOGGER.info("  have to handle: {}", place);

                if (!(place.isGeocoded()))
                {
                    // Do some magic... Maybe the service should not write directly to the database...
                    GeocodeResponse response = geocodingService.ditibForward(ditibCode);

                    // Reload the place, it might have been changed by the geocodingService
                    place = ditibPlaceRepository.findOne(place.getId());

                    // did what we had to, now mark this place as "touched"
                    place.setLastGeocodeAttempt(DateTime.now());

                    place = ditibPlaceRepository.save(place);

                    long sleeptime = (long) (minsleep + (Math.random() * (double) randomsleep));

                    LOGGER.debug("Processing finished, sleeping a while ({} seconds)...", sleeptime);

                    try
                    {
                        Thread.sleep(sleeptime * 1000);
                    }
                    catch (InterruptedException ie)
                    {
                        // Not really important...
                    }

                    if (response.getRate() != null)
                    {
                        if (response.getRate().getRemaining() < 100)
                        {
                            slackNotifier.notify(SlackNotifier.CHANNEL_GEOCODING, "Too little attempts left (" + response.getRate().getRemaining() + " out of " + response.getRate().getLimit() + "), sleeping for half an hour");

                            try
                            {
                                Thread.sleep(30 * 60 * 1000);
                            }
                            catch (InterruptedException ie)
                            {
                                // Not really important...
                            }
                        }
                    }
                }
            }
        }

        LOGGER.info("  done with handleMessage <{}>", message);
    }
}
