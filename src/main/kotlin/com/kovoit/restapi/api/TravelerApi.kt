package com.kovoit.restapi.api

import com.kovoit.restapi.bean.Traveler
import com.kovoit.restapi.service.TravelerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/traveler")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Component
class TravelerApi {

    @Autowired
    private lateinit var travelerService: TravelerService

    @GET
    @Path("/")
    fun getTravelers(): List<Traveler> = travelerService.getTravelers()
}