package com.kovoit.restapi.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kovoit.restapi.bean.Address;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
public class GeocodingService {

    private final RestClient restClient;

    public GeocodingService(
            @Value("${nominatim.base-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${nominatim.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${nominatim.read-timeout-ms:5000}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
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

    public Address reverseGeocode(double lat, double lon) {
        String latParam = String.format(Locale.ROOT, "%f", lat);
        String lonParam = String.format(Locale.ROOT, "%f", lon);
        NominatimReverseResult result = restClient.get()
                .uri("/reverse?lat={lat}&lon={lon}&format=json", latParam, lonParam)
                .retrieve()
                .body(NominatimReverseResult.class);

        if (result == null || result.displayName() == null) {
            throw new IllegalArgumentException("Adresse introuvable pour : " + lat + "," + lon);
        }

        return new Address(result.displayName(), lat, lon);
    }

    record NominatimResult(String lat, String lon) {}

    record NominatimReverseResult(@JsonProperty("display_name") String displayName) {}
}
