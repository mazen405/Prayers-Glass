package com.mazen.prayersglass

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar

class PrayerWidgetProvider : AppWidgetProvider() {
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
        val intent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.next_name, intent)
        for (id in ids) manager.updateAppWidget(id, views)
    }
}
