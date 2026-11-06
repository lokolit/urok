package com.urok.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class Lesson(val start: Int, val end: Int, val name: String, val room: String)

val INK = 0xFF171012.toInt()
val WINE = 0xFF241519.toInt()
val CARD = 0xFF2D1B20.toInt()
val ROSE = 0xFFF1DEDB.toInt()
val MUTED = 0xFFB39A9B.toInt()
val RED = 0xFFE4605C.toInt()

object Store {
    fun fmt(m: Int) = "%02d:%02d".format(m / 60, m % 60)

    fun nowMin(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    private fun defaults() = mutableListOf(
        Lesson(510, 555, "Математика", "214"),
        Lesson(565, 610, "Русский язык", "107"),
        Lesson(630, 675, "Физика", "305"),
        Lesson(685, 730, "История", "112"),
        Lesson(750, 795, "Английский", "208")
    )

    fun load(c: Context): MutableList<Lesson> {
        val s = c.getSharedPreferences("urok", 0).getString("lessons", null) ?: return defaults()
        val a = JSONArray(s)
        return MutableList(a.length()) {
            val o = a.getJSONObject(it)
            Lesson(o.getInt("s"), o.getInt("e"), o.getString("n"), o.getString("r"))
        }.sortedBy { it.start }.toMutableList()
    }

    fun save(c: Context, l: List<Lesson>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("s", it.start).put("e", it.end).put("n", it.name).put("r", it.room)) }
        c.getSharedPreferences("urok", 0).edit().putString("lessons", a.toString()).apply()
    }
}
