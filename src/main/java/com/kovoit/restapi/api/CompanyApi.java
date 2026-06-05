package com.kovoit.restapi.api;

import com.kovoit.restapi.bean.Company;
import com.kovoit.restapi.service.CompanyService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;

import java.util.List;

@Path("/company")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Component
public class CompanyApi {

    private final CompanyService companyService;

    public CompanyApi(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GET
    @Path("/")
    public List<Company> getCompanies() {
        return companyService.getCompanies();
    }

    @GET
    @Path("/{id}")
    public Response getCompany(@PathParam("id") String id) {
        return companyService.getCompanyById(id)
                .map(c -> Response.ok(c).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("/")
    public Response saveCompany(Company company) {
        Company saved = companyService.saveCompany(company);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.WILDCARD)
    public Response deleteCompany(@PathParam("id") String id) {
        companyService.deleteCompany(id);
        return Response.noContent().build();
    }
}