package com.ludoscity.findmybikes.citybik_es.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;


/**
 * Created by F8Full on 2015-10-10.
 * Data model class for citybik.es API
 */
@Entity
public class BikeNetworkDesc {

    public BikeNetworkDesc() {}

    @PrimaryKey
    @NonNull
    public String id;
    public String href;
    public String name;
    @Embedded(prefix = "network_location_")
    public BikeNetworkLocation location;
}
