package com.kovoit.restapi.service;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.RouteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutingServiceTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock/osrm"))
            .build();

    private RoutingService routingService;

    @BeforeEach
    void setUp() {
        routingService = new RoutingService(wm.getRuntimeInfo().getHttpBaseUrl(), 3000, 5000);
    }

    @Test
    void route_returnsDurationDistanceAndGeometry() {
        Address from = new Address("Paris", 48.8566, 2.3522);
        Address to = new Address("Nantes", 47.2184, -1.5536);

        RouteResult result = routingService.route(from, to);

        assertThat(result.durationSeconds()).isEqualTo(300L);
        assertThat(result.distanceMeters()).isEqualTo(5000.0);
        assertThat(result.geometry()).hasSize(2);
        assertThat(result.geometry().get(0).lat()).isEqualTo(48.8566);
        assertThat(result.geometry().get(0).lon()).isEqualTo(2.3522);
        assertThat(result.geometry().get(1).lat()).isEqualTo(47.2184);
        assertThat(result.geometry().get(1).lon()).isEqualTo(-1.5536);
    }

    @Test
    void routeMulti_sendsAllWaypointsInOrder() {
        Address a = new Address("A", 48.8566, 2.3522);
        Address b = new Address("B", 48.86, 2.34);
        Address c = new Address("C", 47.2184, -1.5536);

        RouteResult result = routingService.routeMulti(List.of(a, b, c));

        assertThat(result.durationSeconds()).isEqualTo(600L);
        assertThat(result.geometry()).hasSize(3);
    }

    @Test
    void routeMulti_throwsWhenLessThanTwoWaypoints() {
        assertThatThrownBy(() -> routingService.routeMulti(List.of(new Address("A", 1, 1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void route_throwsWhenNoRouteFound() {
        Address from = new Address("Nowhere", 0, 0);
        Address to = new Address("Nowhere2", 0.001, 0.001);

        assertThatThrownBy(() -> routingService.route(from, to))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
