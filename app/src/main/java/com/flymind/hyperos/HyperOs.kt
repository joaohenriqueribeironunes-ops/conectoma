package com.flymind.hyperos

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Atalhos de configuração do HyperOS 1 (Redmi Note 11S).
 *
 * O app não exige serviço em segundo plano nem permissões especiais; estes
 * atalhos ajudam a liberar o processamento do circuito quando o aparelho
 * aplica restrições de bateria típicas da MIUI/HyperOS.
 */
object HyperOs {

    fun isXiaomi(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
            manufacturer.contains("redmi") || brand.contains("redmi") ||
            manufacturer.contains("poco") || brand.contains("poco")
    }

    fun deviceLabel(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return "$manufacturer $model"
    }

    fun androidLabel(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    fun hyperOsLabel(): String = when {
        !isXiaomi() -> "Fabricante não-Xiaomi"
        Build.DISPLAY.contains("V816", true) ||
            Build.DISPLAY.contains("HyperOS", true) ||
            Build.DISPLAY.contains("OS1.", true) ||
            Build.VERSION.INCREMENTAL.contains("V816", true) ||
            Build.VERSION.INCREMENTAL.contains("OS1.", true) -> "HyperOS detectado"
        else -> "MIUI/HyperOS"
    }

    fun openAutostart(context: Context): Boolean {
        val intent = Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
        )
        return start(context, intent) || openAppSettings(context)
    }

    fun openBatterySettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        return start(context, intent) || openAppSettings(context)
    }

    fun openPowerKeeper(context: Context): Boolean {
        val intent = Intent().setComponent(
            ComponentName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity",
            ),
        ).putExtra("package_name", context.packageName)
            .putExtra("package_label", "Conectoma")
        return start(context, intent) || openBatterySettings(context)
    }

    fun openAppSettings(context: Context): Boolean {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
        return start(context, intent)
    }

    private fun start(context: Context, intent: Intent): Boolean {
        return runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
