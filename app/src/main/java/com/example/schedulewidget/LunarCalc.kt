package com.example.schedulewidget

import java.util.Calendar
import java.util.Date

object LunarCalc {
    private val lunarInfo = longArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04bd7, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0
    )
    private const val MIN_YEAR = 1900
    private const val MAX_YEAR = 2080

    private fun lYearDays(y: Int): Int {
        if (y < MIN_YEAR || y > MAX_YEAR) return 365
        var sum = 348; var i = 0x8000
        while (i > 0x8) { sum += if ((lunarInfo[y - MIN_YEAR] and i.toLong()) != 0L) 1 else 0; i = i shr 1 }
        return sum + leapDays(y)
    }
    private fun leapMonth(y: Int): Int = if (y < MIN_YEAR || y > MAX_YEAR) 0 else (lunarInfo[y - MIN_YEAR] and 0xf).toInt()
    private fun leapDays(y: Int): Int = if (leapMonth(y) == 0) 0 else if ((lunarInfo[y - MIN_YEAR] and 0x10000) != 0L) 30 else 29
    private fun monthDays(y: Int, m: Int): Int = if (y < MIN_YEAR || y > MAX_YEAR) 30 else if ((lunarInfo[y - MIN_YEAR] and (0x10000L shr m)) != 0L) 30 else 29

    data class LunarDate(val month: Int, val day: Int, val isLeap: Boolean)

    fun solarToLunar(date: Date): LunarDate {
        val c = Calendar.getInstance().apply { time = date; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val utc = Calendar.getInstance().apply { time = c.time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val base = Calendar.getInstance().apply { set(1900, 0, 31, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
        var offset = ((utc.timeInMillis - base.timeInMillis) / 86400000L).toInt()
        if (offset < 0) return LunarDate(1, 1, false)
        var i = 1900; var temp = 0
        while (i < MAX_YEAR && offset > 0) { temp = lYearDays(i); offset -= temp; i++ }
        if (offset < 0) { offset += temp; i-- }
        val ly = i; val leap = leapMonth(ly); var isLeap = false; var m = 1
        while (m < 13 && offset > 0) {
            if (leap > 0 && m == leap + 1 && !isLeap) { m--; isLeap = true; temp = leapDays(ly) } else { temp = monthDays(ly, m) }
            if (isLeap && m == leap + 1) isLeap = false
            offset -= temp; m++
        }
        if (offset == 0 && leap > 0 && m == leap + 1) { if (isLeap) isLeap = false else { isLeap = true; m-- } }
        if (offset < 0) { offset += temp; m-- }
        return LunarDate(m, offset + 1, isLeap)
    }

    private val LM = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    private val LD = arrayOf("初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十", "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十", "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十")

    // 简写版
    fun lunarShort(date: Date): String {
        val l = solarToLunar(date)
        return if (l.day == 1) (if (l.isLeap) "闰" else "") + LM[l.month - 1] + "月" else LD[l.day - 1]
    }

    // 新增：完整版
    fun lunarFull(date: Date): String {
        val l = solarToLunar(date)
        return (if (l.isLeap) "闰" else "") + LM[l.month - 1] + "月" + LD[l.day - 1]
    }

    private val FIXED_HOLIDAYS = mapOf("1-1" to "元旦", "2-14" to "情人节", "3-8" to "妇女节", "3-12" to "植树节", "4-1" to "愚人节", "5-1" to "劳动节", "5-4" to "青年节", "6-1" to "儿童节", "7-1" to "建党节", "8-1" to "建军节", "9-10" to "教师节", "10-1" to "国庆节", "12-24" to "平安夜", "12-25" to "圣诞节")
    private val LUNAR_HOLIDAYS = mapOf("1-1" to "春节", "1-15" to "元宵节", "2-2" to "龙抬头", "5-5" to "端午节", "7-7" to "七夕节", "7-15" to "中元节", "8-15" to "中秋节", "9-9" to "重阳节", "12-8" to "腊八节", "12-23" to "小年")

    private fun dynHolidays(y: Int): Map<String, String> {
        val map = mutableMapOf<String, String>()
        run { val c = Calendar.getInstance().apply { set(y, 4, 1) }; while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) c.add(Calendar.DAY_OF_MONTH, 1); c.add(Calendar.DAY_OF_MONTH, 7); map["5-${c.get(Calendar.DAY_OF_MONTH)}"] = "母亲节" }
        run { val c = Calendar.getInstance().apply { set(y, 5, 1) }; while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) c.add(Calendar.DAY_OF_MONTH, 1); c.add(Calendar.DAY_OF_MONTH, 14); map["6-${c.get(Calendar.DAY_OF_MONTH)}"] = "父亲节" }
        run { val c = Calendar.getInstance().apply { set(y, 10, 1) }; while (c.get(Calendar.DAY_OF_WEEK) != Calendar.THURSDAY) c.add(Calendar.DAY_OF_MONTH, 1); c.add(Calendar.DAY_OF_MONTH, 21); map["11-${c.get(Calendar.DAY_OF_MONTH)}"] = "感恩节" }
        return map
    }

    fun getHoliday(date: Date): String? {
        val c = Calendar.getInstance().apply { time = date }
        val key = "${c.get(Calendar.MONTH) + 1}-${c.get(Calendar.DAY_OF_MONTH)}"
        FIXED_HOLIDAYS[key]?.let { return it }
        dynHolidays(c.get(Calendar.YEAR))[key]?.let { return it }
        val tomorrow = Calendar.getInstance().apply { time = date; add(Calendar.DAY_OF_MONTH, 1) }.time
        val tl = solarToLunar(tomorrow)
        if (tl.month == 1 && tl.day == 1 && !tl.isLeap) return "除夕"
        val l = solarToLunar(date)
        if (!l.isLeap) LUNAR_HOLIDAYS["${l.month}-${l.day}"]?.let { return it }
        return null
    }
}
