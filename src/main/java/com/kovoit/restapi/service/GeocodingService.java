package com.kovoit.restapi.service;

import com.kovoit.restapi.bean.Address;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class GeocodingService {

    private final RestClient restClient;

    public GeocodingService(
            @Value("${nominatim.base-url:https://nominatim.openstreetmap.org}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "kovoit-rest-api")
                .build();
    }

    public Address geocode(String fullAddress) {
        List<NominatimResult> results = restClient.get()
                .uri("/search?q={q}&format=json&limit=1", fullAddress)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("Adresse introuvable : " + fullAddress);
        }

        NominatimResult first = results.getFirst();
        return new Address(fullAddress, Double.parseDouble(first.lat()), Double.parseDouble(first.lon()));
    }

    record NominatimResult(String lat, String lon) {}
}
