package com.kovoit.restapi.service;

import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.PersonalInfo;
import com.kovoit.restapi.bean.Traveler;
import com.kovoit.restapi.document.AddressDocument;
import com.kovoit.restapi.document.TravelerDocument;
import com.kovoit.restapi.repository.TravelerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class TravelerService {

    private static final Logger log = LoggerFactory.getLogger(TravelerService.class);

    private final TravelerRepository travelerRepository;
    private final GeocodingService geocodingService;

    public TravelerService(TravelerRepository travelerRepository, GeocodingService geocodingService) {
        this.travelerRepository = travelerRepository;
        this.geocodingService = geocodingService;
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

    public List<Traveler> importFromCsv(InputStream csv) {
        List<Traveler> imported = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(";", -1);
                if (cols.length < 5) continue;
                String firstName  = cols[0].trim();
                String lastName   = cols[1].trim();
                String email      = cols[2].trim();
                String rawAddress = cols[3].trim();
                boolean isDriver  = "oui".equalsIgnoreCase(cols[4].trim());
                try {
                    Address address = geocodingService.geocode(rawAddress);
                    Traveler traveler = new Traveler(new PersonalInfo(firstName, lastName, email, address), isDriver);
                    saveTraveler(traveler);
                    imported.add(traveler);
                } catch (IllegalArgumentException e) {
                    log.warn("Géocodage impossible pour '{}', ligne ignorée", rawAddress);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la lecture du CSV", e);
        }
        return imported;
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
