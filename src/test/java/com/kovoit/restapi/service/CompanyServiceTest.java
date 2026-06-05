package com.kovoit.restapi.service;

import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.Company;
import com.kovoit.restapi.document.AddressDocument;
import com.kovoit.restapi.document.CompanyDocument;
import com.kovoit.restapi.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private CompanyService companyService;

    @Test
    void getCompanies_returnsAllFromRepository() {
        AddressDocument addrDoc = new AddressDocument("Paris, France", new GeoPoint(48.85, 2.35));
        CompanyDocument doc = new CompanyDocument("Acme", addrDoc);
        when(companyRepository.findAll()).thenReturn(List.of(doc));

        List<Company> companies = companyService.getCompanies();

        assertThat(companies).hasSize(1);
        Company c = companies.getFirst();
        assertThat(c.name()).isEqualTo("Acme");
        assertThat(c.address().fullAddress()).isEqualTo("Paris, France");
        assertThat(c.address().lat()).isEqualTo(48.85);
        assertThat(c.address().lon()).isEqualTo(2.35);
    }

    @Test
    void getCompanies_withNullAddress_returnsNullAddress() {
        CompanyDocument doc = new CompanyDocument("Acme", null);
        when(companyRepository.findAll()).thenReturn(List.of(doc));

        List<Company> companies = companyService.getCompanies();

        assertThat(companies.getFirst().address()).isNull();
    }

    @Test
    void getCompanyById_returnsCompany_whenExists() {
        AddressDocument addrDoc = new AddressDocument("Lyon, France", new GeoPoint(45.75, 4.83));
        CompanyDocument doc = new CompanyDocument("Beta Corp", addrDoc);
        when(companyRepository.findById("42")).thenReturn(Optional.of(doc));

        Optional<Company> result = companyService.getCompanyById("42");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Beta Corp");
    }

    @Test
    void getCompanyById_returnsEmpty_whenNotFound() {
        when(companyRepository.findById("99")).thenReturn(Optional.empty());

        Optional<Company> result = companyService.getCompanyById("99");

        assertThat(result).isEmpty();
    }

    @Test
    void saveCompany_geocodesAddressBeforeSaving() {
        when(geocodingService.geocode("Paris, France"))
                .thenReturn(new Address("Paris, France", 48.85, 2.35));
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        companyService.saveCompany(new Company(null, "Acme", new Address("Paris, France", 0, 0)));

        ArgumentCaptor<CompanyDocument> captor = ArgumentCaptor.forClass(CompanyDocument.class);
        verify(companyRepository).save(captor.capture());
        CompanyDocument doc = captor.getValue();
        assertThat(doc.getName()).isEqualTo("Acme");
        assertThat(doc.getAddress().location().getLat()).isEqualTo(48.85);
        assertThat(doc.getAddress().location().getLon()).isEqualTo(2.35);
    }

    @Test
    void saveCompany_returnsCompanyWithId() {
        when(geocodingService.geocode(any()))
                .thenReturn(new Address("Paris, France", 48.85, 2.35));
        CompanyDocument savedDoc = new CompanyDocument("Acme", new AddressDocument("Paris, France", new GeoPoint(48.85, 2.35)));
        when(companyRepository.save(any())).thenReturn(savedDoc);

        Company result = companyService.saveCompany(new Company(null, "Acme", new Address("Paris, France", 0, 0)));

        assertThat(result.name()).isEqualTo("Acme");
    }

    @Test
    void saveCompany_throwsWhenGeocodingFails() {
        when(geocodingService.geocode(any())).thenThrow(new IllegalArgumentException("not found"));

        assertThatThrownBy(() ->
                companyService.saveCompany(new Company(null, "Acme", new Address("adresse invalide", 0, 0)))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteCompany_delegatesToRepository() {
        companyService.deleteCompany("42");

        verify(companyRepository).deleteById("42");
    }
}