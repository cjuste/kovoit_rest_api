package com.kovoit.restapi.bean;

import java.util.List;

public record RouteResult(long durationSeconds, double distanceMeters, List<RoutePoint> geometry) {
}
