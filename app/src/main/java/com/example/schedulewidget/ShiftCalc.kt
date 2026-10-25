package com.example.schedulewidget

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object ShiftCalc {
    fun pad(n: Int) = n.toString().padStart(2, '0')
    fun todayStr(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.YEAR)}-${pad(c.get(Calendar.MONTH) + 1)}-${pad(c.get(Calendar.DAY_OF_MONTH))}"
    }

    fun calcShift(data: JSONObject, person: JSONObject, date: Date): String? {
        val typeId = person.optString("shiftTypeId")
        val types = data.optJSONArray("shiftTypes") ?: return null
        var type: JSONObject? = null
        for (i in 0 until types.length()) { val t = types.getJSONObject(i); if (t.optString("id") == typeId) { type = t; break } }
        val cycle = type?.optJSONArray("cycle") ?: return null
        if (cycle.length() == 0) return null
        val baseDateStr = person.optString("baseDate", todayStr())
        val baseDate = try { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(baseDateStr)!! } catch (e: Exception) { return null }
        
        val a = Calendar.getInstance().apply { time = baseDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val b = Calendar.getInstance().apply { time = date; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val diff = ((b.timeInMillis - a.timeInMillis) / 86400000L).toInt()
        
        val baseIdx = person.optInt("baseIndex", 0)
        val len = cycle.length()
        val idx = ((baseIdx + diff) % len + len) % len
        val n = cycle.get(idx)
        return if (n is JSONObject) n.optString("name") else n.toString()
    }

    fun getMe(data: JSONObject): JSONObject? {
        val meId = data.optString("meId")
        val list = data.optJSONArray("personnel") ?: return null
        for (i in 0 until list.length()) { val p = list.getJSONObject(i); if (p.optString("id") == meId) return p }
        return if (list.length() > 0) list.getJSONObject(0) else null
    }

    fun shiftShort(name: String?): String {
        if (name.isNullOrEmpty()) return "—"
        if (name == "休息" || name == "休班") return "休"
        return name.substring(0, 1)
    }

    fun isToday(date: Date): Boolean {
        val a = Calendar.getInstance().apply { time = date }; val b = Calendar.getInstance()
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    fun mondayOf(date: Date): Date {
        val c = Calendar.getInstance().apply { time = date }
        val dow = c.get(Calendar.DAY_OF_WEEK)
        val offset = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
        c.add(Calendar.DAY_OF_MONTH, offset)
        return c.time
    }

    fun addDays(date: Date, days: Int): Date {
        val c = Calendar.getInstance().apply { time = date }; c.add(Calendar.DAY_OF_MONTH, days); return c.time
    }
}
