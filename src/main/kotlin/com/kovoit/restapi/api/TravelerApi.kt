package com.kovoit.restapi.api

import com.kovoit.restapi.bean.Traveler
import com.kovoit.restapi.service.TravelerService
import org.springframework.stereotype.Component
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/traveler")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Component
class TravelerApi(private val travelerService: TravelerService) {

    @GET
    @Path("/")
    fun getTravelers(): List<Traveler> = travelerService.getTravelers()
}