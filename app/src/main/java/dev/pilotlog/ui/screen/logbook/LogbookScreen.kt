// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.logbook

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pilotlog.domain.model.Flight
import dev.pilotlog.ui.util.LocalDateFormat
import dev.pilotlog.ui.util.formatted
import dev.pilotlog.ui.util.toHhMm
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LogbookScreen(
    onFlightClick: (Long) -> Unit,
    onAddFlight: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: LogbookViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var pageBreakFlight by remember { mutableStateOf<Flight?>(null) }
    var deleteConfirmFlight by remember { mutableStateOf<Flight?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logbook") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    if (!state.isLoading && state.totalMinutes > 0) {
                        Text(
                            text = state.totalMinutes.toHhMm(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.groups.isEmpty() -> EmptyLogbook(
                onAddFlight = onAddFlight,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            else -> Box(Modifier.fillMaxSize()) {
                // Flat label per lazy item (header + each flight share the group's label),
                // so the fast-scroll bubble can show the month/year at any index.
                val itemLabels = remember(state.groups) {
                    buildList {
                        state.groups.forEach { g ->
                            add(g.label)
                            repeat(g.flights.size) { add(g.label) }
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding() + 88.dp,
                        start = 16.dp,
                        end = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.groups.forEach { group ->
                        stickyHeader(key = "header_${group.label}") {
                            MonthHeader(group)
                        }
                        items(group.flights, key = { it.id }) { flight ->
                            SwipeableFlightRow(
                                flight = flight,
                                onClick = { onFlightClick(flight.id) },
                                onLongClick = { pageBreakFlight = flight },
                                onDeleteRequest = { deleteConfirmFlight = flight },
                            )
                        }
                    }
                }

                FastScrollbar(
                    state = listState,
                    itemCount = itemLabels.size,
                    labelFor = { itemLabels.getOrElse(it) { "" } },
                )
            }
        }
    }

    pageBreakFlight?.let { flight ->
        val pageSize = viewModel.pageSizeFor(flight)
        val isBreak = flight.pageBreak
        AlertDialog(
            onDismissRequest = { pageBreakFlight = null },
            title = { Text(if (isBreak) "Page break" else "Mark page break?") },
            text = {
                Text(
                    if (isBreak) {
                        "This flight ends a page of $pageSize flight(s). Removing it folds the page " +
                            "back into the automatic pagination (18 per page)."
                    } else {
                        "This flight becomes the last row of the page — it will hold $pageSize flight(s). " +
                            "The PDF export aligns its page totals to this break."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.togglePageBreak(flight)
                    pageBreakFlight = null
                }) { Text(if (isBreak) "Remove" else "Mark") }
            },
            dismissButton = {
                TextButton(onClick = { pageBreakFlight = null }) { Text("Cancel") }
            },
        )
    }

    deleteConfirmFlight?.let { flight ->
        AlertDialog(
            onDismissRequest = { deleteConfirmFlight = null },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("Delete this flight?") },
            text = {
                Text(
                    "${flight.departureAirport} → ${flight.arrivalAirport} on " +
                        "${flight.date.formatted(LocalDateFormat.current)} will be permanently removed.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(flight)
                    deleteConfirmFlight = null
                    scope.launch { snackbarHost.showSnackbar("Flight deleted") }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmFlight = null }) { Text("Cancel") }
            },
        )
    }
}

/** Google-Photos-style draggable scrollbar with a month/year bubble. */
@Composable
private fun BoxScope.FastScrollbar(
    state: LazyListState,
    itemCount: Int,
    labelFor: (Int) -> String,
) {
    if (itemCount <= 1) return
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val firstVisible by remember { derivedStateOf { state.firstVisibleItemIndex } }
    val activeIndex = (if (dragging) (dragFraction * (itemCount - 1)).roundToInt() else firstVisible)
        .coerceIn(0, itemCount - 1)
    val fraction = activeIndex.toFloat() / (itemCount - 1)

    val showThumb = dragging || state.isScrollInProgress
    val thumbAlpha by animateFloatAsState(if (showThumb) 1f else 0f, label = "thumbAlpha")

    fun jumpTo(yFraction: Float) {
        dragFraction = yFraction.coerceIn(0f, 1f)
        val target = (dragFraction * (itemCount - 1)).roundToInt().coerceIn(0, itemCount - 1)
        scope.launch { state.scrollToItem(target) }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val trackPx = constraints.maxHeight.toFloat()
        val thumbPx = with(density) { 56.dp.toPx() }
        val thumbOffset = ((trackPx - thumbPx).coerceAtLeast(0f) * fraction)

        // Drag strip kept in the right gutter so it doesn't steal row taps.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .width(16.dp)
                .pointerInput(itemCount) {
                    detectVerticalDragGestures(
                        onDragStart = { offset -> dragging = true; jumpTo(offset.y / size.height) },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            jumpTo(change.position.y / size.height)
                        },
                    )
                },
        )

        // Thumb pill
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, thumbOffset.roundToInt()) }
                .padding(end = 4.dp)
                .width(6.dp)
                .height(with(density) { thumbPx.toDp() })
                .alpha(if (dragging) 1f else thumbAlpha)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )

        // Month/year bubble while dragging
        if (dragging) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        IntOffset(0, (thumbOffset + thumbPx / 2 - with(density) { 20.dp.toPx() }).roundToInt())
                    }
                    .padding(end = 18.dp),
            ) {
                Text(
                    text = labelFor(activeIndex),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableFlightRow(
    flight: Flight,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        // A swipe never deletes on its own — it asks for confirmation, then the row
        // snaps back. Deletion only happens once the user confirms the dialog.
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDeleteRequest()
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        FlightRow(flight = flight, onClick = onClick, onLongClick = onLongClick)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FlightRow(flight: Flight, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
      Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(0.18f)) {
                Text(
                    text = "%02d".format(flight.date.dayOfMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "%02d".format(flight.date.monthNumber),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(modifier = Modifier.weight(0.45f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = flight.departureAirport,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = " → ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = flight.arrivalAirport,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = flight.aircraftRegistration.ifBlank { flight.aircraftType },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                modifier = Modifier.weight(0.37f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = flight.totalMinutes.toHhMm(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (flight.nightMinutes > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Icon(
                            Icons.Filled.NightsStay,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = " ${flight.nightMinutes.toHhMm()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
        if (flight.pageBreak) {
            PageEndMarker()
        }
      }
    }
}

/** Discreet end-of-page indicator shown under a flight marked as a page break. */
@Composable
private fun PageEndMarker() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.ContentCut,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "END OF PAGE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun MonthHeader(group: MonthGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 10.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = group.label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = group.totalMinutes.toHhMm(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyLogbook(onAddFlight: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "No flights yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Tap + to log your first flight",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
