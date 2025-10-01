package com.msystem.walking.utils;

import com.msystem.walking.model.LocationPoint;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class TerritoryUtils {

    /**
     * Calcula a área de um polígono usando o algoritmo Shoelace
     * @param polygon Lista de pontos que formam o polígono
     * @return Área em metros quadrados
     */
    public static double calculatePolygonArea(List<LocationPoint> polygon) {
        if (polygon.size() < 3) return 0.0;

        double area = 0.0;
        int n = polygon.size();

        // Converter para coordenadas em metros (aproximação simples)
        for (int i = 0; i < n; i++) {
            LocationPoint current = polygon.get(i);
            LocationPoint next = polygon.get((i + 1) % n);

            // Converter lat/lng para metros (aproximação para pequenas áreas)
            double x1 = current.getLongitude() * 111320 * Math.cos(Math.toRadians(current.getLatitude()));
            double y1 = current.getLatitude() * 110540;
            double x2 = next.getLongitude() * 111320 * Math.cos(Math.toRadians(next.getLatitude()));
            double y2 = next.getLatitude() * 110540;

            area += (x1 * y2 - x2 * y1);
        }

        return Math.abs(area) / 2.0;
    }

    /**
     * Calcula o centro geométrico (centroide) de um polígono
     * @param polygon Lista de pontos do polígono
     * @return Ponto central do polígono
     */
    public static LatLng calculatePolygonCenter(List<LocationPoint> polygon) {
        if (polygon.isEmpty()) return new LatLng(0, 0);

        double sumLat = 0.0;
        double sumLng = 0.0;

        for (LocationPoint point : polygon) {
            sumLat += point.getLatitude();
            sumLng += point.getLongitude();
        }

        return new LatLng(sumLat / polygon.size(), sumLng / polygon.size());
    }

    /**
     * Verifica se um ponto está dentro de um território
     * @param point Ponto a ser verificado
     * @param territory Lista de pontos que formam o território
     * @return true se o ponto está dentro do território
     */
    public static boolean isPointInTerritory(LocationPoint point, List<LocationPoint> territory) {
        if (territory.size() < 3) return false;

        int intersections = 0;
        int n = territory.size();

        for (int i = 0; i < n; i++) {
            LocationPoint p1 = territory.get(i);
            LocationPoint p2 = territory.get((i + 1) % n);

            if (rayIntersectsSegment(point, p1, p2)) {
                intersections++;
            }
        }

        return (intersections % 2) == 1;
    }

    private static boolean rayIntersectsSegment(LocationPoint point, LocationPoint p1, LocationPoint p2) {
        double px = point.getLongitude();
        double py = point.getLatitude();
        double p1x = p1.getLongitude();
        double p1y = p1.getLatitude();
        double p2x = p2.getLongitude();
        double p2y = p2.getLatitude();

        if (p1y > py != p2y > py) {
            double intersectX = (p2x - p1x) * (py - p1y) / (p2y - p1y) + p1x;
            return px < intersectX;
        }

        return false;
    }

    /**
     * Calcula a distância entre dois pontos em metros
     * @param point1 Primeiro ponto
     * @param point2 Segundo ponto
     * @return Distância em metros
     */
    public static double calculateDistance(LocationPoint point1, LocationPoint point2) {
        final int R = 6371000; // Raio da Terra em metros

        double lat1Rad = Math.toRadians(point1.getLatitude());
        double lat2Rad = Math.toRadians(point2.getLatitude());
        double deltaLat = Math.toRadians(point2.getLatitude() - point1.getLatitude());
        double deltaLng = Math.toRadians(point2.getLongitude() - point1.getLongitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Simplifica um polígono removendo pontos muito próximos
     * @param polygon Polígono original
     * @param minDistance Distância mínima entre pontos (em metros)
     * @return Polígono simplificado
     */
    public static List<LocationPoint> simplifyPolygon(List<LocationPoint> polygon, double minDistance) {
        if (polygon.size() < 2) return polygon;

        List<LocationPoint> simplified = new ArrayList<>();
        simplified.add(polygon.get(0));

        for (int i = 1; i < polygon.size(); i++) {
            LocationPoint current = polygon.get(i);
            LocationPoint last = simplified.get(simplified.size() - 1);

            if (calculateDistance(current, last) >= minDistance) {
                simplified.add(current);
            }
        }

        return simplified;
    }

    /**
     * Gera uma cor para território baseada no ID do usuário
     * @param userId ID do usuário para gerar cor consistente
     * @return Cor em formato hexadecimal
     */
    public static String generateTerritoryColor(String userId) {
        // Cores predefinidas para territórios
        String[] colors = {
            "#E57373", "#F06292", "#BA68C8", "#9575CD",
            "#7986CB", "#64B5F6", "#4FC3F7", "#4DD0E1",
            "#4DB6AC", "#81C784", "#AED581", "#DCE775",
            "#FFF176", "#FFD54F", "#FFB74D", "#FF8A65"
        };

        // Usar hash do userId para cor consistente
        int hash = Math.abs(userId.hashCode());
        return colors[hash % colors.length];
    }

    /**
     * Calcula pontos ganhos baseado na área do território
     * @param area Área em metros quadrados
     * @return Pontos ganhos
     */
    public static int calculatePointsFromArea(double area) {
        // 1 ponto para cada 50 metros quadrados
        // Bonificação para territórios maiores
        int basePoints = (int) (area / 50);

        if (area > 10000) { // Territórios > 1 hectare
            basePoints = (int) (basePoints * 1.5);
        } else if (area > 5000) { // Territórios > 5000m²
            basePoints = (int) (basePoints * 1.2);
        }

        return Math.max(basePoints, 1); // Mínimo 1 ponto
    }

    /**
     * Verifica se um território é válido (área mínima, forma adequada, etc.)
     * @param polygon Pontos do território
     * @return true se o território é válido
     */
    public static boolean isValidTerritory(List<LocationPoint> polygon) {
        if (polygon.size() < 3) return false;

        double area = calculatePolygonArea(polygon);

        // Área mínima de 100m²
        if (area < 100) return false;

        // Área máxima de 50.000m² (5 hectares) para evitar territórios enormes
        if (area > 50000) return false;

        return true;
    }
}
