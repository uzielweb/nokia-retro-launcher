package com.uziel.nokiaretrolauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

object SystemBridge {

    const val CURRENT_VERSION_NAME = "1.2.0"
    const val CURRENT_VERSION_CODE = 103
    private const val UPDATE_MANIFEST_URL =
        "https://raw.githubusercontent.com/uzielweb/nokia-retro-launcher/main/version.json"

    fun loadInstalledApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        val selfPackage = context.packageName
        return resolveInfos
            .asSequence()
            .filter { it.activityInfo.packageName != selfPackage }
            .map { info ->
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                    .ifEmpty { info.activityInfo.packageName }
                val comp = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                val iconBitmap = runCatching {
                    drawableToImageBitmap(info.loadIcon(pm), 96)
                }.getOrNull()
                AppEntry(
                    label = label,
                    packageName = info.activityInfo.packageName,
                    componentName = comp,
                    icon = iconBitmap
                )
            }
            .distinctBy { it.componentName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .toList()
    }

    private fun drawableToImageBitmap(drawable: Drawable, sizePx: Int): ImageBitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val scaled = Bitmap.createScaledBitmap(drawable.bitmap, sizePx, sizePx, true)
            return scaled.asImageBitmap()
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }

    fun launchApp(context: Context, app: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = app.componentName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        runCatching { context.startActivity(intent) }
    }

    fun openDialer(context: Context, number: String) {
        val uri = if (number.isNotBlank()) Uri.parse("tel:${Uri.encode(number)}") else null
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun openMessages(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun openCamera(context: Context) {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun openAndroidSettings(context: Context) {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun openDefaultLauncherSettings(context: Context) {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun toggleTorch(context: Context, turnOn: Boolean): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val isBack = chars.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
                hasFlash && isBack
            } ?: cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                cm.setTorchMode(cameraId, turnOn)
                turnOn
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun playKeyFeedback(
        context: Context,
        hapticsEnabled: Boolean,
        soundEnabled: Boolean,
        sound: RetroSound = RetroSound.SELECT
    ) {
        if (hapticsEnabled) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator.vibrate(
                        VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(18L)
                    }
                }
            }
        }
        if (soundEnabled) {
            RetroSoundEngine.play(context, sound)
        }
    }

    fun readDeviceStatus(context: Context, locale: Locale = Locale.getDefault()): DeviceStatus {
        return runCatching {
            val batteryIntent = runCatching {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }.getOrNull()
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val batteryPct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 100

            var wifiConnected = false
            var cellularConnected = false
            runCatching {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(network)
                if (caps != null) {
                    wifiConnected = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    cellularConnected = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                }
            }

            val now = Date()
            val dateFmt = SimpleDateFormat("EEE, dd MMM", locale)
            val timeFmt = SimpleDateFormat("HH:mm", locale)

            DeviceStatus(
                batteryPercent = batteryPct.coerceIn(0, 100),
                isWifiConnected = wifiConnected,
                hasMobileData = cellularConnected || !wifiConnected,
                signalBars = 4,
                dateText = dateFmt.format(now),
                timeText = timeFmt.format(now)
            )
        }.getOrDefault(DeviceStatus())
    }

    private val t9Map = mapOf(
        'a' to '2', 'b' to '2', 'c' to '2',
        'd' to '3', 'e' to '3', 'f' to '3',
        'g' to '4', 'h' to '4', 'i' to '4',
        'j' to '5', 'k' to '5', 'l' to '5',
        'm' to '6', 'n' to '6', 'o' to '6',
        'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
        't' to '8', 'u' to '8', 'v' to '8',
        'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9'
    )

    private fun normalizeAscii(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{M}+"), "").lowercase(Locale.ENGLISH)
    }

    fun matchesT9(appLabel: String, digits: String): Boolean {
        if (digits.isEmpty()) return true
        val clean = normalizeAscii(appLabel)
        val converted = clean.map { ch ->
            t9Map[ch] ?: ch
        }.joinToString("")
        return converted.contains(digits) || clean.contains(digits, ignoreCase = true)
    }

    fun matchesAppQuery(appLabel: String, t9Digits: String, textQuery: String): Boolean {
        val t9Ok = matchesT9(appLabel, t9Digits)
        if (!t9Ok) return false
        if (textQuery.isBlank()) return true
        return normalizeAscii(appLabel).contains(normalizeAscii(textQuery.trim()))
    }

    private fun wallpaperFile(context: Context): File =
        File(context.filesDir, "custom_wallpaper.jpg")

    fun saveCustomWallpaper(context: Context, uri: Uri): ImageBitmap? {
        return runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val maxDim = 720
            val w = raw.width
            val h = raw.height
            val scale = if (w > maxDim || h > maxDim) {
                maxDim.toFloat() / maxOf(w, h).toFloat()
            } else {
                1f
            }
            val finalBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    raw,
                    (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                raw
            }
            FileOutputStream(wallpaperFile(context)).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            finalBitmap.asImageBitmap()
        }.getOrNull()
    }

    fun loadCustomWallpaper(context: Context): ImageBitmap? {
        val file = wallpaperFile(context)
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }

    fun loadShortcutPackages(prefs: SharedPreferences): List<String> {
        val raw = prefs.getString("quick_shortcuts", "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split("|").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    fun toggleShortcutPackage(prefs: SharedPreferences, pkg: String): List<String> {
        val current = loadShortcutPackages(prefs).toMutableList()
        if (current.contains(pkg)) {
            current.remove(pkg)
        } else {
            current.add(pkg)
        }
        prefs.edit().putString("quick_shortcuts", current.joinToString("|")).apply()
        return current
    }

    fun checkForUpdate(): UpdateInfo? {
        return runCatching {
            val url = URL("$UPDATE_MANIFEST_URL?t=${System.currentTimeMillis()}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                useCaches = false
                requestMethod = "GET"
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val remoteVersion = json.optString("version", "").trim()
                val remoteCode = json.optInt("versionCode", 0)
                val apkUrl = json.optString("apk_url", "").trim()
                val releaseUrl = json.optString("release_url", "").trim()
                val targetUrl = apkUrl.ifEmpty { releaseUrl }
                if (remoteVersion.isEmpty() || targetUrl.isEmpty()) return@runCatching null

                val hasNewer = remoteCode > CURRENT_VERSION_CODE ||
                    compareSemver(remoteVersion, CURRENT_VERSION_NAME) > 0
                if (hasNewer) {
                    UpdateInfo(
                        version = remoteVersion,
                        versionCode = remoteCode,
                        apkUrl = apkUrl.ifEmpty { targetUrl },
                        releaseUrl = releaseUrl.ifEmpty { targetUrl }
                    )
                } else {
                    null
                }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    fun compareSemver(v1: String, v2: String): Int {
        val p1 = v1.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val p2 = v2.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(p1.size, p2.size)
        for (i in 0 until maxLen) {
            val a = p1.getOrElse(i) { 0 }
            val b = p2.getOrElse(i) { 0 }
            if (a > b) return 1
            if (a < b) return -1
        }
        return 0
    }

    fun openUrl(context: Context, url: String) {
        if (url.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }
}
