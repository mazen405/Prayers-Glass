package com.mazen.prayersglass

import android.app.AppWidgetManager
import android.app.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.widget.RemoteViews
import java.util.Calendar
import kotlin.math.max

class PrayerWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_WALLPAPER_CHANGED) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))
            if (ids.isNotEmpty()) onUpdate(context, manager, ids)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val prefs = context.getSharedPreferences("prayers", Context.MODE_PRIVATE)
        val names = (prefs.getString("names", "الفجر|الظهر|العصر|المغرب|العشاء") ?: "").split("|")
        val times = (prefs.getString("times", "") ?: "").split("|")
        val location = prefs.getString("location", "افتح التطبيق لتحديد موقعك") ?: "افتح التطبيق لتحديد موقعك"
        val now = Calendar.getInstance()
        var nextIndex = -1
        var nextMillis = Long.MAX_VALUE
        for (i in times.indices) {
            if (i >= names.size || times[i].length < 5) continue
            val parts = times[i].split(":")
            val candidate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.timeInMillis <= now.timeInMillis) candidate.add(Calendar.DAY_OF_YEAR, 1)
            if (candidate.timeInMillis < nextMillis) { nextMillis = candidate.timeInMillis; nextIndex = i }
        }
        val views = RemoteViews(context.packageName, R.layout.prayer_widget)
        views.setTextViewText(R.id.location, location)

        // A real wallpaper image layer behind translucent glass gives the widget visible depth.
        val wallpaperUri = prefs.getString("wallpaper_uri", null)
        val backdrop = wallpaperUri?.let { makeBlurredBackdrop(context, Uri.parse(it)) }
        if (backdrop != null) {
            views.setImageViewBitmap(R.id.wallpaper_blur, backdrop)
            views.setViewVisibility(R.id.wallpaper_blur, android.view.View.VISIBLE)
        } else {
            views.setImageViewBitmap(R.id.wallpaper_blur, makeFallbackBackdrop())
        }

        if (nextIndex >= 0) {
            val left = (nextMillis - now.timeInMillis) / 60000
            views.setTextViewText(R.id.next_label, "الصلاة القادمة")
            views.setTextViewText(R.id.next_name, names[nextIndex])
            views.setTextViewText(R.id.countdown, "متبقي ${left / 60} ساعة و ${left % 60} دقيقة")
            views.setTextViewText(R.id.times, names.indices.filter { it < times.size }.joinToString("   •   ") { "${names[it]} ${times[it]}" })
        } else {
            views.setTextViewText(R.id.next_name, "افتح التطبيق لتحديث المواقيت")
            views.setTextViewText(R.id.countdown, "المواقيت غير متاحة")
        }
        val intent = PendingIntentCompat.activity(context)
        views.setOnClickPendingIntent(R.id.next_name, intent)
        for (id in ids) manager.updateAppWidget(id, views)
    }

    private fun makeBlurredBackdrop(context: Context, uri: Uri): Bitmap? = try {
        val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
        val source = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val small = Bitmap.createScaledBitmap(source, max(1, source.width / 3), max(1, source.height / 3), true)
        if (source !== small) source.recycle()
        boxBlur(small, 5).also { if (it !== small) small.recycle() }
    } catch (_: Exception) { null }

    private fun boxBlur(input: Bitmap, radius: Int): Bitmap {
        val w = input.width; val h = input.height
        val pixels = IntArray(w * h)
        input.getPixels(pixels, 0, w, 0, 0, w, h)
        val temp = pixels.copyOf()
        for (y in 0 until h) for (x in 0 until w) {
            var r=0; var g=0; var b=0; var n=0
            for (dx in -radius..radius) { val xx=(x+dx).coerceIn(0,w-1); val c=temp[y*w+xx]; r+=Color.red(c); g+=Color.green(c); b+=Color.blue(c); n++ }
            pixels[y*w+x]=Color.rgb(r/n,g/n,b/n)
        }
        val horizontal = pixels.copyOf()
        for (y in 0 until h) for (x in 0 until w) {
            var r=0; var g=0; var b=0; var n=0
            for (dy in -radius..radius) { val yy=(y+dy).coerceIn(0,h-1); val c=horizontal[yy*w+x]; r+=Color.red(c); g+=Color.green(c); b+=Color.blue(c); n++ }
            pixels[y*w+x]=Color.rgb(r/n,g/n,b/n)
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun makeFallbackBackdrop(): Bitmap {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(43, 72, 93)
        canvas.drawRect(Rect(0,0,32,32), paint)
        return bitmap
    }
}
