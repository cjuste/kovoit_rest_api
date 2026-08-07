package com.kovoit.restapi.service;

import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.Company;
import com.kovoit.restapi.bean.PersonalInfo;
import com.kovoit.restapi.bean.Traveler;
import com.kovoit.restapi.document.AddressDocument;
import com.kovoit.restapi.document.TravelerDocument;
import com.kovoit.restapi.repository.TravelerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelerServiceTest {

    @Mock
    private TravelerRepository travelerRepository;

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private TravelerService travelerService;

    @Test
    void getTravelers_returnsAllFromRepository() {
        AddressDocument addrDoc = new AddressDocument("Paris", new GeoPoint(48.87, 2.33));
        TravelerDocument doc = new TravelerDocument("Alice", "Dupont", "alice@example.com", addrDoc, true, null);
        when(travelerRepository.findAll()).thenReturn(List.of(doc));

        List<Traveler> travelers = travelerService.getTravelers();

        assertThat(travelers).hasSize(1);
        Traveler t = travelers.getFirst();
        assertThat(t.personalInfo().firstName()).isEqualTo("Alice");
        assertThat(t.personalInfo().lastName()).isEqualTo("Dupont");
        assertThat(t.personalInfo().email()).isEqualTo("alice@example.com");
        assertThat(t.personalInfo().address().fullAddress()).isEqualTo("Paris");
        assertThat(t.personalInfo().address().lat()).isEqualTo(48.87);
        assertThat(t.personalInfo().address().lon()).isEqualTo(2.33);
        assertThat(t.isDriver()).isTrue();
    }

    @Test
    void getTravelers_withNullAddress_returnsNullAddress() {
        TravelerDocument doc = new TravelerDocument("Bob", "Martin", "bob@example.com", null, false, null);
        when(travelerRepository.findAll()).thenReturn(List.of(doc));

        List<Traveler> travelers = travelerService.getTravelers();

        assertThat(travelers.getFirst().personalInfo().address()).isNull();
    }

    @Test
    void getTravelers_resolvesCompanyFromCompanyId() {
        Company company = new Company("company-1", "Acme", new Address("Nantes", 47.2, -1.5));
        TravelerDocument doc = new TravelerDocument("Alice", "Dupont", "alice@example.com", null, true, "company-1");
        when(travelerRepository.findAll()).thenReturn(List.of(doc));
        when(companyService.getCompanyById("company-1")).thenReturn(Optional.of(company));

        List<Traveler> travelers = travelerService.getTravelers();

        assertThat(travelers.getFirst().company()).isEqualTo(company);
    }

    @Test
    void getTravelers_withNullCompanyId_returnsNullCompany() {
        TravelerDocument doc = new TravelerDocument("Bob", "Martin", "bob@example.com", null, false, null);
        when(travelerRepository.findAll()).thenReturn(List.of(doc));

        List<Traveler> travelers = travelerService.getTravelers();

        assertThat(travelers.getFirst().company()).isNull();
    }

    @Test
    void saveTraveler_returnsOriginalTraveler() {
        Traveler traveler = new Traveler(new PersonalInfo("Alice", "Dupont", "alice@example.com",
                new Address("Paris", 48.87, 2.33)));
        when(travelerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Traveler result = travelerService.saveTraveler(traveler);

        assertThat(result).isEqualTo(traveler);
    }

    @Test
    void saveTraveler_convertsFieldsCorrectly() {
        Traveler traveler = new Traveler(
                new PersonalInfo("Alice", "Dupont", "alice@example.com", new Address("Paris", 48.87, 2.33)),
                true);
        when(travelerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        travelerService.saveTraveler(traveler);

        ArgumentCaptor<TravelerDocument> captor = ArgumentCaptor.forClass(TravelerDocument.class);
        verify(travelerRepository).save(captor.capture());
        TravelerDocument doc = captor.getValue();
        assertThat(doc.getFirstName()).isEqualTo("Alice");
        assertThat(doc.getLastName()).isEqualTo("Dupont");
        assertThat(doc.getEmail()).isEqualTo("alice@example.com");
        assertThat(doc.isDriver()).isTrue();
        assertThat(doc.getAddress().fullAddress()).isEqualTo("Paris");
        assertThat(doc.getAddress().location().getLat()).isEqualTo(48.87);
        assertThat(doc.getAddress().location().getLon()).isEqualTo(2.33);
    }

    @Test
    void saveTraveler_withNullAddress_setsNullAddressDocument() {
        Traveler traveler = new Traveler(new PersonalInfo("Bob", "Martin", "bob@example.com", null));
        when(travelerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        travelerService.saveTraveler(traveler);

        ArgumentCaptor<TravelerDocument> captor = ArgumentCaptor.forClass(TravelerDocument.class);
        verify(travelerRepository).save(captor.capture());
        assertThat(captor.getValue().getAddress()).isNull();
    }

    @Test
    void saveTraveler_convertsCompanyToCompanyId() {
        Company company = new Company("company-1", "Acme", new Address("Nantes", 47.2, -1.5));
        Traveler traveler = new Traveler(
                new PersonalInfo("Alice", "Dupont", "alice@example.com", new Address("Paris", 48.87, 2.33)),
                true, company);
        when(travelerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        travelerService.saveTraveler(traveler);

        ArgumentCaptor<TravelerDocument> captor = ArgumentCaptor.forClass(TravelerDocument.class);
        verify(travelerRepository).save(captor.capture());
        assertThat(captor.getValue().getCompanyId()).isEqualTo("company-1");
    }

    @Test
    void saveTraveler_withNullCompany_setsNullCompanyId() {
        Traveler traveler = new Traveler(new PersonalInfo("Bob", "Martin", "bob@example.com", null));
        when(travelerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        travelerService.saveTraveler(traveler);

        ArgumentCaptor<TravelerDocument> captor = ArgumentCaptor.forClass(TravelerDocument.class);
        verify(travelerRepository).save(captor.capture());
        assertThat(captor.getValue().getCompanyId()).isNull();
    }

    @Test
    void findByCompanyId_returnsTravelersWithResolvedCompany() {
        Company company = new Company("company-1", "Acme", new Address("Nantes", 47.2, -1.5));
        TravelerDocument doc = new TravelerDocument("Alice", "Dupont", "alice@example.com", null, false, "company-1");
        when(travelerRepository.findByCompanyId("company-1")).thenReturn(List.of(doc));
        when(companyService.getCompanyById("company-1")).thenReturn(Optional.of(company));

        List<Traveler> result = travelerService.findByCompanyId("company-1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().company()).isEqualTo(company);
    }

    @Test
    void findByCompanyId_withMultipleTravelers_resolvesCompanyOnlyOnce() {
        Company company = new Company("company-1", "Acme", new Address("Nantes", 47.2, -1.5));
        TravelerDocument doc1 = new TravelerDocument("Alice", "Dupont", "alice@example.com", null, false, "company-1");
        TravelerDocument doc2 = new TravelerDocument("Bob", "Martin", "bob@example.com", null, false, "company-1");
        when(travelerRepository.findByCompanyId("company-1")).thenReturn(List.of(doc1, doc2));
        when(companyService.getCompanyById("company-1")).thenReturn(Optional.of(company));

        List<Traveler> result = travelerService.findByCompanyId("company-1");

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(t -> assertThat(t.company()).isEqualTo(company));
        verify(companyService, times(1)).getCompanyById("company-1");
    }

    @Test
    void findByCompanyId_withNoTravelers_doesNotResolveCompany() {
        when(travelerRepository.findByCompanyId("company-1")).thenReturn(List.of());

        List<Traveler> result = travelerService.findByCompanyId("company-1");

        assertThat(result).isEmpty();
        verify(companyService, times(0)).getCompanyById(any());
    }

    @Test
    void importFromCsv_parsesAllRows() {
        String csv = """
            Prénom;Nom;Email;Adresse;Conducteur;
            Clément;Juste;cj@test.fr;Paris, France;Oui;
            Toto;Durand;td@test.fr;Lyon, France;Non;
            """;
        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        Company company = new Company("company-1", "Acme", new Address("Nantes", 47.2, -1.5));
        when(companyService.getCompanyById("company-1")).thenReturn(Optional.of(company));
        when(geocodingService.geocode("Paris, France")).thenReturn(new Address("Paris, France", 48.85, 2.35));
        when(geocodingService.geocode("Lyon, France")).thenReturn(new Address("Lyon, France", 45.75, 4.83));
        when(travelerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Traveler> result = travelerService.importFromCsv(input, "company-1");

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().company()).isEqualTo(company);
        verify(travelerRepository, times(2)).save(any());
    }

    @Test
    void importFromCsv_setsDriverFlag() {
        String csv = """
            Prénom;Nom;Email;Adresse;Conducteur;
            Alice;Dupont;alice@test.fr;Paris, France;Oui;
            Bob;Martin;bob@test.fr;Lyon, France;Non;
            """;
        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        Company company = new Company("company-1", "Acme", new Address("Nantes", 47.2, -1.5));
        when(companyService.getCompanyById("company-1")).thenReturn(Optional.of(company));
        when(geocodingService.geocode(any())).thenReturn(new Address("any", 0, 0));
        when(travelerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Traveler> result = travelerService.importFromCsv(input, "company-1");

        assertThat(result.get(0).isDriver()).isTrue();
        assertThat(result.get(1).isDriver()).isFalse();
    }

    @Test
    void importFromCsv_skipsRowOnGeocodingFailure() {
        String csv = """
            Prénom;Nom;Email;Adresse;Conducteur;
            Alice;Dupont;alice@test.fr;adresse valide;Non;
            Bob;Martin;bob@test.fr;adresse invalide;Non;
            """;
        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        Company company = new Company("company-1", "Acme", new Address("Nantes", 47.2, -1.5));
        when(companyService.getCompanyById("company-1")).thenReturn(Optional.of(company));
        when(geocodingService.geocode("adresse valide")).thenReturn(new Address("adresse valide", 1, 1));
        when(geocodingService.geocode("adresse invalide")).thenThrow(new IllegalArgumentException("not found"));
        when(travelerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Traveler> result = travelerService.importFromCsv(input, "company-1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().personalInfo().firstName()).isEqualTo("Alice");
    }

    @Test
    void importFromCsv_throwsWhenCompanyNotFound() {
        InputStream input = new ByteArrayInputStream("dummy".getBytes(StandardCharsets.UTF_8));
        when(companyService.getCompanyById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> travelerService.importFromCsv(input, "missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
