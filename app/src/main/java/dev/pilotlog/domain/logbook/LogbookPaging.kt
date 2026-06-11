// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.logbook

/**
 * Splits a chronological flight list into paper-logbook pages.
 *
 * A page ends at a flight flagged as a page break (the user's manual marker) or
 * after [ROWS_PER_PAGE] flights — whichever comes first. The counter resets at
 * every page boundary.
 *
 * As a consequence, removing one manual marker simply re-merges its page with
 * the following flights and the automatic 18-row split re-applies, while every
 * other marker stays a fixed boundary. The reconciliation is therefore automatic
 * and never breaks the markers the user kept.
 */
object LogbookPaging {

    const val ROWS_PER_PAGE = 18

    /** Group [items] into pages. Order is preserved (expected: oldest → newest). */
    fun <T> paginate(
        items: List<T>,
        rowsPerPage: Int = ROWS_PER_PAGE,
        isBreak: (T) -> Boolean,
    ): List<List<T>> {
        require(rowsPerPage > 0) { "rowsPerPage must be > 0" }
        val pages = ArrayList<List<T>>()
        var page = ArrayList<T>()
        for (item in items) {
            page.add(item)
            if (isBreak(item) || page.size >= rowsPerPage) {
                pages.add(page)
                page = ArrayList()
            }
        }
        if (page.isNotEmpty()) pages.add(page)
        return pages
    }

    /**
     * Number of flights on the page that would end at [index] if that flight were
     * a page break — used for the "N flights on this page" feedback while marking.
     */
    fun <T> pageSizeEndingAt(
        items: List<T>,
        index: Int,
        rowsPerPage: Int = ROWS_PER_PAGE,
        isBreak: (T) -> Boolean,
    ): Int {
        require(index in items.indices) { "index out of bounds" }
        var lastPageSize = 0
        var count = 0
        for (i in 0..index) {
            count++
            val boundary = i == index || isBreak(items[i]) || count >= rowsPerPage
            if (boundary) {
                lastPageSize = count
                count = 0
            }
        }
        return lastPageSize
    }
}
