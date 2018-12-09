package com.kovoit.restapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class TravelerServiceTest {

    @InjectMocks
    private lateinit var travelerService: TravelerService

    @Test
    fun testGetTravelers() {
        var travelers = travelerService.getTravelers()

        assertThat(travelers).hasSize(1)
    }
}