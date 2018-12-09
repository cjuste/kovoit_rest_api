package com.kovoit.restapi

import com.kovoit.restapi.api.TravelerApi
import org.glassfish.jersey.server.ResourceConfig
import org.springframework.stereotype.Component

@Component
class JerseyConfig() : ResourceConfig() {

    init {
        register(TravelerApi())
    }
}