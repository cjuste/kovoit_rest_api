package com.kovoit.restapi;

import com.kovoit.restapi.api.CompanyApi;
import com.kovoit.restapi.api.TravelerApi;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(TravelerApi.class);
        register(CompanyApi.class);
        register(MultiPartFeature.class);
    }
}
