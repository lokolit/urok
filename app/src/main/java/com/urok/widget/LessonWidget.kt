package com.urok.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar

class LessonWidget : AppWidgetProvider() {

    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        ids.forEach { m.updateAppWidget(it, build(c)) }
        schedule(c)
    }

    companion object {
        fun refresh(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            m.getAppWidgetIds(ComponentName(c, LessonWidget::class.java))
                .forEach { m.updateAppWidget(it, build(c)) }
            schedule(c)
        }

        private fun build(c: Context): RemoteViews {
            val v = RemoteViews(c.packageName, R.layout.widget)
            val l = Store.load(c)
            val t = Store.nowMin()
            val cur = l.firstOrNull { it.start <= t && t < it.end }
            val nxt = l.firstOrNull { it.start > t }
            val room = { x: Lesson -> if (x.room.isBlank()) "" else " · каб. ${x.room}" }

            v.setTextViewText(R.id.cur_name, cur?.name ?: if (nxt == null) "Уроков нет" else "Перемена")
            v.setTextViewText(R.id.cur_time, cur?.let { "до ${Store.fmt(it.end)}${room(it)}" } ?: "")
            v.setTextViewText(R.id.nxt_name, nxt?.name ?: "На сегодня всё")
            v.setTextViewText(R.id.nxt_time, nxt?.let { "в ${Store.fmt(it.start)}${room(it)}" } ?: "")

            val open = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.root, open)
            return v
        }

        // Будим виджет ровно в момент начала или конца ближайшего урока.
        private fun schedule(c: Context) {
            val t = Store.nowMin()
            val points = Store.load(c).flatMap { listOf(it.start, it.end) }.filter { it > t } + 1440
            val at = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5); set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, points.min())
            }.timeInMillis

            val ids = AppWidgetManager.getInstance(c)
                .getAppWidgetIds(ComponentName(c, LessonWidget::class.java))
            val i = Intent(c, LessonWidget::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            val pi = PendingIntent.getBroadcast(
                c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                .setAndAllowWhileIdle(AlarmManager.RTC, at, pi)
        }
    }
}
