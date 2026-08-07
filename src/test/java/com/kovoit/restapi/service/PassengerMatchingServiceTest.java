package com.kovoit.restapi.service;

import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.Company;
import com.kovoit.restapi.bean.PassengerMatch;
import com.kovoit.restapi.bean.PersonalInfo;
import com.kovoit.restapi.bean.RoutePoint;
import com.kovoit.restapi.bean.RouteResult;
import com.kovoit.restapi.bean.Traveler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassengerMatchingServiceTest {

    @Mock
    private TravelerService travelerService;

    @Mock
    private RoutingService routingService;

    @Mock
    private GeocodingService geocodingService;

    private PassengerMatchingService service;

    private static final Company COMPANY = new Company("company-1", "Acme", new Address("Nantes", 48.100, 2.000));
    private static final Address DRIVER_ADDRESS = new Address("Rennes", 48.000, 2.000);
    private static final Traveler CONDUCTEUR = new Traveler(
            new PersonalInfo("Alice", "Dupont", "alice@example.com", DRIVER_ADDRESS), true, COMPANY);

    @BeforeEach
    void setUp() {
        service = new PassengerMatchingService(travelerService, routingService, geocodingService, 5, 2, 4, 0.15, 300);
        lenient().when(routingService.route(eq(DRIVER_ADDRESS), eq(COMPANY.address())))
                .thenReturn(new RouteResult(10_000L, 10_000.0, straightRoute()));
    }

    private static List<RoutePoint> straightRoute() {
        return List.of(
                new RoutePoint(48.000, 2.000),
                new RoutePoint(48.025, 2.000),
                new RoutePoint(48.050, 2.000),
                new RoutePoint(48.075, 2.000),
                new RoutePoint(48.100, 2.000));
    }

    private static Traveler passenger(String email, double lat, double lon, boolean isDriver) {
        return new Traveler(new PersonalInfo("P", email, email, new Address("Adr " + email, lat, lon)), isDriver);
    }

    @Test
    void nearestIndexOnRoute_picksClosestPoint() {
        List<RoutePoint> geometry = straightRoute();

        int index = service.nearestIndexOnRoute(geometry, new Address("X", 48.049, 2.000));

        assertThat(index).isEqualTo(2);
    }

    @Test
    void clusterCandidates_groupsCandidatesWithinRadius() {
        RoutePoint pointA = new RoutePoint(48.050, 2.000);
        RoutePoint pointB = new RoutePoint(48.050, 2.001); // ~74m away, within 300m radius
        RoutePoint pointC = new RoutePoint(48.100, 2.000); // far away, own cluster
        PassengerMatchingService.Candidate candidateA =
                new PassengerMatchingService.Candidate(passenger("a@test.fr", 48.050, 2.000, false), pointA, 2, 10L);
        PassengerMatchingService.Candidate candidateB =
                new PassengerMatchingService.Candidate(passenger("b@test.fr", 48.050, 2.001, false), pointB, 2, 20L);
        PassengerMatchingService.Candidate candidateC =
                new PassengerMatchingService.Candidate(passenger("c@test.fr", 48.100, 2.000, false), pointC, 4, 30L);

        List<PassengerMatchingService.Cluster> clusters =
                service.clusterCandidates(List.of(candidateA, candidateB, candidateC));

        assertThat(clusters).hasSize(2);
        assertThat(clusters.get(0).members()).containsExactlyInAnyOrder(candidateA, candidateB);
        assertThat(clusters.get(1).members()).containsExactly(candidateC);
    }

    @Test
    void findPassengers_withoutCompany_returnsEmptyAndDoesNothing() {
        Traveler conducteurSansCompany = new Traveler(new PersonalInfo("Bob", "Martin", "bob@example.com", DRIVER_ADDRESS));

        List<PassengerMatch> result = service.findPassengers(conducteurSansCompany);

        assertThat(result).isEmpty();
        verifyNoInteractions(travelerService, routingService, geocodingService);
    }

    @Test
    void findPassengers_withNullDriverAddress_returnsEmptyAndDoesNothing() {
        Traveler conducteurSansAdresse = new Traveler(
                new PersonalInfo("Alice", "Dupont", "alice@example.com", null), true, COMPANY);

        List<PassengerMatch> result = service.findPassengers(conducteurSansAdresse);

        assertThat(result).isEmpty();
        verifyNoInteractions(travelerService, routingService, geocodingService);
    }

    @Test
    void findPassengers_withNullCompanyAddress_returnsEmptyAndDoesNothing() {
        Company companySansAdresse = new Company("company-1", "Acme", null);
        Traveler conducteur = new Traveler(
                new PersonalInfo("Alice", "Dupont", "alice@example.com", DRIVER_ADDRESS), true, companySansAdresse);

        List<PassengerMatch> result = service.findPassengers(conducteur);

        assertThat(result).isEmpty();
        verifyNoInteractions(travelerService, routingService, geocodingService);
    }

    @Test
    void findPassengers_noCandidates_returnsEmpty() {
        when(travelerService.findByCompanyId("company-1")).thenReturn(List.of());

        List<PassengerMatch> result = service.findPassengers(CONDUCTEUR);

        assertThat(result).isEmpty();
    }

    @Test
    void findPassengers_excludesCandidateOutsideFiveMinuteRadius() {
        Traveler farCandidate = passenger("far@test.fr", 48.050, 2.100, false);
        when(travelerService.findByCompanyId("company-1")).thenReturn(List.of(farCandidate));
        Address stopPoint = new Address("Point de rendez-vous", 48.050, 2.000);
        when(routingService.route(eq(farCandidate.personalInfo().address()), eq(stopPoint)))
                .thenReturn(new RouteResult(10_000L, 10_000.0, List.of()));

        List<PassengerMatch> result = service.findPassengers(CONDUCTEUR);

        assertThat(result).isEmpty();
    }

    @Test
    void findPassengers_twoCandidatesShareStop_bothIncludedWithSameRendezVous() {
        Traveler c1 = passenger("c1@test.fr", 48.050, 2.001, false);
        Traveler c2 = passenger("c2@test.fr", 48.050, 2.0012, false);
        when(travelerService.findByCompanyId("company-1")).thenReturn(List.of(c1, c2));

        Address stopPoint = new Address("Point de rendez-vous", 48.050, 2.000);
        when(routingService.route(eq(c1.personalInfo().address()), eq(stopPoint)))
                .thenReturn(new RouteResult(100L, 100.0, List.of()));
        when(routingService.route(eq(c2.personalInfo().address()), eq(stopPoint)))
                .thenReturn(new RouteResult(150L, 150.0, List.of()));
        when(routingService.routeMulti(List.of(DRIVER_ADDRESS, stopPoint, COMPANY.address())))
                .thenReturn(new RouteResult(10_050L, 10_050.0, List.of()));
        when(geocodingService.reverseGeocode(48.050, 2.000)).thenReturn(new Address("Arrêt", 48.050, 2.000));

        List<PassengerMatch> result = service.findPassengers(CONDUCTEUR);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(m -> m.passenger().personalInfo().email())
                .containsExactlyInAnyOrder("c1@test.fr", "c2@test.fr");
        assertThat(result).allSatisfy(m -> assertThat(m.rendezVous().fullAddress()).isEqualTo("Arrêt"));
    }

    @Test
    void findPassengers_excludesSubsetWhenTotalDetourExceedsRatioOnShortBaseRoute() {
        PassengerMatchingService shortRouteService =
                new PassengerMatchingService(travelerService, routingService, geocodingService, 5, 2, 4, 0.15, 300);
        when(routingService.route(eq(DRIVER_ADDRESS), eq(COMPANY.address())))
                .thenReturn(new RouteResult(1_000L, 1_000.0, straightRoute()));

        Traveler candidate = passenger("c@test.fr", 48.050, 2.001, false);
        when(travelerService.findByCompanyId("company-1")).thenReturn(List.of(candidate));

        Address stopPoint = new Address("Point de rendez-vous", 48.050, 2.000);
        when(routingService.route(eq(candidate.personalInfo().address()), eq(stopPoint)))
                .thenReturn(new RouteResult(100L, 100.0, List.of()));
        // 200s detour <= 300s per-stop cap, but > 15% of 1_000s (150s) -> rejected on ratio.
        when(routingService.routeMulti(List.of(DRIVER_ADDRESS, stopPoint, COMPANY.address())))
                .thenReturn(new RouteResult(1_200L, 1_200.0, List.of()));

        List<PassengerMatch> result = shortRouteService.findPassengers(CONDUCTEUR);

        assertThat(result).isEmpty();
    }

    @Test
    void findPassengers_truncatesToMaxPassengers() {
        List<Traveler> candidates = List.of(
                passenger("p1@test.fr", 48.050, 2.0001, false),
                passenger("p2@test.fr", 48.050, 2.0002, false),
                passenger("p3@test.fr", 48.050, 2.0003, false),
                passenger("p4@test.fr", 48.050, 2.0004, false),
                passenger("p5@test.fr", 48.050, 2.0005, false));
        when(travelerService.findByCompanyId("company-1")).thenReturn(candidates);

        Address stopPoint = new Address("Point de rendez-vous", 48.050, 2.000);
        long[] durations = {50L, 60L, 70L, 80L, 90L};
        for (int i = 0; i < candidates.size(); i++) {
            when(routingService.route(eq(candidates.get(i).personalInfo().address()), eq(stopPoint)))
                    .thenReturn(new RouteResult(durations[i], durations[i], List.of()));
        }
        when(routingService.routeMulti(List.of(DRIVER_ADDRESS, stopPoint, COMPANY.address())))
                .thenReturn(new RouteResult(10_090L, 10_090.0, List.of()));
        when(geocodingService.reverseGeocode(48.050, 2.000)).thenReturn(new Address("Arrêt", 48.050, 2.000));

        List<PassengerMatch> result = service.findPassengers(CONDUCTEUR);

        assertThat(result).hasSize(4);
        assertThat(result).extracting(m -> m.passenger().personalInfo().email())
                .containsExactlyInAnyOrder("p1@test.fr", "p2@test.fr", "p3@test.fr", "p4@test.fr");
    }

    @Test
    void findPassengers_includesDriverCandidateAsPassenger() {
        Traveler otherDriver = passenger("other-driver@test.fr", 48.050, 2.001, true);
        when(travelerService.findByCompanyId("company-1")).thenReturn(List.of(otherDriver));

        Address stopPoint = new Address("Point de rendez-vous", 48.050, 2.000);
        when(routingService.route(eq(otherDriver.personalInfo().address()), eq(stopPoint)))
                .thenReturn(new RouteResult(100L, 100.0, List.of()));
        when(routingService.routeMulti(List.of(DRIVER_ADDRESS, stopPoint, COMPANY.address())))
                .thenReturn(new RouteResult(10_100L, 10_100.0, List.of()));
        when(geocodingService.reverseGeocode(48.050, 2.000)).thenReturn(new Address("Arrêt", 48.050, 2.000));

        List<PassengerMatch> result = service.findPassengers(CONDUCTEUR);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().passenger().personalInfo().email()).isEqualTo("other-driver@test.fr");
    }
}
