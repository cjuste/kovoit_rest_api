package com.kovoit.restapi;

import com.kovoit.restapi.repository.TravelerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class RestApiApplicationTests {

    @MockitoBean
    TravelerRepository travelerRepository;

    @Test
    void contextLoads() {
    }
}
