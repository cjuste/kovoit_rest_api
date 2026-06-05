package com.kovoit.restapi;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.Company;
import com.kovoit.restapi.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CompanyIntegrationTest {

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

    @LocalServerPort
    int port;

    @Autowired
    CompanyRepository companyRepository;

    RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        companyRepository.deleteAll();
    }

    @Test
    void postCompany_returns201WithGeocodedBody() {
        Company body = new Company(null, "Acme Corp", new Address("Paris, France", 0, 0));

        ResponseEntity<Company> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/company/", body, Company.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Company created = response.getBody();
        assertThat(created).isNotNull();
        assertThat(created.name()).isEqualTo("Acme Corp");
        assertThat(created.id()).isNotNull();
        assertThat(created.address().lat()).isEqualTo(48.8566);
        assertThat(created.address().lon()).isEqualTo(2.3522);
    }

    @Test
    void postCompany_documentIsIndexedInElasticsearch() {
        Company body = new Company(null, "Acme Corp", new Address("Paris, France", 0, 0));

        restTemplate.postForEntity("http://localhost:" + port + "/api/company/", body, Company.class);

        assertThat(companyRepository.count()).isEqualTo(1);
    }

    @Test
    void getCompanies_returnsAllCompanies() {
        Company body = new Company(null, "Acme Corp", new Address("Paris, France", 0, 0));
        restTemplate.postForEntity("http://localhost:" + port + "/api/company/", body, Company.class);

        ResponseEntity<Company[]> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/company/", Company[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].name()).isEqualTo("Acme Corp");
    }

    @Test
    void getCompany_returnsCompany_whenExists() {
        Company body = new Company(null, "Acme Corp", new Address("Paris, France", 0, 0));
        Company created = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/company/", body, Company.class).getBody();

        ResponseEntity<Company> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/company/" + created.id(), Company.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("Acme Corp");
    }

    @Test
    void getCompany_returns404_whenNotFound() {
        HttpClientErrorException exception = catchThrowableOfType(
                () -> restTemplate.getForEntity(
                        "http://localhost:" + port + "/api/company/unknown-id", Company.class),
                HttpClientErrorException.class);

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteCompany_returns204AndRemovesFromElasticsearch() {
        Company body = new Company(null, "Acme Corp", new Address("Paris, France", 0, 0));
        Company created = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/company/", body, Company.class).getBody();

        ResponseEntity<Void> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/company/" + created.id(),
                HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(companyRepository.count()).isEqualTo(0);
    }
}
