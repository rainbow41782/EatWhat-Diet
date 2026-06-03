package cn.edu.cn.javadiet.service.impl;

import cn.edu.cn.javadiet.service.LocationService;
import org.springframework.stereotype.Service;

@Service
public class LocationServiceImpl implements LocationService {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    @Override
    public double[] geocode(String address) {
        throw new IllegalStateException("map api key is not configured");
    }

    @Override
    public double calculateDistanceKm(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude) {
        double startLatRadians = Math.toRadians(startLatitude);
        double endLatRadians = Math.toRadians(endLatitude);
        double deltaLat = Math.toRadians(endLatitude - startLatitude);
        double deltaLon = Math.toRadians(endLongitude - startLongitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(startLatRadians) * Math.cos(endLatRadians)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
