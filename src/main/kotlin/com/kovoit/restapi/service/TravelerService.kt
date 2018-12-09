package com.kovoit.restapi.service

import com.kovoit.restapi.bean.PersonalInfo
import com.kovoit.restapi.bean.Traveler
import org.springframework.stereotype.Service

@Service
class TravelerService {

    fun getTravelers(): List<Traveler> = listOf(Traveler(PersonalInfo("first", "traveler", "first@traveler.com")))
}