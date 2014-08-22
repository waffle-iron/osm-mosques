package com.gurkensalat.osm.entity;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "LINKED_PLACES")
public class LinkedPlace extends AbstractPersistable<Long>
{
    @Column(name = "VERSION")
    private Integer version;

    @Column(name = "SCORE")
    private double score;

    private transient double lat;

    private transient double lon;

    @Column(name = "osm_id", length = 80)
    private String osmId;

    private transient OsmPlace osmPlace;

    @Column(name = "D_CODE", length = 80)
    private String ditibCode;

    private transient DitibPlace ditibPlace;

    @Column(name = "cagedata_key", length = 80)
    private String geocodedPlaceKey;

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public double getScore()
    {
        return score;
    }

    public void setScore(double score)
    {
        this.score = score;
    }

    public double getLat()
    {
        return lat;
    }

    public void setLat(double lat)
    {
        this.lat = lat;
    }

    public double getLon()
    {
        return lon;
    }

    public void setLon(double lon)
    {
        this.lon = lon;
    }

    public String getOsmId()
    {
        return osmId;
    }

    public void setOsmId(String osmId)
    {
        this.osmId = osmId;
    }

    public OsmPlace getOsmPlace()
    {
        return osmPlace;
    }

    public void setOsmPlace(OsmPlace osmPlace)
    {
        this.osmPlace = osmPlace;
    }

    public String getDitibCode()
    {
        return ditibCode;
    }

    public void setDitibCode(String ditibCode)
    {
        this.ditibCode = ditibCode;
    }

    public DitibPlace getDitibPlace()
    {
        return ditibPlace;
    }

    public void setDitibPlace(DitibPlace ditibPlace)
    {
        this.ditibPlace = ditibPlace;
    }

    public String getGeocodedPlaceKey()
    {
        return geocodedPlaceKey;
    }

    public void setGeocodedPlaceKey(String geocodedPlaceKey)
    {
        this.geocodedPlaceKey = geocodedPlaceKey;
    }
}
