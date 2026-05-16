package com.kovoit.restapi.document;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

public record AddressDocument(@Field(type = FieldType.Text) String fullAddress, @GeoPointField GeoPoint location) {

    @Override
    public String fullAddress() {
        return fullAddress;
    }
}
