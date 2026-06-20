package com.apptasticsoftware.rssreader.module.georss;

import java.util.Objects;
import java.util.stream.Stream;

public class GeoFenceFilter {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final Coordinate center;
    private final double radiusKm;

    public GeoFenceFilter(Coordinate center, double radiusKm) {
        Objects.requireNonNull(center, "Center coordinate must not be null");
        if (radiusKm < 0) {
            throw new IllegalArgumentException("Radius must not be negative");
        }
        this.center = center;
        this.radiusKm = radiusKm;
    }

    public Stream<GeoRssItem> filter(Stream<GeoRssItem> items) {
        Objects.requireNonNull(items, "Items stream must not be null");
        return items.filter(this::isWithinFence);
    }

    private boolean isWithinFence(GeoRssItem item) {
        boolean pointWithin = item.getGeoRssPointAsCoordinate()
                .map(this::isCoordinateWithinFence)
                .orElse(false);
        if (pointWithin) {
            return true;
        }

        boolean lineWithin = item.getGeoRssLineAsCoordinates().stream()
                .anyMatch(this::isCoordinateWithinFence);
        if (lineWithin) {
            return true;
        }

        boolean polygonWithin = item.getGeoRssPolygonAsCoordinates().stream()
                .anyMatch(this::isCoordinateWithinFence);
        if (polygonWithin) {
            return true;
        }

        boolean boxWithin = item.getGeoRssBoxAsCoordinates().stream()
                .anyMatch(this::isCoordinateWithinFence);
        return boxWithin;
    }

    boolean isCoordinateWithinFence(Coordinate coordinate) {
        double distance = haversineDistance(center, coordinate);
        return distance <= radiusKm;
    }

    static double haversineDistance(Coordinate from, Coordinate to) {
        double latDiff = Math.toRadians(to.getLatitude() - from.getLatitude());
        double lonDiff = Math.toRadians(to.getLongitude() - from.getLongitude());
        double sinLat = Math.sin(latDiff / 2);
        double sinLon = Math.sin(lonDiff / 2);
        double a = sinLat * sinLat + Math.cos(Math.toRadians(from.getLatitude())) * Math.cos(Math.toRadians(to.getLatitude())) * sinLon * sinLon;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public Coordinate getCenter() {
        return center;
    }

    public double getRadiusKm() {
        return radiusKm;
    }
}
