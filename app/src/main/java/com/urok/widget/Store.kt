package com.urok.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

// day: 0 = Пн ... 6 = Вс
data class Lesson(val day: Int, val start: Int, val end: Int, val name: String, val room: String)

val INK = 0xFF171012.toInt()
val WINE = 0xFF241519.toInt()
val CARD = 0xFF2D1B20.toInt()
val ROSE = 0xFFF1DEDB.toInt()
val MUTED = 0xFFB39A9B.toInt()
val RED = 0xFFE4605C.toInt()

val DAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

object Store {
    fun fmt(m: Int) = "%02d:%02d".format(m / 60, m % 60)

    fun nowMin(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    fun today(): Int = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7

    fun load(c: Context): MutableList<Lesson> {
        val s = c.getSharedPreferences("urok", 0).getString("lessons2", null) ?: return mutableListOf()
        val a = JSONArray(s)
        return MutableList(a.length()) {
            val o = a.getJSONObject(it)
            Lesson(o.getInt("d"), o.getInt("s"), o.getInt("e"), o.getString("n"), o.getString("r"))
        }.sortedWith(compareBy({ it.day }, { it.start })).toMutableList()
    }

    fun save(c: Context, l: List<Lesson>) {
        val a = JSONArray()
        l.forEach {
            a.put(JSONObject().put("d", it.day).put("s", it.start).put("e", it.end).put("n", it.name).put("r", it.room))
        }
        c.getSharedPreferences("urok", 0).edit().putString("lessons2", a.toString()).apply()
    }
}
