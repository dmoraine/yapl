// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.usecase.backup

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import dev.pilotlog.domain.logbook.LogbookPaging
import dev.pilotlog.domain.model.EngineType
import dev.pilotlog.domain.model.Flight
import dev.pilotlog.domain.model.PreviousTotals
import dev.pilotlog.domain.repository.AircraftRepository
import dev.pilotlog.domain.repository.FlightRepository
import dev.pilotlog.domain.usecase.previoustotals.GetPreviousTotalsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * Export the whole logbook as a PDF following the BCAA / EASA column layout.
 * Each logbook page is half an A4 sheet, so an A4 portrait page holds two
 * stacked logbook pages. Per-column totals carry forward page to page, seeded
 * by the "previous totals" (pre-digital hours).
 */
class ExportLogbookPdfUseCase @Inject constructor(
    private val flightRepository: FlightRepository,
    private val aircraftRepository: AircraftRepository,
    private val getPreviousTotals: GetPreviousTotalsUseCase,
) {

    // A4 portrait at 72 dpi (points).
    private val pageW = 595f
    private val pageH = 842f
    private val halfH = pageH / 2f
    private val marginX = 18f
    private val contentW = pageW - 2 * marginX

    private val rowsPerPage = 18
    private val topMargin = 14f
    private val groupHdrH = 18f
    private val leafHdrH = 14f
    private val rowH = 15f
    private val totalsRowH = 14f

    private data class Leaf(val group: String, val label: String, val weight: Float)

    private val leaves = listOf(
        Leaf("DATE", "dd/mm/yy", 34f),
        Leaf("DEPARTURE", "PLACE", 24f), Leaf("DEPARTURE", "TIME", 20f),
        Leaf("ARRIVAL", "PLACE", 24f), Leaf("ARRIVAL", "TIME", 20f),
        Leaf("AIRCRAFT", "TYPE", 34f), Leaf("AIRCRAFT", "REG", 30f),
        Leaf("SINGLE PILOT TIME", "SE", 17f), Leaf("SINGLE PILOT TIME", "ME", 17f),
        Leaf("MULTI-PILOT TIME", "", 22f),
        Leaf("TOTAL TIME OF FLIGHT", "", 24f),
        Leaf("NAME PIC", "", 40f),
        Leaf("LANDINGS", "DAY", 17f), Leaf("LANDINGS", "NIGHT", 17f),
        Leaf("OPERATIONAL CONDITION TIME", "NIGHT", 22f),
        Leaf("OPERATIONAL CONDITION TIME", "IFR", 22f),
        Leaf("PILOT FUNCTION TIME", "PIC", 22f),
        Leaf("PILOT FUNCTION TIME", "CO-PILOT", 22f),
        Leaf("PILOT FUNCTION TIME", "DUAL", 20f),
        Leaf("PILOT FUNCTION TIME", "INSTR", 20f),
        Leaf("FSTD SESSION", "DATE", 16f), Leaf("FSTD SESSION", "TYPE", 18f),
        Leaf("FSTD SESSION", "TOTAL", 16f),
        Leaf("REMARKS AND ENDORSEMENTS", "", 40f),
    )

    // Leaf indices for the value columns (used by both rows and totals).
    private val idxSe = 7
    private val idxMe = 8
    private val idxMulti = 9
    private val idxTotal = 10
    private val idxName = 11
    private val idxLdgDay = 12
    private val idxLdgNight = 13
    private val idxNight = 14
    private val idxIfr = 15
    private val idxPic = 16
    private val idxCo = 17
    private val idxDual = 18
    private val idxInstr = 19
    private val idxRemarks = 23

    private data class Totals(
        var se: Int = 0, var me: Int = 0, var multi: Int = 0, var total: Int = 0,
        var ldgDay: Int = 0, var ldgNight: Int = 0,
        var night: Int = 0, var ifr: Int = 0,
        var pic: Int = 0, var co: Int = 0, var dual: Int = 0, var instr: Int = 0,
    ) {
        operator fun plus(o: Totals) = Totals(
            se + o.se, me + o.me, multi + o.multi, total + o.total,
            ldgDay + o.ldgDay, ldgNight + o.ldgNight,
            night + o.night, ifr + o.ifr,
            pic + o.pic, co + o.co, dual + o.dual, instr + o.instr,
        )
    }

    private fun Totals.valueAt(idx: Int): Pair<Int, Boolean>? = when (idx) {
        idxSe -> se to true; idxMe -> me to true; idxMulti -> multi to true
        idxTotal -> total to true
        idxLdgDay -> ldgDay to false; idxLdgNight -> ldgNight to false
        idxNight -> night to true; idxIfr -> ifr to true
        idxPic -> pic to true; idxCo -> co to true
        idxDual -> dual to true; idxInstr -> instr to true
        else -> null
    }

    suspend operator fun invoke(context: Context, uri: Uri, fromDate: LocalDate? = null): Int {
        val flights = flightRepository.getFlights().first()
            .sortedWith(compareBy({ it.date }, { it.departureTime }))
        val engineByType = aircraftRepository.getTypes().first()
            .associate { it.typeCode to it.engineType }
        val prev = getPreviousTotals()

        // Column x boundaries.
        val scale = contentW / leaves.sumOf { it.weight.toDouble() }.toFloat()
        val xs = FloatArray(leaves.size + 1)
        xs[0] = marginX
        for (i in leaves.indices) xs[i + 1] = xs[i] + leaves[i].weight * scale

        // Pages follow the user's page-break markers, else break every 18 flights.
        val allPages = LogbookPaging.paginate(flights, rowsPerPage) { it.pageBreak }
            .ifEmpty { listOf(emptyList()) }

        // When exporting from a date, start at the FIRST flight of the page that
        // contains that date, and carry forward the totals of the skipped pages.
        val startIndex = if (fromDate == null) 0 else {
            allPages.indexOfFirst { page -> page.any { it.date >= fromDate } }
                .let { if (it < 0) allPages.size else it }
        }
        var running = baselineFrom(prev)
        for (i in 0 until startIndex) running += sumRows(allPages[i], engineByType)
        val renderPages = allPages.drop(startIndex).ifEmpty { listOf(emptyList()) }

        val doc = PdfDocument()
        var pdfPage: PdfDocument.Page? = null
        renderPages.forEachIndexed { i, rows ->
            val slot = i % 2
            if (slot == 0) {
                val info = PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), i / 2 + 1).create()
                pdfPage = doc.startPage(info)
            }
            val originY = if (slot == 0) 0f else halfH
            val pageTotals = sumRows(rows, engineByType)
            val totalRow = running + pageTotals
            drawLogbookPage(
                pdfPage!!.canvas, xs, originY, rows, engineByType,
                pageTotals, running, totalRow, startIndex + i + 1,
            )
            running = totalRow
            if (slot == 1) { doc.finishPage(pdfPage); pdfPage = null }
        }
        if (pdfPage != null) doc.finishPage(pdfPage)

        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            doc.writeTo(out)
        } ?: run { doc.close(); throw IllegalStateException("Could not open file for writing") }
        doc.close()
        return renderPages.sumOf { it.size }
    }

    private fun baselineFrom(p: PreviousTotals?): Totals = if (p == null) Totals() else Totals(
        se = 0, me = 0, multi = 0, total = p.totalMinutes,
        ldgDay = p.totalLandingsDay, ldgNight = p.totalLandingsNight,
        night = p.nightMinutes, ifr = p.ifrMinutes,
        pic = p.picMinutes, co = p.copilotMinutes, dual = p.dualMinutes, instr = p.instructorMinutes,
    )

    private fun sumRows(rows: List<Flight>, engineByType: Map<String, EngineType>): Totals {
        val t = Totals()
        for (f in rows) {
            when {
                f.isMultiPilot -> t.multi += f.totalMinutes
                engineByType[f.aircraftType] == EngineType.SINGLE -> t.se += f.totalMinutes
                else -> t.me += f.totalMinutes
            }
            t.total += f.totalMinutes
            t.ldgDay += f.landingsDay
            t.ldgNight += f.landingsNight
            t.night += f.nightMinutes
            t.ifr += f.ifrMinutes
            t.pic += f.picMinutes
            t.co += f.copilotMinutes
            t.dual += f.dualMinutes
            t.instr += f.instructorMinutes
        }
        return t
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    private val thinPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 0.4f; isAntiAlias = true }
    private val medPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 0.7f; isAntiAlias = true }
    private val thickPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.1f; isAntiAlias = true }
    private val groupPaint = Paint().apply { color = Color.BLACK; textSize = 4.2f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
    private val leafPaint = Paint().apply { color = Color.BLACK; textSize = 4.6f; isAntiAlias = true }
    private val dataPaint = Paint().apply { color = Color.BLACK; textSize = 6f; isAntiAlias = true }
    private val numPaint = Paint().apply { color = Color.BLACK; textSize = 6f; isAntiAlias = true; typeface = Typeface.MONOSPACE }
    private val totalPaint = Paint().apply { color = Color.BLACK; textSize = 6f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }

    private fun drawLogbookPage(
        c: Canvas, xs: FloatArray, originY: Float,
        rows: List<Flight>, engineByType: Map<String, EngineType>,
        pageTotals: Totals, prevTotals: Totals, totalTotals: Totals,
        pageNumber: Int,
    ) {
        val left = xs.first()
        val right = xs.last()
        val groupTop = originY + topMargin
        val leafTop = groupTop + groupHdrH
        val dataTop = leafTop + leafHdrH

        // ── Header ──────────────────────────────────────────────────────────────
        // Single-leaf groups are merged into one tall cell (like the paper book);
        // multi-leaf groups show the group label on top and leaf labels below.
        val groupRanges = mutableListOf<Pair<Int, Int>>()   // start, endExclusive
        var i = 0
        while (i < leaves.size) {
            var j = i
            while (j + 1 < leaves.size && leaves[j + 1].group == leaves[i].group) j++
            val start = i
            val endEx = j + 1
            groupRanges.add(start to endEx)
            val gx0 = xs[start]
            val gx1 = xs[endEx]
            if (endEx - start == 1) {
                val leafLabel = leaves[start].label
                val text = if (leafLabel.isEmpty()) leaves[start].group
                else "${leaves[start].group} ($leafLabel)"
                drawMultiline(c, text, gx0, groupTop, gx1 - gx0, dataTop - groupTop, groupPaint)
            } else {
                drawMultiline(c, leaves[start].group, gx0, groupTop, gx1 - gx0, groupHdrH, groupPaint)
                for (k in start until endEx) {
                    drawCell(c, leaves[k].label, xs[k], leafTop, xs[k + 1] - xs[k], leafHdrH, leafPaint, Align.CENTER)
                }
            }
            i = endEx
        }

        // ── Data rows ───────────────────────────────────────────────────────────
        var y = dataTop
        for (r in 0 until rowsPerPage) {
            val f = rows.getOrNull(r)
            if (f != null) drawFlightRow(c, xs, f, y, engineByType)
            y += rowH
        }
        val dataBottom = y

        // ── Totals rows ─────────────────────────────────────────────────────────
        drawTotalsRow(c, xs, "TOTAL THIS PAGE", pageTotals, y); y += totalsRowH
        drawTotalsRow(c, xs, "TOTAL FROM PREVIOUS PAGES", prevTotals, y); y += totalsRowH
        drawTotalsRow(c, xs, "TOTAL TIME", totalTotals, y); y += totalsRowH
        val tableBottom = y

        // ── Grid ────────────────────────────────────────────────────────────────
        // Vertical separators: group-outer boundaries run the full height (medium);
        // interior leaf boundaries start below the merged group label (thin).
        for (k in 1 until leaves.size) {
            val outer = leaves[k - 1].group != leaves[k].group
            // Interior boundaries of the totals label span (leaves 0..6) stop at the
            // data bottom so the "TOTAL …" label cells read as one merged box.
            val bottomY = if (k in 1..6) dataBottom else tableBottom
            c.drawLine(xs[k], if (outer) groupTop else leafTop, xs[k], bottomY, if (outer) medPaint else thinPaint)
        }
        // Horizontal: group/leaf separator only under multi-leaf groups.
        for ((start, endEx) in groupRanges) {
            if (endEx - start > 1) c.drawLine(xs[start], leafTop, xs[endEx], leafTop, thinPaint)
        }
        c.drawLine(left, dataTop, right, dataTop, medPaint)            // header / data
        var ry = dataTop + rowH
        for (r in 1 until rowsPerPage) { c.drawLine(left, ry, right, ry, thinPaint); ry += rowH }
        c.drawLine(left, dataBottom, right, dataBottom, medPaint)      // data / totals
        var ty = dataBottom + totalsRowH
        for (t in 1 until 3) { c.drawLine(left, ty, right, ty, medPaint); ty += totalsRowH }
        // Outer frame (thick), drawn last.
        c.drawRect(left, groupTop, right, tableBottom, thickPaint)

        // Page number in the remarks column of the last totals row.
        drawCell(c, "Page $pageNumber", xs[idxRemarks], dataBottom + 2 * totalsRowH, xs[idxRemarks + 1] - xs[idxRemarks], totalsRowH, leafPaint, Align.CENTER)
    }

    private fun drawFlightRow(c: Canvas, xs: FloatArray, f: Flight, top: Float, engineByType: Map<String, EngineType>) {
        fun cell(idx: Int, text: String, paint: Paint = numPaint, align: Align = Align.CENTER) =
            drawCell(c, text, xs[idx], top, xs[idx + 1] - xs[idx], rowH, paint, align)

        cell(0, "%02d/%02d/%02d".format(f.date.dayOfMonth, f.date.monthNumber, f.date.year % 100))
        cell(1, f.departureAirport, dataPaint)
        cell(2, "%02d:%02d".format(f.departureTime.hour, f.departureTime.minute))
        cell(3, f.arrivalAirport, dataPaint)
        cell(4, "%02d:%02d".format(f.arrivalTime.hour, f.arrivalTime.minute))
        cell(5, f.aircraftType, dataPaint)
        cell(6, f.aircraftRegistration, dataPaint)

        when {
            f.isMultiPilot -> cell(idxMulti, hhmm(f.totalMinutes))
            engineByType[f.aircraftType] == EngineType.SINGLE -> cell(idxSe, hhmm(f.totalMinutes))
            else -> cell(idxMe, hhmm(f.totalMinutes))
        }
        cell(idxTotal, hhmm(f.totalMinutes))
        cell(idxName, f.picName, dataPaint, Align.LEFT)
        if (f.landingsDay > 0) cell(idxLdgDay, f.landingsDay.toString())
        if (f.landingsNight > 0) cell(idxLdgNight, f.landingsNight.toString())
        cell(idxNight, hhmm(f.nightMinutes))
        cell(idxIfr, hhmm(f.ifrMinutes))
        cell(idxPic, hhmm(f.picMinutes))
        cell(idxCo, hhmm(f.copilotMinutes))
        cell(idxDual, hhmm(f.dualMinutes))
        cell(idxInstr, hhmm(f.instructorMinutes))

        val remarks = listOf(f.flightNumber, f.remarks).filter { it.isNotBlank() }.joinToString("  ")
        if (remarks.isNotBlank()) cell(idxRemarks, remarks, dataPaint, Align.LEFT)
    }

    private fun drawTotalsRow(c: Canvas, xs: FloatArray, label: String, t: Totals, top: Float) {
        // Label spans leaves 0..6 (date .. registration).
        drawCell(c, label, xs[0], top, xs[7] - xs[0], totalsRowH, totalPaint, Align.CENTER)
        for (k in leaves.indices) {
            val v = t.valueAt(k) ?: continue
            val (value, isDuration) = v
            if (value <= 0) continue
            drawCell(c, if (isDuration) hhmm(value) else value.toString(), xs[k], top, xs[k + 1] - xs[k], totalsRowH, totalPaint, Align.CENTER)
        }
    }

    private enum class Align { LEFT, CENTER }

    private fun drawCell(c: Canvas, text: String, x: Float, top: Float, w: Float, h: Float, paint: Paint, align: Align) {
        if (text.isEmpty()) return
        c.save()
        c.clipRect(x, top, x + w, top + h)
        val baseline = top + h / 2f - (paint.descent() + paint.ascent()) / 2f
        when (align) {
            Align.CENTER -> { paint.textAlign = Paint.Align.CENTER; c.drawText(text, x + w / 2f, baseline, paint) }
            Align.LEFT -> { paint.textAlign = Paint.Align.LEFT; c.drawText(text, x + 1.5f, baseline, paint) }
        }
        c.restore()
    }

    /** Centered, up to two lines, used for group headers that may not fit on one line. */
    private fun drawMultiline(c: Canvas, text: String, x: Float, top: Float, w: Float, h: Float, paint: Paint) {
        c.save()
        c.clipRect(x, top, x + w, top + h)
        paint.textAlign = Paint.Align.CENTER
        val cx = x + w / 2f
        if (paint.measureText(text) <= w - 2f || !text.contains(' ')) {
            val baseline = top + h / 2f - (paint.descent() + paint.ascent()) / 2f
            c.drawText(text, cx, baseline, paint)
        } else {
            // Split into two roughly balanced lines on a space.
            val words = text.split(' ')
            val mid = words.size / 2
            val l1 = words.subList(0, mid).joinToString(" ")
            val l2 = words.subList(mid, words.size).joinToString(" ")
            val lineH = (paint.descent() - paint.ascent())
            val totalTextH = lineH * 2
            var ly = top + (h - totalTextH) / 2f - paint.ascent()
            c.drawText(l1, cx, ly, paint); ly += lineH
            c.drawText(l2, cx, ly, paint)
        }
        c.restore()
    }

    private fun hhmm(min: Int): String = if (min <= 0) "" else "%d:%02d".format(min / 60, min % 60)
}
