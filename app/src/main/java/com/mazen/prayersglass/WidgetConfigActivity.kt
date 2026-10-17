package com.mazen.prayersglass

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity

class WidgetConfigActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 36, 28, 36)
            setBackgroundColor(Color.rgb(16, 26, 38))
        }
        root.addView(TextView(this).apply {
            text = "جهّز زجاج Prayers Glass"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "اختَر صورة خلفية الشاشة الحالية، وسنستخرج ألوانها لضبط مظهر الودجت."
            textSize = 16f
            setTextColor(Color.rgb(215, 232, 240))
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 24)
        })
        root.addView(Button(this).apply {
            text = "اختيار الخلفية من المعرض"
            setOnClickListener { chooseWallpaper() }
        })
        setContentView(root)
    }

    private fun chooseWallpaper() {
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(picker, PICK_IMAGE)
    }

    @Deprecated("Uses the activity result contract required by this minimal project")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_IMAGE || resultCode != RESULT_OK || data?.data == null) {
            if (requestCode == PICK_IMAGE) finish()
            return
        }
        val uri = data.data!!
        try {
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            contentResolver.takePersistableUriPermission(uri, flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val color = sampleDominantColor(uri)
            getSharedPreferences("prayers", MODE_PRIVATE).edit()
                .putString("wallpaper_uri", uri.toString())
                .putInt("wallpaper_color", color)
                .apply()
            refreshWidgets()
            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, result)
        } catch (_: Exception) {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    private fun sampleDominantColor(uri: Uri): Int {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = BitmapFactory.Options().apply { inSampleSize = maxOf(1, minOf(bounds.outWidth, bounds.outHeight) / 100) }
        val bitmap = contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, sample) }
            ?: return Color.rgb(50, 78, 96)
        var red = 0L; var green = 0L; var blue = 0L; var count = 0L
        val step = maxOf(1, minOf(bitmap.width, bitmap.height) / 40)
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val pixel = bitmap.getPixel(x, y)
                red += Color.red(pixel); green += Color.green(pixel); blue += Color.blue(pixel); count++
            }
        }
        bitmap.recycle()
        if (count == 0L) return Color.rgb(50, 78, 96)
        return Color.rgb((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
    }

    private fun refreshWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, PrayerWidgetProvider::class.java))
        if (ids.isNotEmpty()) PrayerWidgetProvider().onUpdate(this, manager, ids)
    }

    companion object { private const val PICK_IMAGE = 1001 }
}
