// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.domain.logbook

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LogbookPagingTest {

    @Test
    fun `splits every 18 with no markers`() {
        val pages = LogbookPaging.paginate((1..40).toList()) { false }
        assertThat(pages.map { it.size }).containsExactly(18, 18, 4).inOrder()
    }

    @Test
    fun `manual marker ends the page early and resets the counter`() {
        // markers on items 10 and 30
        val pages = LogbookPaging.paginate((1..40).toList()) { it == 10 || it == 30 }
        // 1..10 (10) | 11..28 (auto-18) | 29..30 (2) | 31..40 (10)
        assertThat(pages.map { it.size }).containsExactly(10, 18, 2, 10).inOrder()
    }

    @Test
    fun `removing a marker re-merges and auto-resplits while other markers stay`() {
        // only item 30 marked (item 10 marker removed)
        val pages = LogbookPaging.paginate((1..40).toList()) { it == 30 }
        // 1..18 (auto-18) | 19..30 (12, marker) | 31..40 (10)
        assertThat(pages.map { it.size }).containsExactly(18, 12, 10).inOrder()
    }

    @Test
    fun `marker exactly at 18 behaves like the auto-split`() {
        val pages = LogbookPaging.paginate((1..36).toList()) { it == 18 }
        assertThat(pages.map { it.size }).containsExactly(18, 18).inOrder()
    }

    @Test
    fun `pageSizeEndingAt counts from the previous boundary`() {
        // item 30 marked; page ending at index 24 (item 25) starts after the auto-18 at item 18
        val n = LogbookPaging.pageSizeEndingAt((1..40).toList(), index = 24) { it == 30 }
        assertThat(n).isEqualTo(7)   // items 19..25
    }

    @Test
    fun `pageSizeEndingAt right after an earlier marker`() {
        // item 10 marked; ending at index 12 (item 13) -> page started at item 11 -> 11..13 = 3
        val n = LogbookPaging.pageSizeEndingAt((1..40).toList(), index = 12) { it == 10 }
        assertThat(n).isEqualTo(3)
    }

    @Test
    fun `empty list yields no pages`() {
        assertThat(LogbookPaging.paginate(emptyList<Int>()) { false }).isEmpty()
    }

    @Test
    fun `last partial page is kept`() {
        val pages = LogbookPaging.paginate((1..5).toList()) { false }
        assertThat(pages).hasSize(1)
        assertThat(pages.first()).hasSize(5)
    }
}
