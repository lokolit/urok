package com.urok.widget

import android.app.Activity
import android.app.TimePickerDialog
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var lessons: MutableList<Lesson>
    private lateinit var listBox: LinearLayout
    private val dayViews = mutableListOf<TextView>()
    private var day = Store.today()
    private var start = 510
    private var end = 555

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, r: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(r).toFloat() }

    private fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun button(t: String, bg: Int, fg: Int, click: (TextView) -> Unit) = text(t, 15f, fg, true).apply {
        gravity = Gravity.CENTER; background = shape(bg, 16); setPadding(dp(12), dp(14), dp(12), dp(14))
        setOnClickListener { click(this) }
    }

    private fun field(hint: String) = EditText(this).apply {
        this.hint = hint; setHintTextColor(MUTED); setTextColor(ROSE); textSize = 15f
        background = shape(CARD, 14); setPadding(dp(14), dp(12), dp(14), dp(12)); setSingleLine()
    }

    private fun LinearLayout.put(v: View, top: Int = 0, weight: Float = 0f) {
        val w = if (weight > 0) 0 else LinearLayout.LayoutParams.MATCH_PARENT
        addView(v, LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply { topMargin = dp(top) })
    }

    private fun styleDays() {
        dayViews.forEachIndexed { i, v ->
            v.background = shape(if (i == day) RED else CARD, 12)
            v.setTextColor(if (i == day) 0xFF2A0F10.toInt() else ROSE)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = INK
        window.navigationBarColor = INK
        lessons = Store.load(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(INK)
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }
        root.put(text("Урок", 28f, ROSE, true))
        root.put(text("Расписание по дням недели. Виджет покажет текущий и следующий урок.", 14f, MUTED), 4)

        val daysRow = LinearLayout(this)
        DAYS.forEachIndexed { i, n ->
            val tv = text(n, 14f, ROSE, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, dp(10))
                setOnClickListener { day = i; styleDays(); render() }
            }
            dayViews.add(tv)
            daysRow.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (i > 0) marginStart = dp(4)
            })
        }
        root.put(daysRow, 16)
        styleDays()

        listBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = shape(WINE, 22)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        root.put(listBox, 12)

        val name = field("Предмет, например Физика")
        val room = field("Кабинет (необязательно)")
        root.put(name, 16)
        root.put(room, 8)

        val times = LinearLayout(this)
        val bs = button("Начало ${Store.fmt(start)}", CARD, ROSE) { btn ->
            TimePickerDialog(this, R.style.AppDialog, { _, h, m -> start = h * 60 + m; btn.text = "Начало ${Store.fmt(start)}" }, start / 60, start % 60, true).show()
        }
        val be = button("Конец ${Store.fmt(end)}", CARD, ROSE) { btn ->
            TimePickerDialog(this, R.style.AppDialog, { _, h, m -> end = h * 60 + m; btn.text = "Конец ${Store.fmt(end)}" }, end / 60, end % 60, true).show()
        }
        times.put(bs, 0, 1f)
        times.addView(View(this), dp(8), 1)
        times.put(be, 0, 1f)
        root.put(times, 8)

        root.put(button("Добавить урок", RED, 0xFF2A0F10.toInt()) {
            val n = name.text.toString().trim()
            if (n.isEmpty() || end <= start) {
                Toast.makeText(this, "Введите предмет и проверьте время", Toast.LENGTH_SHORT).show()
            } else {
                lessons.add(Lesson(day, start, end, n, room.text.toString().trim()))
                lessons.sortWith(compareBy({ it.day }, { it.start }))
                name.text.clear(); room.text.clear()
                commit()
            }
        }, 12)

        setContentView(ScrollView(this).apply { setBackgroundColor(INK); addView(root) })
        render()
    }

    private fun commit() {
        Store.save(this, lessons)
        LessonWidget.refresh(this)
        render()
    }

    private fun render() {
        listBox.removeAllViews()
        val shown = lessons.filter { it.day == day }.sortedBy { it.start }
        if (shown.isEmpty()) listBox.put(text("В этот день уроков нет. Добавьте первый ниже.", 14f, MUTED).apply {
            setPadding(dp(8), dp(10), dp(8), dp(10))
        })
        val t = Store.nowMin()
        val isToday = day == Store.today()
        shown.forEach { l ->
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(10), dp(4), dp(10))
                if (isToday && l.start <= t && t < l.end) background = shape(0x33E4605C, 14)
            }
            row.addView(text("${Store.fmt(l.start)}\n${Store.fmt(l.end)}", 13f, 0xFFF08A85.toInt(), true).apply {
                minWidth = dp(60)
            })
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            col.put(text(l.name, 16f, ROSE, true))
            if (l.room.isNotBlank()) col.put(text("каб. ${l.room}", 12f, MUTED))
            row.put(col, 0, 1f)
            row.addView(text("×", 24f, MUTED).apply {
                setPadding(dp(14), 0, dp(10), 0)
                setOnClickListener { lessons.remove(l); commit() }
            })
            listBox.put(row)
        }
    }
}
