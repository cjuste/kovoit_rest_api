package com.kovoit.restapi.service;

import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.PassengerMatch;
import com.kovoit.restapi.bean.RoutePoint;
import com.kovoit.restapi.bean.RouteResult;
import com.kovoit.restapi.bean.Traveler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PassengerMatchingService {

    private final TravelerService travelerService;
    private final RoutingService routingService;
    private final GeocodingService geocodingService;

    private final long maxDetourSecondsPerStop;
    private final int maxStops;
    private final int maxPassengers;
    private final double maxRouteIncreaseRatio;
    private final double stopClusterRadiusMeters;

    public PassengerMatchingService(
            TravelerService travelerService,
            RoutingService routingService,
            GeocodingService geocodingService,
            @Value("${carpool.max-detour-minutes-per-stop:5}") int maxDetourMinutesPerStop,
            @Value("${carpool.max-stops:2}") int maxStops,
            @Value("${carpool.max-passengers:4}") int maxPassengers,
            @Value("${carpool.max-route-increase-ratio:0.15}") double maxRouteIncreaseRatio,
            @Value("${carpool.stop-cluster-radius-meters:300}") double stopClusterRadiusMeters) {
        this.travelerService = travelerService;
        this.routingService = routingService;
        this.geocodingService = geocodingService;
        this.maxDetourSecondsPerStop = maxDetourMinutesPerStop * 60L;
        this.maxStops = maxStops;
        this.maxPassengers = maxPassengers;
        this.maxRouteIncreaseRatio = maxRouteIncreaseRatio;
        this.stopClusterRadiusMeters = stopClusterRadiusMeters;
    }

    public List<PassengerMatch> findPassengers(Traveler conducteur) {
        if (conducteur.company() == null) {
            return List.of();
        }
        Address destination = conducteur.company().address();
        Address driverAddress = conducteur.personalInfo().address();
        if (destination == null || driverAddress == null) {
            return List.of();
        }
        RouteResult baseRoute = routingService.route(driverAddress, destination);

        List<Traveler> candidats = travelerService.findByCompanyId(conducteur.company().id()).stream()
                .filter(t -> !t.personalInfo().email().equals(conducteur.personalInfo().email()))
                .toList();
        if (candidats.isEmpty()) {
            return List.of();
        }

        List<Candidate> viable = new ArrayList<>();
        for (Traveler candidat : candidats) {
            Address address = candidat.personalInfo().address();
            if (address == null) {
                continue;
            }
            int routeIndex = nearestIndexOnRoute(baseRoute.geometry(), address);
            if (routeIndex < 0) {
                continue;
            }
            RoutePoint point = baseRoute.geometry().get(routeIndex);
            long legDuration = routingService.route(address, toAddress(point)).durationSeconds();
            if (legDuration <= maxDetourSecondsPerStop) {
                viable.add(new Candidate(candidat, point, routeIndex, legDuration));
            }
        }
        if (viable.isEmpty()) {
            return List.of();
        }

        List<Cluster> clusters = revalidateClusters(clusterCandidates(viable));
        if (clusters.isEmpty()) {
            return List.of();
        }

        List<Cluster> bestSubset = selectBestSubset(clusters, driverAddress, destination, baseRoute.durationSeconds());
        if (bestSubset.isEmpty()) {
            return List.of();
        }

        List<Candidate> selected = bestSubset.stream()
                .flatMap(c -> c.members().stream())
                .sorted(Comparator.comparingLong(Candidate::legDurationSeconds))
                .limit(maxPassengers)
                .toList();

        List<PassengerMatch> result = new ArrayList<>();
        for (Candidate candidate : selected) {
            Cluster cluster = bestSubset.stream()
                    .filter(c -> c.members().contains(candidate))
                    .findFirst()
                    .orElseThrow();
            Address rendezVous = geocodingService.reverseGeocode(cluster.point().lat(), cluster.point().lon());
            result.add(new PassengerMatch(candidate.traveler(), rendezVous));
        }
        return result;
    }

    int nearestIndexOnRoute(List<RoutePoint> geometry, Address address) {
        int bestIndex = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < geometry.size(); i++) {
            RoutePoint point = geometry.get(i);
            double distance = haversineMeters(address.lat(), address.lon(), point.lat(), point.lon());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusMeters = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusMeters * c;
    }

    List<Cluster> clusterCandidates(List<Candidate> viable) {
        List<Candidate> sorted = viable.stream()
                .sorted(Comparator.comparingInt(Candidate::routeIndex))
                .toList();
        List<Cluster> clusters = new ArrayList<>();
        for (Candidate candidate : sorted) {
            Cluster match = null;
            for (Cluster cluster : clusters) {
                double distance = haversineMeters(
                        cluster.point().lat(), cluster.point().lon(),
                        candidate.point().lat(), candidate.point().lon());
                if (distance <= stopClusterRadiusMeters) {
                    match = cluster;
                    break;
                }
            }
            if (match != null) {
                match.members().add(candidate);
            } else {
                List<Candidate> members = new ArrayList<>();
                members.add(candidate);
                clusters.add(new Cluster(candidate.point(), candidate.routeIndex(), members));
            }
        }
        return clusters;
    }

    private List<Cluster> revalidateClusters(List<Cluster> clusters) {
        List<Cluster> result = new ArrayList<>();
        for (Cluster cluster : clusters) {
            List<Candidate> validMembers = new ArrayList<>();
            for (Candidate member : cluster.members()) {
                long duration = routingService.route(member.traveler().personalInfo().address(), toAddress(cluster.point()))
                        .durationSeconds();
                if (duration <= maxDetourSecondsPerStop) {
                    validMembers.add(new Candidate(member.traveler(), cluster.point(), member.routeIndex(), duration));
                }
            }
            if (!validMembers.isEmpty()) {
                result.add(new Cluster(cluster.point(), cluster.routeIndex(), validMembers));
            }
        }
        return result;
    }

    private List<Cluster> selectBestSubset(List<Cluster> clusters, Address driverAddress, Address destination,
                                            long baseDurationSeconds) {
        List<Cluster> best = List.of();
        long bestDetour = Long.MAX_VALUE;
        int bestCount = 0;
        long maxTotalDetour = Math.round(baseDurationSeconds * maxRouteIncreaseRatio);

        for (List<Cluster> combo : combinationsUpTo(clusters, maxStops)) {
            long total = routeDuration(driverAddress, destination, combo, baseDurationSeconds);
            long totalDetour = total - baseDurationSeconds;
            if (totalDetour > maxTotalDetour) {
                continue;
            }

            boolean perStopOk = true;
            for (Cluster excluded : combo) {
                List<Cluster> without = combo.stream().filter(c -> c != excluded).toList();
                long withoutDuration = routeDuration(driverAddress, destination, without, baseDurationSeconds);
                long marginal = total - withoutDuration;
                if (marginal > maxDetourSecondsPerStop) {
                    perStopOk = false;
                    break;
                }
            }
            if (!perStopOk) {
                continue;
            }

            int count = combo.stream().mapToInt(c -> c.members().size()).sum();
            if (count > bestCount || (count == bestCount && totalDetour < bestDetour)) {
                best = combo;
                bestCount = count;
                bestDetour = totalDetour;
            }
        }
        return best;
    }

    private long routeDuration(Address driverAddress, Address destination, List<Cluster> stops, long baseDurationSeconds) {
        if (stops.isEmpty()) {
            return baseDurationSeconds;
        }
        List<Address> waypoints = new ArrayList<>();
        waypoints.add(driverAddress);
        stops.forEach(c -> waypoints.add(toAddress(c.point())));
        waypoints.add(destination);
        return routingService.routeMulti(waypoints).durationSeconds();
    }

    private List<List<Cluster>> combinationsUpTo(List<Cluster> clusters, int maxSize) {
        List<List<Cluster>> result = new ArrayList<>();
        int limit = Math.min(maxSize, clusters.size());
        for (int size = 1; size <= limit; size++) {
            combine(clusters, size, 0, new ArrayList<>(), result);
        }
        return result;
    }

    private void combine(List<Cluster> clusters, int size, int start, List<Cluster> current, List<List<Cluster>> result) {
        if (current.size() == size) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < clusters.size(); i++) {
            current.add(clusters.get(i));
            combine(clusters, size, i + 1, current, result);
            current.removeLast();
        }
    }

    private Address toAddress(RoutePoint point) {
        return new Address("Point de rendez-vous", point.lat(), point.lon());
    }

    record Candidate(Traveler traveler, RoutePoint point, int routeIndex, long legDurationSeconds) {}

    record Cluster(RoutePoint point, int routeIndex, List<Candidate> members) {}
}
