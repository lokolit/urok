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
            val all = Store.load(c)
            val d = Store.today()
            val t = Store.nowMin()
            val l = all.filter { it.day == d }.sortedBy { it.start }
            val cur = l.firstOrNull { it.start <= t && t < it.end }
            val nxt = l.firstOrNull { it.start > t }

            // Если сегодня уроков больше нет, ищем ближайший урок в следующие дни.
            var later: Lesson? = null
            var laterWhen = ""
            if (nxt == null) {
                for (k in 1..7) {
                    val dd = (d + k) % 7
                    val f = all.filter { it.day == dd }.minByOrNull { it.start }
                    if (f != null) {
                        later = f
                        laterWhen = if (k == 1) "завтра" else DAYS[dd]
                        break
                    }
                }
            }
            val room = { x: Lesson -> if (x.room.isBlank()) "" else " · каб. ${x.room}" }

            v.setTextViewText(
                R.id.cur_name,
                cur?.name ?: when {
                    nxt != null -> "Перемена"
                    l.isNotEmpty() -> "Уроки закончились"
                    else -> "Сегодня уроков нет"
                }
            )
            v.setTextViewText(R.id.cur_time, cur?.let { "до ${Store.fmt(it.end)}${room(it)}" } ?: "")
            v.setTextViewText(R.id.nxt_name, nxt?.name ?: later?.name ?: "Добавьте уроки в приложении")
            v.setTextViewText(
                R.id.nxt_time,
                nxt?.let { "в ${Store.fmt(it.start)}${room(it)}" }
                    ?: later?.let { "$laterWhen в ${Store.fmt(it.start)}${room(it)}" } ?: ""
            )

            val open = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.root, open)
            return v
        }

        // Будим виджет в момент начала или конца ближайшего урока и в полночь (смена дня).
        private fun schedule(c: Context) {
            val t = Store.nowMin()
            val d = Store.today()
            val points = Store.load(c).filter { it.day == d }
                .flatMap { listOf(it.start, it.end) }.filter { it > t } + 1440
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
