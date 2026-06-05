package com.kovoit.restapi.service;

import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.Company;
import com.kovoit.restapi.document.AddressDocument;
import com.kovoit.restapi.document.CompanyDocument;
import com.kovoit.restapi.repository.CompanyRepository;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final GeocodingService geocodingService;

    public CompanyService(CompanyRepository companyRepository, GeocodingService geocodingService) {
        this.companyRepository = companyRepository;
        this.geocodingService = geocodingService;
    }

    public List<Company> getCompanies() {
        return StreamSupport.stream(companyRepository.findAll().spliterator(), false)
                .map(this::fromDocument)
                .toList();
    }

    public Optional<Company> getCompanyById(String id) {
        return companyRepository.findById(id).map(this::fromDocument);
    }

    public Company saveCompany(Company company) {
        Address geocoded = geocodingService.geocode(company.address().fullAddress());
        AddressDocument addressDoc = new AddressDocument(
                geocoded.fullAddress(),
                new GeoPoint(geocoded.lat(), geocoded.lon())
        );
        CompanyDocument saved = companyRepository.save(new CompanyDocument(company.name(), addressDoc));
        return fromDocument(saved);
    }

    public void deleteCompany(String id) {
        companyRepository.deleteById(id);
    }

    private Company fromDocument(CompanyDocument doc) {
        Address address = null;
        if (doc.getAddress() != null) {
            address = new Address(
                    doc.getAddress().fullAddress(),
                    doc.getAddress().location().getLat(),
                    doc.getAddress().location().getLon()
            );
        }
        return new Company(doc.getId(), doc.getName(), address);
    }
}