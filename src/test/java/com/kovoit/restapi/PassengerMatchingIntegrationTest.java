package com.kovoit.restapi;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.Company;
import com.kovoit.restapi.bean.PassengerMatch;
import com.kovoit.restapi.bean.PersonalInfo;
import com.kovoit.restapi.bean.Traveler;
import com.kovoit.restapi.document.AddressDocument;
import com.kovoit.restapi.document.CompanyDocument;
import com.kovoit.restapi.document.TravelerDocument;
import com.kovoit.restapi.repository.CompanyRepository;
import com.kovoit.restapi.repository.TravelerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PassengerMatchingIntegrationTest {

    @RegisterExtension
    static WireMockExtension nominatimWm = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock/nominatim"))
            .build();

    @RegisterExtension
    static WireMockExtension osrmWm = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock/osrm"))
            .build();

    static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.4.1"))
            .withEnv("xpack.security.enabled", "false");

    static {
        elasticsearch.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris",
                () -> "http://" + elasticsearch.getHttpHostAddress());
        registry.add("nominatim.base-url",
                () -> nominatimWm.getRuntimeInfo().getHttpBaseUrl());
        registry.add("osrm.base-url",
                () -> osrmWm.getRuntimeInfo().getHttpBaseUrl());
    }

    @LocalServerPort
    int port;

    @Autowired
    TravelerRepository travelerRepository;

    @Autowired
    CompanyRepository companyRepository;

    RestTemplate restTemplate;
    Company company;

    private static final Address DRIVER_ADDRESS = new Address("Paris", 48.8566, 2.3522);
    private static final Address COMPANY_ADDRESS = new Address("Nantes", 47.2184, -1.5536);
    private static final Address PASSENGER_ADDRESS = new Address("Proche de Paris", 48.8570, 2.3520);

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        travelerRepository.deleteAll();
        companyRepository.deleteAll();

        CompanyDocument saved = companyRepository.save(new CompanyDocument("Acme",
                new AddressDocument(COMPANY_ADDRESS.fullAddress(),
                        new GeoPoint(COMPANY_ADDRESS.lat(), COMPANY_ADDRESS.lon()))));
        company = new Company(saved.getId(), saved.getName(), COMPANY_ADDRESS);

        travelerRepository.save(new TravelerDocument("Bob", "Martin", "bob@example.com",
                new AddressDocument(PASSENGER_ADDRESS.fullAddress(),
                        new GeoPoint(PASSENGER_ADDRESS.lat(), PASSENGER_ADDRESS.lon())),
                false, company.id()));
    }

    @Test
    void findPassengers_returnsMatchingPassengerWithRendezVousPoint() {
        Traveler conducteur = new Traveler(
                new PersonalInfo("Alice", "Dupont", "alice@example.com", DRIVER_ADDRESS), true, company);

        ResponseEntity<PassengerMatch[]> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/traveler/passagers", conducteur, PassengerMatch[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PassengerMatch[] matches = response.getBody();
        assertThat(matches).hasSize(1);
        assertThat(matches[0].passenger().personalInfo().email()).isEqualTo("bob@example.com");
        assertThat(matches[0].rendezVous().fullAddress()).isEqualTo("Paris, Île-de-France, France");
    }
}
