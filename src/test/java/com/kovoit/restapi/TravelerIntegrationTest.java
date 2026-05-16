package com.kovoit.restapi;

import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.PersonalInfo;
import com.kovoit.restapi.bean.Traveler;
import com.kovoit.restapi.repository.TravelerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TravelerIntegrationTest {

    // Port hôte aléatoire attribué par Docker — aucun conflit avec un ES local
    static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.4.1"))
            .withEnv("xpack.security.enabled", "false");

    static {
        elasticsearch.start();
    }

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris",
                () -> "http://" + elasticsearch.getHttpHostAddress());
    }

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
                "http://localhost:" + port + "/traveler/", body, Traveler.class);

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

        restTemplate.postForEntity("http://localhost:" + port + "/traveler/", body, Traveler.class);

        assertThat(travelerRepository.count()).isEqualTo(1);
    }
}
