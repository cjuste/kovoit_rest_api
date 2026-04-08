package com.kovoit.restapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class TravelerServiceTest {

    @InjectMocks
    private lateinit var travelerService: TravelerService

    @Test
    fun testGetTravelers() {
        val travelers = travelerService.getTravelers()

        assertThat(travelers).hasSize(1)
    }
}
