package com.kovoit.restapi.service;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.kovoit.restapi.bean.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeocodingServiceTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock/nominatim"))
            .build();

    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        geocodingService = new GeocodingService(wm.getRuntimeInfo().getHttpBaseUrl(), 3000, 5000);
    }

    @Test
    void should_return_address_with_lat_lon_from_nominatim() {
        Address address = geocodingService.geocode("Paris, France");

        assertThat(address.fullAddress()).isEqualTo("Paris, France");
        assertThat(address.lat()).isEqualTo(48.8566);
        assertThat(address.lon()).isEqualTo(2.3522);
    }

    @Test
    void should_throw_when_address_not_found() {
        assertThatThrownBy(() -> geocodingService.geocode("adresse introuvable"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adresse introuvable");
    }

    @Test
    void reverseGeocode_returnsAddressWithDisplayNameAndOriginalCoordinates() {
        Address address = geocodingService.reverseGeocode(48.8566, 2.3522);

        assertThat(address.fullAddress()).isEqualTo("Paris, Île-de-France, France");
        assertThat(address.lat()).isEqualTo(48.8566);
        assertThat(address.lon()).isEqualTo(2.3522);
    }

    @Test
    void reverseGeocode_throwsWhenNoResult() {
        assertThatThrownBy(() -> geocodingService.reverseGeocode(0.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
