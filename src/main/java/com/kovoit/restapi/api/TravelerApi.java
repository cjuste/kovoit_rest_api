package com.kovoit.restapi.api;

import com.kovoit.restapi.bean.PassengerMatch;
import com.kovoit.restapi.bean.Traveler;
import com.kovoit.restapi.service.PassengerMatchingService;
import com.kovoit.restapi.service.TravelerService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Path("/traveler")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Component
public class TravelerApi {

    private final TravelerService travelerService;
    private final PassengerMatchingService passengerMatchingService;

    public TravelerApi(TravelerService travelerService, PassengerMatchingService passengerMatchingService) {
        this.travelerService = travelerService;
        this.passengerMatchingService = passengerMatchingService;
    }

    @GET
    @Path("/")
    public List<Traveler> getTravelers() {
        return travelerService.getTravelers();
    }

    @POST
    @Path("/")
    public Response saveTraveler(Traveler traveler) {
        Traveler saved = travelerService.saveTraveler(traveler);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @POST
    @Path("/import-csv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importCsv(@FormDataParam("file") InputStream fileInputStream,
                               @FormDataParam("companyId") String companyId) {
        List<Traveler> imported = travelerService.importFromCsv(fileInputStream, companyId);
        return Response.ok(imported).build();
    }

    @POST
    @Path("/passagers")
    public Response findPassengers(Traveler conducteur) {
        List<PassengerMatch> matches = passengerMatchingService.findPassengers(conducteur);
        return Response.ok(matches).build();
    }
}
