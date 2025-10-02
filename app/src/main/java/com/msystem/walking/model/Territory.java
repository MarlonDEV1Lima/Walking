package com.msystem.walking.model;

import com.msystem.walking.utils.TerritoryUtils;

import java.util.Date;
import java.util.List;

public class Territory {
    private String territoryId;
    private String ownerId;
    private String ownerName;
    private List<LocationPoint> polygon; // Pontos que formam o polígono do território
    private double area; // Área em metros quadrados
    private int pointsValue;
    private Date conqueredAt;
    private String color; // Cor hexadecimal para exibir no mapa
    private String region; // Região geográfica (cidade, bairro, etc.)

    public Territory() {
        // Construtor vazio necessário para Firebase
        // ✅ Inicializar com valores padrão seguros
        this.area = 0.0;
        this.pointsValue = 0;
        this.conqueredAt = new Date();
    }

    public Territory(String ownerId, String ownerName, List<LocationPoint> polygon) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.polygon = polygon;
        this.conqueredAt = new Date();

        // ✅ Validação antes de calcular área e pontos
        if (polygon != null && !polygon.isEmpty()) {
            this.area = TerritoryUtils.calculatePolygonArea(polygon);
            this.pointsValue = TerritoryUtils.calculatePointsFromArea(this.area);
        } else {
            this.area = 0.0;
            this.pointsValue = 0;
        }

        // ✅ Validação antes de gerar cor
        if (ownerId != null) {
            this.color = TerritoryUtils.generateTerritoryColor(ownerId);
        } else {
            this.color = "#808080"; // Cor padrão cinza
        }
    }

    // Getters e Setters
    public String getTerritoryId() {
        return territoryId;
    }

    public void setTerritoryId(String territoryId) {
        this.territoryId = territoryId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public List<LocationPoint> getPolygon() {
        return polygon;
    }

    public void setPolygon(List<LocationPoint> polygon) {
        this.polygon = polygon;
        // ✅ Recalcular área e pontos apenas se polygon for válido
        if (polygon != null && !polygon.isEmpty()) {
            this.area = TerritoryUtils.calculatePolygonArea(polygon);
            this.pointsValue = TerritoryUtils.calculatePointsFromArea(this.area);
        }
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
        this.pointsValue = TerritoryUtils.calculatePointsFromArea(area);
    }

    public int getPointsValue() {
        return pointsValue;
    }

    public void setPointsValue(int pointsValue) {
        this.pointsValue = pointsValue;
    }

    public Date getConqueredAt() {
        return conqueredAt;
    }

    public void setConqueredAt(Date conqueredAt) {
        this.conqueredAt = conqueredAt;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * Verifica se este território é válido
     */
    public boolean isValid() {
        return ownerId != null && ownerName != null &&
                polygon != null && !polygon.isEmpty() &&
                TerritoryUtils.isValidTerritory(this.polygon);
    }

    /**
     * Calcula a distância do centro deste território para um ponto
     */
    public double distanceFromCenter(LocationPoint point) {
        if (polygon == null || polygon.isEmpty() || point == null) return Double.MAX_VALUE;

        // ✅ Usar try-catch para stream operations
        try {
            // Calcular centro do território
            double centerLat = polygon.stream().mapToDouble(LocationPoint::getLatitude).average().orElse(0);
            double centerLng = polygon.stream().mapToDouble(LocationPoint::getLongitude).average().orElse(0);

            LocationPoint center = new LocationPoint(centerLat, centerLng);
            return TerritoryUtils.calculateDistance(center, point);
        } catch (Exception e) {
            return Double.MAX_VALUE;
        }
    }

    /**
     * Verifica se um ponto está dentro deste território
     */
    public boolean containsPoint(LocationPoint point) {
        if (polygon == null || polygon.isEmpty() || point == null) {
            return false;
        }

        try {
            return TerritoryUtils.isPointInTerritory(point, this.polygon);
        } catch (Exception e) {
            return false;
        }
    }
}
