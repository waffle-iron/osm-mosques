package com.gurkensalat.osm.repository;

import com.gurkensalat.osm.SimpleConfiguration;
import com.gurkensalat.osm.mosques.entity.OsmMosquePlace;
import com.gurkensalat.osm.mosques.repository.OsmMosquePlaceRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.transaction.Transactional;

import java.util.Arrays;
import java.util.Iterator;

import static com.gurkensalat.osm.entity.PlaceType.OSM_PLACE_OF_WORSHIP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = SimpleConfiguration.class)
public class OsmMosquePlaceRepositoryTest
{
    @Autowired
    OsmMosquePlaceRepository testable;

    @Before
    public void setUp()
    {
        testable.deleteAll();
    }

    @Test
    public void emptyIfNullCountryCodeFromGeocoding()
    {
        OsmMosquePlace place1 = createTestPlace("foo-1");
        place1 = testable.save(place1);

        testable.emptyIfNullCountryCodeFromGeocoding();

        Iterable<OsmMosquePlace> places = testable.findAll();
        assertNotNull(places);
        OsmMosquePlace place2 = places.iterator().next();

        // TODO Actually, we should verify that place2 has no NULL entry in ADDR_CODE_FROM_GEOCODING
        // but transaction demarcation causes the update to not be written to database
    }

    @Test
    public void findOne()
    {
        OsmMosquePlace place1 = createTestPlace("foo-1");
        place1 = testable.save(place1);

        OsmMosquePlace place2 = createTestPlace("foo-2");
        place2 = testable.save(place2);

        Iterable<OsmMosquePlace> places = testable.findAll();
        assertNotNull(places);
        assertEquals(2, countFoundPlaces(places.iterator()));

        Page<OsmMosquePlace> justOnePlace = testable.findAll(new PageRequest(0, 1));
        assertNotNull(justOnePlace);
        assertEquals(1, countFoundPlaces(justOnePlace.iterator()));
    }

    private OsmMosquePlace createTestPlace(String name)
    {
        OsmMosquePlace place1 = new OsmMosquePlace(name, OSM_PLACE_OF_WORSHIP);
        place1.setCreationTime(DateTime.now());
        place1.setModificationTime(DateTime.now());
        return place1;
    }

    private int countFoundPlaces(Iterator<OsmMosquePlace> iterator)
    {
        int foundPlaces = 0;
        while (iterator.hasNext())
        {
            iterator.next();
            foundPlaces++;
        }

        return foundPlaces;
    }
}
