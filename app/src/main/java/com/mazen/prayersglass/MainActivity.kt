package com.mazen.prayersglass

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var root: LinearLayout
    private var selectedStyle = "glass"
    private val bg = Color.rgb(8, 12, 22)
    private val muted = Color.rgb(174, 184, 205)
    private val aqua = Color.rgb(177, 244, 239)
    private val blue = Color.rgb(48, 102, 246)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedStyle = getSharedPreferences("prayers", MODE_PRIVATE).getString("widget_style", "glass") ?: "glass"
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(26), dp(22), dp(26))
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(root) }
        setContentView(scroll)

        addText("Prayers Glass", 30f, Color.WHITE, true, Gravity.START)
        addText("اختَر شكل ويدجت الصلاة", 21f, Color.WHITE, true, Gravity.START, 8)
        addText("معاينة واختيار من داخل التطبيق", 14f, muted, false, Gravity.START, 4)
        addSpace(22)

        addStyleCard("glass", "Liquid Glass", "زجاج شفاف ناعم", true)
        addSpace(12)
        addStyleCard("compact", "الصلاة القادمة", "ويدجت مختصر وواضح", false)
        addSpace(12)
        addStyleCard("strip", "مواقيت الصلاة", "شريط يعرض المواقيت كلها", false)

        addSpace(22)
        val add = Button(this).apply {
            text = "إضافة الويدجت للشاشة الرئيسية"
            textSize = 16f
            isAllCaps = false
            setOnClickListener { pinWidget() }
        }
        root.addView(add, matchWrap())
        addSpace(10)
        val refresh = Button(this).apply {
            text = "تحديد الموقع وتحديث المواقيت"
            isAllCaps = false
            setOnClickListener { requestLocationAndUpdate() }
        }
        root.addView(refresh, matchWrap())
        status = TextView(this).apply {
            text = "اختَر التصميم ثم اضغط إضافة الويدجت."
            textSize = 13f; setTextColor(muted); gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(8))
        }
        root.addView(status, matchWrap())
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) updatePrayerTimes()
        else requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 42)
    }

    private fun addStyleCard(style: String, title: String, subtitle: String, glass: Boolean) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = rounded(if (selectedStyle == style) 0xFF23394A.toInt() else 0xFF171C28.toInt(),
                if (selectedStyle == style) aqua else 0xFF515866.toInt())
            isClickable = true
            isFocusable = true
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val name = TextView(this).apply { text = title; textSize = 17f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD }
        val tick = TextView(this).apply { text = if (selectedStyle == style) "✓ محدد" else "اختيار"; textSize = 13f; setTextColor(if (selectedStyle == style) aqua else muted) }
        top.addView(name, LinearLayout.LayoutParams(0, dp(28), 1f)); top.addView(tick)
        card.addView(top)
        card.addView(TextView(this).apply { text = subtitle; textSize = 12f; setTextColor(muted); setPadding(0, 0, 0, dp(12)) })
        val preview = LinearLayout(this).apply {
            orientation = if (style == "strip") LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = rounded(if (glass) 0xAA536875.toInt() else 0xFF252A36.toInt(), 0xFF929BA8.toInt())
            minimumHeight = dp(if (style == "strip") 68 else 92)
        }
        if (style == "strip") {
            listOf("الفجر\n05:32", "الظهر\n12:40", "العصر\n15:55", "المغرب\n18:21", "العشاء\n19:38").forEach { label ->
                preview.addView(TextView(this).apply { text = label; textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.WHITE) },
                    LinearLayout.LayoutParams(0, dp(42), 1f))
            }
        } else {
            preview.addView(TextView(this).apply {
                text = if (style == "glass") "الصلاة القادمة                         30.118, 31.318\nالفجر\nمتبقي 4 ساعات و 6 دقائق" else "الفجر  •  الصلاة القادمة\nمتبقي 4 ساعات و 6 دقائق"
                textSize = if (style == "glass") 15f else 17f
                typeface = Typeface.DEFAULT_BOLD; setTextColor(if (glass) Color.WHITE else aqua)
            })
            if (style == "glass") preview.addView(TextView(this).apply {
                text = "الفجر 05:32  •  الظهر 12:40  •  العصر 15:55"
                textSize = 10f; setTextColor(Color.WHITE); setPadding(0, dp(8), 0, 0)
            })
        }
        card.addView(preview)
        card.setOnClickListener {
            selectedStyle = style
            getSharedPreferences("prayers", MODE_PRIVATE).edit().putString("widget_style", style).apply()
            root.removeAllViews()
            rebuildContent()
        }
        root.addView(card, matchWrap())
    }

    private fun rebuildContent() {
        root.removeAllViews()
        addText("Prayers Glass", 30f, Color.WHITE, true, Gravity.START)
        addText("اختَر شكل ويدجت الصلاة", 21f, Color.WHITE, true, Gravity.START, 8)
        addText("معاينة واختيار من داخل التطبيق", 14f, muted, false, Gravity.START, 4)
        addSpace(22)
        addStyleCard("glass", "Liquid Glass", "زجاج شفاف ناعم", true)
        addSpace(12); addStyleCard("compact", "الصلاة القادمة", "ويدجت مختصر وواضح", false)
        addSpace(12); addStyleCard("strip", "مواقيت الصلاة", "شريط يعرض المواقيت كلها", false)
        addSpace(22)
        root.addView(Button(this).apply { text = "إضافة الويدجت للشاشة الرئيسية"; isAllCaps = false; setOnClickListener { pinWidget() } }, matchWrap())
        addSpace(8)
        root.addView(Button(this).apply { text = "تحديد الموقع وتحديث المواقيت"; isAllCaps = false; setOnClickListener { requestLocationAndUpdate() } }, matchWrap())
        root.addView(status, matchWrap())
    }

    private fun pinWidget() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = AppWidgetManager.getInstance(this)
            val provider = ComponentName(this, PrayerWidgetProvider::class.java)
            if (manager.isRequestPinAppWidgetSupported) {
                val ok = manager.requestPinAppWidget(provider, null, null)
                status.text = if (ok) "تم إرسال طلب الإضافة. أكّد الإضافة من الشاشة الرئيسية." else "اللانشر لم يقبل طلب الإضافة؛ أضفه من قائمة الويدجت."
            } else status.text = "اللانشر لا يدعم الإضافة المباشرة. اضغط مطولًا على الشاشة الرئيسية وافتح الويدجت."
        } else status.text = "اضغط مطولًا على الشاشة الرئيسية وافتح قائمة الويدجت."
    }

    private fun requestLocationAndUpdate() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 42)
        } else updatePrayerTimes()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 42 && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) updatePrayerTimes()
        else if (::status.isInitialized) status.text = "اسمح بالوصول للموقع لحساب المواقيت حسب مكانك."
    }

    private fun updatePrayerTimes() {
        if (::status.isInitialized) status.text = "جارٍ تحديد الموقع وجلب المواقيت…"
        executor.execute {
            try {
                val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                var loc: android.location.Location? = null
                for (provider in listOf(android.location.LocationManager.NETWORK_PROVIDER, android.location.LocationManager.GPS_PROVIDER)) {
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
                    if (::status.isInitialized) status.text = "تم تحديث المواقيت • " + prefs.getString("location", "")
                    refreshWidget()
                }
            } catch (e: Exception) {
                runOnUiThread { if (::status.isInitialized) status.text = "تعذر تحديث المواقيت. تأكد من الإنترنت والموقع." }
            }
        }
    }

    private fun refreshWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, PrayerWidgetProvider::class.java))
        if (ids.isNotEmpty()) PrayerWidgetProvider().onUpdate(this, manager, ids)
    }
    private fun addText(value: String, size: Float, color: Int, bold: Boolean, gravity: Int, top: Int = 0) {
        root.addView(TextView(this).apply {
            text = value; textSize = size; setTextColor(color); this.gravity = gravity
            if (bold) typeface = Typeface.DEFAULT_BOLD
            if (top > 0) setPadding(0, dp(top), 0, 0)
        }, matchWrap())
    }
    private fun addSpace(value: Int) { root.addView(View(this), LinearLayout.LayoutParams(1, dp(value))) }
    private fun matchWrap() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(fill: Int, stroke: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(24).toFloat(); setStroke(dp(1), stroke)
    }
}
