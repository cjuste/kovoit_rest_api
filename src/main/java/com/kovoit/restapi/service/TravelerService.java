package com.kovoit.restapi.service;

import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.PersonalInfo;
import com.kovoit.restapi.bean.Traveler;
import com.kovoit.restapi.document.AddressDocument;
import com.kovoit.restapi.document.TravelerDocument;
import com.kovoit.restapi.repository.TravelerRepository;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class TravelerService {

    private final TravelerRepository travelerRepository;

    public TravelerService(TravelerRepository travelerRepository) {
        this.travelerRepository = travelerRepository;
    }

    public List<Traveler> getTravelers() {
        return StreamSupport.stream(travelerRepository.findAll().spliterator(), false)
                .map(this::fromDocument)
                .toList();
    }

    public Traveler saveTraveler(Traveler traveler) {
        travelerRepository.save(toDocument(traveler));
        return traveler;
    }

    private Traveler fromDocument(TravelerDocument doc) {
        Address address = null;
        if (doc.getAddress() != null) {
            address = new Address(
                doc.getAddress().fullAddress(),
                doc.getAddress().location().getLat(),
                doc.getAddress().location().getLon()
            );
        }
        return new Traveler(new PersonalInfo(doc.getFirstName(), doc.getLastName(), doc.getEmail(), address), doc.isDriver());
    }

    private TravelerDocument toDocument(Traveler traveler) {
        PersonalInfo info = traveler.personalInfo();
        AddressDocument addressDoc = null;
        if (info.address() != null) {
            addressDoc = new AddressDocument(
                info.address().fullAddress(),
                new GeoPoint(info.address().lat(), info.address().lon())
            );
        }
        return new TravelerDocument(info.firstName(), info.lastName(), info.email(), addressDoc, traveler.isDriver());
    }
}
