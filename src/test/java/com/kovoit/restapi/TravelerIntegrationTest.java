package com.kovoit.restapi;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.PersonalInfo;
import com.kovoit.restapi.bean.Traveler;
import com.kovoit.restapi.repository.TravelerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TravelerIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock/nominatim"))
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
                () -> wm.getRuntimeInfo().getHttpBaseUrl());
    }

    private static final String SAMPLE_CSV = """
            Prénom;Nom;Email;Adresse;Conducteur;
            Clément;Juste;clement.juste@mycompany.org;5 rue du Patis Patelin, 35200 Rennes;Oui;
            Toto;Du canton;toto.ducanton@mycompany.org;2 rue de la Mabilais, 35000 Rennes;Non;
            """;

    // Adresse inconnue non couverte par les stubs → catch-all retourne [] → ligne ignorée
    private static final String CSV_WITH_UNKNOWN_ADDRESS = """
            Prénom;Nom;Email;Adresse;Conducteur;
            Clément;Juste;clement.juste@mycompany.org;5 rue du Patis Patelin, 35200 Rennes;Oui;
            Toto;Du canton;toto.ducanton@mycompany.org;adresse inconnue xyz;Non;
            """;

    @LocalServerPort
    int port;

    @Autowired
    TravelerRepository travelerRepository;

    RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        travelerRepository.deleteAll();
    }

    @Test
    void postTraveler_returns201WithBody() {
        Traveler body = new Traveler(
                new PersonalInfo("Alice", "Dupont", "alice@example.com",
                        new Address("Paris", 48.87, 2.33)),
                false
        );

        ResponseEntity<Traveler> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/traveler/", body, Traveler.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void postTraveler_documentIsIndexedInElasticsearch() {
        Traveler body = new Traveler(
                new PersonalInfo("Bob", "Martin", "bob@example.com",
                        new Address("Lyon", 45.75, 4.83)),
                true
        );

        restTemplate.postForEntity("http://localhost:" + port + "/api/traveler/", body, Traveler.class);

        assertThat(travelerRepository.count()).isEqualTo(1);
    }

    @Test
    void importCsv_returns200WithBothTravelers() {
        ResponseEntity<Traveler[]> response = postCsv(SAMPLE_CSV);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Traveler[] travelers = response.getBody();
        assertThat(travelers).hasSize(2);
        assertThat(travelers[0].personalInfo().firstName()).isEqualTo("Clément");
        assertThat(travelers[0].personalInfo().address().lat()).isEqualTo(48.1220);
        assertThat(travelers[0].isDriver()).isTrue();
        assertThat(travelers[1].personalInfo().firstName()).isEqualTo("Toto");
        assertThat(travelers[1].isDriver()).isFalse();
    }

    @Test
    void importCsv_indexesTravelersInElasticsearch() {
        postCsv(SAMPLE_CSV);

        assertThat(travelerRepository.count()).isEqualTo(2);
    }

    @Test
    void importCsv_skipsRowWhenGeocodingFails() {
        ResponseEntity<Traveler[]> response = postCsv(CSV_WITH_UNKNOWN_ADDRESS);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].personalInfo().firstName()).isEqualTo("Clément");
        assertThat(travelerRepository.count()).isEqualTo(1);
    }

    private ResponseEntity<Traveler[]> postCsv(String csvContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() { return "employees.csv"; }
        });
        return restTemplate.exchange(
                "http://localhost:" + port + "/api/traveler/import-csv",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Traveler[].class
        );
    }
}
