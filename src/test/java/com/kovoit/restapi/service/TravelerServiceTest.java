package com.kovoit.restapi.service;

import com.kovoit.restapi.bean.Traveler;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class TravelerServiceTest {

    @InjectMocks
    private TravelerService travelerService;

    @Test
    void testGetTravelers() {
        List<Traveler> travelers = travelerService.getTravelers();
        Assertions.assertThat(travelers).hasSize(1);
    }
}
