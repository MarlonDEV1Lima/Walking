package com.msystem.walking.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import com.msystem.walking.R;

public class PermissionUtils {

    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
        void onPermissionExplained();
    }

    public static void showLocationPermissionDialog(Context context, PermissionCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle("Localização Necessária")
                .setMessage("Este app precisa de acesso à localização para:\n\n" +
                          "• Rastrear suas caminhadas\n" +
                          "• Mostrar seu trajeto no mapa\n" +
                          "• Calcular distância e pontos\n\n" +
                          "Sua privacidade é protegida!")
                .setPositiveButton("Permitir", (dialog, which) -> {
                    dialog.dismiss();
                    if (callback != null) callback.onPermissionExplained();
                })
                .setNegativeButton("Agora Não", (dialog, which) -> {
                    dialog.dismiss();
                    if (callback != null) callback.onPermissionDenied();
                })
                .setCancelable(false)
                .show();
    }

    public static void showPermissionDeniedDialog(Context context, PermissionCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle("Permissão Negada")
                .setMessage("Sem acesso à localização, o WalkKing não consegue:\n\n" +
                          "• Rastrear suas atividades\n" +
                          "• Mostrar o mapa\n" +
                          "• Conquistar territórios\n\n" +
                          "Para ativar, vá em Configurações > Apps > WalkKing > Permissões > Localização")
                .setPositiveButton("Abrir Configurações", (dialog, which) -> {
                    openAppSettings(context);
                    dialog.dismiss();
                })
                .setNegativeButton("Continuar Sem GPS", (dialog, which) -> {
                    dialog.dismiss();
                    if (callback != null) callback.onPermissionDenied();
                })
                .show();
    }

    public static void showBackgroundLocationDialog(Context context, PermissionCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle("Rastreamento Contínuo")
                .setMessage("Para funcionar melhor, o WalkKing pode rastrear sua localização mesmo quando o app não estiver aberto.\n\n" +
                          "• Isso permite rastrear atividades completas\n" +
                          "• Não consome bateria extra\n" +
                          "• Você pode desativar a qualquer momento\n\n" +
                          "Recomendamos escolher 'Permitir sempre'")
                .setPositiveButton("Entendi", (dialog, which) -> {
                    dialog.dismiss();
                    if (callback != null) callback.onPermissionExplained();
                })
                .setNegativeButton("Pular", (dialog, which) -> {
                    dialog.dismiss();
                    if (callback != null) callback.onPermissionDenied();
                })
                .show();
    }

    public static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    public static void showLocationServicesDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("GPS Desativado")
                .setMessage("O GPS do seu dispositivo está desativado.\n\n" +
                          "Para usar o WalkKing, ative o GPS em:\n" +
                          "Configurações > Localização > Ativar")
                .setPositiveButton("Abrir Configurações", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(intent);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
