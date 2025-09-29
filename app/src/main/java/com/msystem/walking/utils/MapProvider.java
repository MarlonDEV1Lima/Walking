package com.msystem.walking.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class MapProvider {
    private static final String PREFS_NAME = "WalkingMapPrefs";
    private static final String MAP_PROVIDER_KEY = "map_provider";

    public enum MapType {
        GOOGLE_MAPS,    // Gratuito até 28.500 usos/mês
        OPENSTREETMAP   // 100% gratuito sem limites
    }

    private static MapProvider instance;
    private Context context;
    private MapType currentMapType;

    private MapProvider(Context context) {
        this.context = context.getApplicationContext();
        loadMapPreference();
    }

    public static MapProvider getInstance(Context context) {
        if (instance == null) {
            instance = new MapProvider(context);
        }
        return instance;
    }

    private void loadMapPreference() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String mapType = prefs.getString(MAP_PROVIDER_KEY, MapType.OPENSTREETMAP.name()); // Padrão: OpenStreetMap (gratuito)
        currentMapType = MapType.valueOf(mapType);
    }

    public void setMapProvider(MapType mapType) {
        this.currentMapType = mapType;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(MAP_PROVIDER_KEY, mapType.name()).apply();
    }

    public MapType getCurrentMapType() {
        return currentMapType;
    }

    public boolean isGoogleMapsAvailable() {
        // Verifica se a API key do Google Maps está configurada
        try {
            android.content.pm.ApplicationInfo appInfo = context.getPackageManager()
                .getApplicationInfo(context.getPackageName(), android.content.pm.PackageManager.GET_META_DATA);

            if (appInfo.metaData != null) {
                String apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY");
                return apiKey != null && !apiKey.contains("SUBSTITUA") && !apiKey.isEmpty();
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public String getMapProviderName() {
        switch (currentMapType) {
            case GOOGLE_MAPS:
                return "Google Maps (Gratuito: 28.5k/mês)";
            case OPENSTREETMAP:
                return "OpenStreetMap (100% Gratuito)";
            default:
                return "OpenStreetMap";
        }
    }
}
