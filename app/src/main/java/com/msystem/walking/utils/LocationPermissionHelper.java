package com.msystem.walking.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class LocationPermissionHelper {

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasBackgroundLocationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Versões anteriores ao Android 10 não precisam dessa permissão
    }

    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    public static boolean shouldShowRequestPermissionRationale(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static boolean isGPSEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static boolean isNetworkLocationEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public static boolean isHighAccuracyModeEnabled(Context context) {
        try {
            int locationMode = Settings.Secure.getInt(
                context.getContentResolver(),
                Settings.Secure.LOCATION_MODE
            );
            return locationMode == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    public static boolean isAnyLocationProviderEnabled(Context context) {
        return isGPSEnabled(context) || isNetworkLocationEnabled(context);
    }

    /**
     * Verifica se todas as condições para localização de alta precisão estão atendidas
     */
    public static boolean isOptimalLocationSetup(Context context) {
        return hasLocationPermission(context) &&
               isGPSEnabled(context) &&
               isHighAccuracyModeEnabled(context);
    }

    /**
     * Retorna uma mensagem explicando qual configuração está faltando
     */
    public static String getLocationSetupMessage(Context context) {
        if (!hasLocationPermission(context)) {
            return "Permissão de localização não concedida. Vá em Configurações > Permissões e ative a localização para este app.";
        }

        if (!isGPSEnabled(context)) {
            return "GPS desabilitado. Ative o GPS nas configurações do dispositivo.";
        }

        if (!isHighAccuracyModeEnabled(context)) {
            return "Modo de alta precisão desabilitado. Vá em Configurações > Localização > Modo e selecione 'Alta precisão'.";
        }

        return "Configuração de localização está otimizada.";
    }
}
