package com.kovoit.restapi.service;

import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.RoutePoint;
import com.kovoit.restapi.bean.RouteResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class RoutingService {

    private final RestClient restClient;

    public RoutingService(
            @Value("${osrm.base-url:http://localhost:5000}") String baseUrl,
            @Value("${osrm.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${osrm.read-timeout-ms:5000}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "kovoit-rest-api")
                .build();
    }

    public RouteResult route(Address from, Address to) {
        return routeMulti(List.of(from, to));
    }

    public RouteResult routeMulti(List<Address> waypointsInOrder) {
        if (waypointsInOrder.size() < 2) {
            throw new IllegalArgumentException("Il faut au moins 2 points pour calculer un trajet");
        }

        String coordinates = waypointsInOrder.stream()
                .map(a -> String.format(Locale.ROOT, "%f,%f", a.lon(), a.lat()))
                .collect(Collectors.joining(";"));
        String path = "/route/v1/driving/" + coordinates + "?overview=full&geometries=geojson";

        OsrmResponse response = restClient.get()
                .uri(path)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            throw new IllegalArgumentException("Aucun trajet trouvé pour : " + coordinates);
        }

        OsrmRoute osrmRoute = response.routes().getFirst();
        List<RoutePoint> geometry = osrmRoute.geometry().coordinates().stream()
                .map(coord -> new RoutePoint(coord.get(1), coord.getFirst()))
                .toList();

        return new RouteResult(Math.round(osrmRoute.duration()), osrmRoute.distance(), geometry);
    }

    record OsrmResponse(List<OsrmRoute> routes) {}

    record OsrmRoute(double duration, double distance, OsrmGeometry geometry) {}

    record OsrmGeometry(List<List<Double>> coordinates) {}
}
