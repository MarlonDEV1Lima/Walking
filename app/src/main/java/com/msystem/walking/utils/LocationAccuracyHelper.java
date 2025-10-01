package com.msystem.walking.utils;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.util.Log;

public class LocationAccuracyHelper {

    private static final String TAG = "LocationAccuracy";

    // Constantes para níveis de precisão
    public static final float EXCELLENT_ACCURACY = 5f;  // 5 metros
    public static final float GOOD_ACCURACY = 10f;      // 10 metros
    public static final float FAIR_ACCURACY = 20f;      // 20 metros
    public static final float POOR_ACCURACY = 50f;      // 50 metros

    /**
     * Avalia a qualidade da localização recebida
     */
    public static LocationQuality evaluateLocationQuality(Location location) {
        if (location == null) {
            return LocationQuality.NO_LOCATION;
        }

        float accuracy = location.getAccuracy();
        long age = System.currentTimeMillis() - location.getTime();

        // Verificar se a localização é muito antiga (mais de 2 minutos)
        if (age > 2 * 60 * 1000) {
            return LocationQuality.OUTDATED;
        }

        // Avaliar precisão
        if (accuracy <= EXCELLENT_ACCURACY) {
            return LocationQuality.EXCELLENT;
        } else if (accuracy <= GOOD_ACCURACY) {
            return LocationQuality.GOOD;
        } else if (accuracy <= FAIR_ACCURACY) {
            return LocationQuality.FAIR;
        } else if (accuracy <= POOR_ACCURACY) {
            return LocationQuality.POOR;
        } else {
            return LocationQuality.VERY_POOR;
        }
    }

    /**
     * Retorna uma mensagem explicativa sobre a qualidade da localização
     */
    public static String getLocationQualityMessage(LocationQuality quality) {
        switch (quality) {
            case EXCELLENT:
                return "GPS com excelente precisão (±5m)";
            case GOOD:
                return "GPS com boa precisão (±10m)";
            case FAIR:
                return "GPS com precisão razoável (±20m)";
            case POOR:
                return "GPS com baixa precisão (±50m)";
            case VERY_POOR:
                return "GPS com precisão muito baixa (>50m)";
            case OUTDATED:
                return "Localização desatualizada";
            case NO_LOCATION:
                return "Nenhuma localização disponível";
            default:
                return "Qualidade desconhecida";
        }
    }

    /**
     * Verifica se a localização é confiável para rastreamento de caminhada
     */
    public static boolean isLocationReliableForTracking(Location location) {
        LocationQuality quality = evaluateLocationQuality(location);
        return quality == LocationQuality.EXCELLENT ||
               quality == LocationQuality.GOOD ||
               quality == LocationQuality.FAIR;
    }

    /**
     * Verifica se está rodando no emulador
     */
    public static boolean isRunningOnEmulator() {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator");
    }

    /**
     * Verifica se a localização parece ser do Google HQ (emulador não configurado)
     */
    public static boolean isGoogleHQLocation(Location location) {
        if (location == null) return false;

        // Coordenadas aproximadas do Google HQ em Mountain View, CA
        double googleLat = 37.4220936;
        double googleLng = -122.083922;

        // Verificar se está muito próximo do Google HQ (dentro de 10km)
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                googleLat, googleLng, results);

        return results[0] < 10000; // 10km de distância
    }

    /**
     * Retorna sugestões para melhorar a precisão do GPS
     */
    public static String getImprovementSuggestions(Context context) {
        StringBuilder suggestions = new StringBuilder();

        if (isRunningOnEmulator()) {
            suggestions.append("DETECTADO: Você está rodando no emulador Android.\n\n");
            suggestions.append("Para configurar sua localização real:\n\n");
            suggestions.append("1. No Android Studio, vá em Tools > AVD Manager\n");
            suggestions.append("2. Clique nos '...' do seu emulador e selecione 'Extended controls'\n");
            suggestions.append("3. Vá na seção 'Location' no menu lateral\n");
            suggestions.append("4. Digite suas coordenadas reais ou use 'Search' para encontrar sua cidade\n");
            suggestions.append("5. Clique em 'Send' para aplicar a nova localização\n\n");
            suggestions.append("Alternativamente:\n");
            suggestions.append("- Use um dispositivo físico para testes de GPS\n");
            suggestions.append("- Configure GPS mock location apps no emulador");
        } else {
            suggestions.append("Para melhorar a precisão do GPS:\n\n");
            suggestions.append("1. Certifique-se de estar ao ar livre com céu aberto\n");
            suggestions.append("2. Aguarde alguns minutos para o GPS se calibrar\n");
            suggestions.append("3. Verifique se o modo 'Alta precisão' está ativado\n");
            suggestions.append("4. Reinicie o GPS desligando e ligando novamente\n");
            suggestions.append("5. Limpe o cache do Google Play Services\n");
            suggestions.append("6. Evite áreas com muitos prédios altos ou cobertura densa");
        }

        return suggestions.toString();
    }

    /**
     * Log detalhado da localização para debug
     */
    public static void logLocationDetails(Location location, String source) {
        if (location == null) {
            Log.w(TAG, source + ": Localização é null");
            return;
        }

        long age = System.currentTimeMillis() - location.getTime();
        LocationQuality quality = evaluateLocationQuality(location);

        String emulatorInfo = isRunningOnEmulator() ? " [EMULADOR]" : " [DISPOSITIVO]";
        String googleHQInfo = isGoogleHQLocation(location) ? " [GOOGLE HQ - Configure localização no emulador!]" : "";

        Log.d(TAG, String.format(
            "%s: Lat=%.6f, Lng=%.6f, Precisão=%.1fm, Idade=%ds, Qualidade=%s, Provedor=%s%s%s",
            source,
            location.getLatitude(),
            location.getLongitude(),
            location.getAccuracy(),
            age / 1000,
            quality.name(),
            location.getProvider(),
            emulatorInfo,
            googleHQInfo
        ));
    }

    /**
     * Verifica se duas localizações são significativamente diferentes
     */
    public static boolean isSignificantLocationChange(Location oldLocation, Location newLocation, float minimumDistance) {
        if (oldLocation == null || newLocation == null) {
            return true;
        }

        float distance = oldLocation.distanceTo(newLocation);
        return distance >= minimumDistance;
    }

    public enum LocationQuality {
        EXCELLENT,    // ±5m
        GOOD,         // ±10m
        FAIR,         // ±20m
        POOR,         // ±50m
        VERY_POOR,    // >50m
        OUTDATED,     // Muito antiga
        NO_LOCATION   // Nenhuma localização
    }
}
