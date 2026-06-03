package cn.edu.cn.javadiet.service;

public interface LocationService {

    double[] geocode(String address);

    double calculateDistanceKm(double startLatitude, double startLongitude, double endLatitude, double endLongitude);
}
