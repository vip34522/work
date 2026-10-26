package com.example.schedulewidget

import android.graphics.*
import org.json.JSONObject
import java.util.*

object WeekBitmapGenerator {

    fun generateWeekBitmap(width: Int, height: Int, data: JSONObject?, me: JSONObject?, offset: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val today = Date()
        val monday = ShiftCalc.addDays(ShiftCalc.mondayOf(today), offset * 7)
        val weekNames = listOf("一", "二", "三", "四", "五", "六", "日")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF()

        val w = width.toFloat()
        val h = height.toFloat()

        fun getCenteredY(centerY: Float, paint: Paint): Float {
            val fm = paint.fontMetrics
            return centerY - (fm.ascent + fm.descent) / 2f
        }

        // 1. 画带圆角的白色大背景 (避免被系统切角影响)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        rectF.set(0f, 0f, w, h)
        canvas.drawRoundRect(rectF, 20f, 20f, paint)

        // ================= 2. 画上方日历区 =================
        val topH = h * 0.52f
        val cellW = w / 7f
        
        val line1Y = topH * 0.25f
        val line2Y = topH * 0.55f
        val line3Y = topH * 0.82f

        for (i in 0 until 7) {
            val d = ShiftCalc.addDays(monday, i)
            val c = Calendar.getInstance().apply { time = d }
            val isToday = ShiftCalc.isToday(d)
            
            val shift = if (data != null && me != null) ShiftCalc.calcShift(data, me, d) else null
            val shiftStr = ShiftCalc.shiftShort(shift)
            val shiftColor = getShiftColor(shiftStr)
            val centerX = cellW * i + cellW / 2f

            // 今日高亮：浅蓝背景圆角
            if (isToday) {
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#EFF6FF")
                rectF.set(cellW * i + 4f, line1Y - 20f, cellW * (i + 1) - 4f, line3Y + 22f)
                canvas.drawRoundRect(rectF, 12f, 12f, paint)
            }

            // 周几
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = Math.min(cellW * 0.35f, 36f)
            textPaint.color = Color.parseColor(if (isToday) "#2563EB" else "#64748B")
            textPaint.isFakeBoldText = isToday
            canvas.drawText("周${weekNames[i]}", centerX, getCenteredY(line1Y, textPaint), textPaint)

            // 日期
            textPaint.textSize = Math.min(cellW * 0.55f, 60f)
            textPaint.color = Color.parseColor(if (isToday) "#2563EB" else "#1E293B")
            textPaint.isFakeBoldText = true
            canvas.drawText(c.get(Calendar.DAY_OF_MONTH).toString(), centerX, getCenteredY(line2Y, textPaint), textPaint)

            // 班次色块
            val rectW = cellW * 0.8f
            val rectH = Math.min(topH * 0.22f, 40f)
            rectF.set(centerX - rectW / 2f, line3Y - rectH / 2f, centerX + rectW / 2f, line3Y + rectH / 2f)
            
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(shiftColor)
            canvas.drawRoundRect(rectF, 8f, 8f, paint)

            textPaint.textSize = Math.min(rectH * 0.65f, 28f)
            textPaint.color = Color.WHITE
            textPaint.isFakeBoldText = false
            canvas.drawText(shiftStr, centerX, getCenteredY(line3Y, textPaint), textPaint)
        }

        // 优雅的浅灰横线
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(16f, topH, w - 16f, topH + 2f, paint)

        // ================= 3. 画下方详情区 =================
        val botStartY = topH + 2f
        val botH = h - botStartY
        val leftW = w * 0.30f // 左侧留 30%

        // ---------------- 3.1 左下角 ----------------
        val dateItems = mutableListOf<String>()
        val dateColors = mutableListOf<String>()
        
        dateItems.add("${Calendar.getInstance().get(Calendar.MONTH) + 1}月${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}日")
        dateColors.add("#1E293B")
        dateItems.add(LunarCalc.lunarFull(today))
        dateColors.add("#64748B")
        dateItems.add("星期${weekNames[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]}")
        dateColors.add("#64748B")
        
        val holiday = LunarCalc.getHoliday(today)
        if (!holiday.isNullOrEmpty()) {
            dateItems.add(holiday)
            dateColors.add("#EF4444")
        }

        val itemCount = dateItems.size
        val textRowH = Math.min(botH / 4.5f, 45f) 
        val totalTextH = itemCount * textRowH
        var currentY = botStartY + (botH - totalTextH) / 2f + textRowH / 2f 

        textPaint.textAlign = Paint.Align.CENTER
        for (i in 0 until itemCount) {
            textPaint.color = Color.parseColor(dateColors[i])
            textPaint.textSize = if (i == 0) Math.min(textRowH * 0.7f, 40f) else Math.min(textRowH * 0.5f, 30f)
            textPaint.isFakeBoldText = (i == 0 || i == 3) 
            canvas.drawText(dateItems[i], leftW / 2f, getCenteredY(currentY, textPaint), textPaint)
            currentY += textRowH
        }

        // 中间极浅竖线
        paint.color = Color.parseColor("#F1F5F9")
        paint.strokeWidth = 2f
        canvas.drawLine(leftW, botStartY + 12f, leftW, h - 12f, paint)

        // ---------------- 3.2 右下角：人员列表 ----------------
        val personsToShow = getTodayPersons(data)
        val showCount = Math.min(personsToShow.size, 3)
        val listRowH = Math.min(botH / 3.2f, 55f) 
        val listStartY = botStartY + (botH - (showCount * listRowH)) / 2f + listRowH / 2f 

        val rightStartX = leftW + 12f
        // 调整右侧三列的间距
        val colNameX = rightStartX
        val colTypeX = rightStartX + (w * 0.20f) // 班种稍微紧凑一点
        val colBadgeRight = w - 24f // 增大右侧边距，防止出框

        var count = 0
        for (p in personsToShow) {
            if (count >= 3) break
            
            val pName = p.optString("name")
            val shift = ShiftCalc.calcShift(data!!, p, today)
            val shiftStr = ShiftCalc.shiftShort(shift)
            val typeName = getTypeName(data, p.optString("shiftTypeId"))
            val shiftColor = getShiftColor(shiftStr)

            val rowCenterY = listStartY + count * listRowH

            // 【新增】浅色分割线（不拉通，有留白）
            if (count > 0) {
                linePaint.color = Color.parseColor("#F8FAFC") // 极浅的灰
                linePaint.strokeWidth = 2f
                canvas.drawLine(rightStartX, rowCenterY - listRowH / 2f, w - 24f, rowCenterY - listRowH / 2f, linePaint)
            }

            // 1. 姓名
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = Color.parseColor("#2563EB")
            textPaint.isFakeBoldText = true
            textPaint.textSize = Math.min(listRowH * 0.45f, 36f)
            canvas.drawText(pName, colNameX, getCenteredY(rowCenterY, textPaint), textPaint)

            // 2. 班种
            textPaint.color = Color.parseColor("#64748B")
            textPaint.isFakeBoldText = false
            textPaint.textSize = Math.min(listRowH * 0.35f, 26f)
            canvas.drawText(typeName, colTypeX, getCenteredY(rowCenterY, textPaint), textPaint)

            // 3. 班次标签
            val badgeW = Math.min(w * 0.12f, 45f)
            val badgeH = Math.min(listRowH * 0.6f, 32f)
            val badgeLeft = colBadgeRight - badgeW
            
            rectF.set(badgeLeft, rowCenterY - badgeH / 2f, colBadgeRight, rowCenterY + badgeH / 2f)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor(shiftColor)
            canvas.drawRoundRect(rectF, 6f, 6f, paint)

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.WHITE
            textPaint.textSize = Math.min(badgeH * 0.65f, 24f)
            canvas.drawText(shiftStr, badgeLeft + badgeW / 2f, getCenteredY(rowCenterY, textPaint), textPaint)

            count++
        }
        
        return bitmap
    }

    private fun getShiftColor(shiftStr: String): String {
        return when(shiftStr) {
            "休" -> "#EF4444"
            "白" -> "#3B82F6"
            "夜" -> "#1E293B"
            "二" -> "#0EA5E9"
            else -> "#10B981"
        }
    }

    private fun getTypeName(data: JSONObject, typeId: String): String {
        val types = data.optJSONArray("shiftTypes") ?: return "排班"
        for (i in 0 until types.length()) {
            if (types.getJSONObject(i).optString("id") == typeId) {
                return types.getJSONObject(i).optString("name")
            }
        }
        return "排班"
    }
    
    private fun getTodayPersons(data: JSONObject?): List<JSONObject> {
        val list = mutableListOf<JSONObject>()
        val arr = data?.optJSONArray("personnel") ?: return list
        for (i in 0 until arr.length()) {
            list.add(arr.getJSONObject(i))
        }
        return list
    }
}
