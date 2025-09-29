package com.msystem.walking.utils;

import com.msystem.walking.model.LocationPoint;
import com.msystem.walking.model.Territory;

import java.util.ArrayList;
import java.util.List;

public class TerritoryUtils {

    // Raio em metros para criar território ao redor da rota
    private static final double TERRITORY_RADIUS = 50.0;

    /**
     * Cria territórios baseados na rota percorrida pelo usuário
     * @param route Lista de pontos da rota
     * @param userId ID do usuário
     * @param userName Nome do usuário
     * @return Lista de territórios conquistados
     */
    public static List<Territory> createTerritoriesFromRoute(List<LocationPoint> route, String userId, String userName) {
        List<Territory> territories = new ArrayList<>();

        if (route == null || route.size() < 2) {
            return territories;
        }

        // Criar território a cada 100 metros aproximadamente
        int pointInterval = Math.max(1, route.size() / 10);

        for (int i = 0; i < route.size(); i += pointInterval) {
            LocationPoint center = route.get(i);
            List<LocationPoint> polygon = createCircularPolygon(center, TERRITORY_RADIUS);

            Territory territory = new Territory(userId, userName, polygon);
            territory.setArea(Math.PI * TERRITORY_RADIUS * TERRITORY_RADIUS); // Área do círculo
            territory.setColor(generateColorForUser(userId));
            territories.add(territory);
        }

        return territories;
    }

    /**
     * Cria um polígono circular ao redor de um ponto central
     * @param center Ponto central
     * @param radius Raio em metros
     * @return Lista de pontos formando um círculo
     */
    private static List<LocationPoint> createCircularPolygon(LocationPoint center, double radius) {
        List<LocationPoint> polygon = new ArrayList<>();
        int numberOfPoints = 12; // 12 pontos para formar um círculo aproximado

        // Converter raio de metros para graus (aproximação)
        double radiusInDegrees = radius / 111000.0; // 1 grau ≈ 111 km

        for (int i = 0; i < numberOfPoints; i++) {
            double angle = 2 * Math.PI * i / numberOfPoints;
            double lat = center.getLatitude() + radiusInDegrees * Math.cos(angle);
            double lon = center.getLongitude() + radiusInDegrees * Math.sin(angle) / Math.cos(Math.toRadians(center.getLatitude()));

            polygon.add(new LocationPoint(lat, lon));
        }

        return polygon;
    }

    /**
     * Gera uma cor única baseada no ID do usuário
     * @param userId ID do usuário
     * @return Cor em formato hexadecimal
     */
    private static String generateColorForUser(String userId) {
        int hash = userId.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;

        // Garantir que as cores não sejam muito escuras
        r = Math.max(r, 100);
        g = Math.max(g, 100);
        b = Math.max(b, 100);

        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * Calcula a distância entre dois pontos em metros
     * @param point1 Primeiro ponto
     * @param point2 Segundo ponto
     * @return Distância em metros
     */
    public static double calculateDistance(LocationPoint point1, LocationPoint point2) {
        final int R = 6371; // Raio da Terra em km

        double latDistance = Math.toRadians(point2.getLatitude() - point1.getLatitude());
        double lonDistance = Math.toRadians(point2.getLongitude() - point1.getLongitude());

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(point1.getLatitude())) * Math.cos(Math.toRadians(point2.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // converter para metros

        return distance;
    }
}
