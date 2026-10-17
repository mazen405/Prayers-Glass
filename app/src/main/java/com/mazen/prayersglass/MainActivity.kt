package com.mazen.prayersglass

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color
import android.graphics.Typeface
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(28, 36, 28, 36)
            setBackgroundColor(Color.rgb(16, 26, 38))
        }
        val title = TextView(this).apply {
            text = "Prayers Glass"; textSize = 30f; setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "مواقيت الصلاة بتصميم زجاجي"; textSize = 18f
            setTextColor(Color.rgb(184, 244, 240)); gravity = Gravity.CENTER; setPadding(0, 12, 0, 24)
        }
        status = TextView(this).apply {
            text = "اضغط لتحديد موقعك وجلب المواقيت."; textSize = 15f
            setTextColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(0, 12, 0, 24)
        }
        val button = Button(this).apply { text = "تحديث الموقع والمواقيت"; setOnClickListener { updatePrayerTimes() } }
        root.addView(title); root.addView(subtitle); root.addView(status); root.addView(button)
        setContentView(root)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) updatePrayerTimes()
        else requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 42)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 42 && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) updatePrayerTimes()
        else status.text = "اسمح بالوصول للموقع لحساب المواقيت حسب مكانك."
    }
    private fun updatePrayerTimes() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 42); return
        }
        status.text = "جارٍ تحديد الموقع وجلب المواقيت…"
        executor.execute {
            try {
                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                var loc: Location? = null
                for (provider in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)) {
                    try {
                        if (lm.isProviderEnabled(provider)) {
                            val candidate = lm.getLastKnownLocation(provider)
                            if (candidate != null && (loc == null || candidate.time > loc!!.time)) loc = candidate
                        }
                    } catch (_: SecurityException) {}
                }
                val latitude = loc?.latitude ?: 30.0444
                val longitude = loc?.longitude ?: 31.2357
                val connection = URL("https://api.aladhan.com/v1/timings?latitude=$latitude&longitude=$longitude&method=5").openConnection()
                connection.connectTimeout = 10000; connection.readTimeout = 10000
                val json = JSONObject(connection.getInputStream().bufferedReader().use { it.readText() })
                if (json.optInt("code") != 200) throw Exception("تعذر جلب المواقيت")
                val data = json.getJSONObject("data")
                val timings = data.getJSONObject("timings")
                val keys = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
                val arabic = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
                val prefs = getSharedPreferences("prayers", MODE_PRIVATE)
                prefs.edit().putString("location", if (loc == null) "القاهرة • موقع افتراضي" else "%.3f, %.3f".format(Locale.US, latitude, longitude))
                    .putString("names", arabic.joinToString("|"))
                    .putString("times", keys.joinToString("|") { timings.getString(it).take(5) })
                    .putLong("updated", System.currentTimeMillis()).apply()
                runOnUiThread {
                    status.text = "تم تحديث المواقيت بنجاح.\nأضف الويدجت للشاشة الرئيسية.\nالموقع: " + prefs.getString("location", "")
                    refreshWidget()
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "تعذر تحديث المواقيت. تأكد من الإنترنت وتفعيل الموقع.\n" + (e.message ?: "") }
            }
        }
    }
    private fun refreshWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, PrayerWidgetProvider::class.java))
        if (ids.isNotEmpty()) PrayerWidgetProvider().onUpdate(this, manager, ids)
    }
}
