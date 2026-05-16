package com.kovoit.restapi.service;

import com.kovoit.restapi.bean.PersonalInfo;
import com.kovoit.restapi.bean.Traveler;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TravelerService {

    public List<Traveler> getTravelers() {
        return List.of(new Traveler(new PersonalInfo("first", "traveler", "first@traveler.com")));
    }
}
