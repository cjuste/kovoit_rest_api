package com.kovoit.restapi.api;

import com.kovoit.restapi.bean.Traveler;
import com.kovoit.restapi.service.TravelerService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.springframework.stereotype.Component;
import java.util.List;

@Path("/traveler")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Component
public class TravelerApi {

    private final TravelerService travelerService;

    public TravelerApi(TravelerService travelerService) {
        this.travelerService = travelerService;
    }

    @GET
    @Path("/")
    public List<Traveler> getTravelers() {
        return travelerService.getTravelers();
    }
}
