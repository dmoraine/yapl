// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilotlog.domain.model.Aircraft
import dev.pilotlog.domain.model.Airport
import dev.pilotlog.domain.model.Flight
import dev.pilotlog.domain.model.AircraftType
import dev.pilotlog.domain.model.EngineType
import dev.pilotlog.domain.model.PilotRole
import dev.pilotlog.domain.usecase.aircraft.GetAircraftTypeUseCase
import dev.pilotlog.domain.usecase.aircraft.GetAircraftTypesUseCase
import dev.pilotlog.domain.usecase.aircraft.GetRegistrationsByTypeUseCase
import dev.pilotlog.domain.usecase.aircraft.SaveAircraftTypeUseCase
import dev.pilotlog.domain.usecase.aircraft.SaveRegistrationUseCase
import dev.pilotlog.domain.usecase.airport.GetAirportByIcaoUseCase
import dev.pilotlog.domain.usecase.airport.SaveCustomAirportUseCase
import dev.pilotlog.domain.usecase.airport.SearchAirportsUseCase
import dev.pilotlog.domain.usecase.settings.GetSettingsUseCase
import dev.pilotlog.domain.usecase.flight.AddFlightUseCase
import dev.pilotlog.domain.usecase.flight.GetFlightsUseCase
import dev.pilotlog.domain.usecase.flight.UpdateFlightUseCase
import dev.pilotlog.domain.usecase.nighttime.CalculateNightTimeUseCase
import dev.pilotlog.domain.repository.FlightRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import javax.inject.Inject

data class FlightFormState(
    val flightId: Long? = null,

    val date: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),

    val depQuery: String = "",
    val depAirport: Airport? = null,
    val depSuggestions: List<Airport> = emptyList(),
    val depTime: LocalTime? = null,

    val arrQuery: String = "",
    val arrAirport: Airport? = null,
    val arrSuggestions: List<Airport> = emptyList(),
    val arrTime: LocalTime? = null,

    val totalMinutes: Int = 0,

    val nightMinutes: Int = 0,
    val nightIsAuto: Boolean = true,
    val isNightCalculating: Boolean = false,

    val ifrMinutes: Int = 0,

    val registration: String = "",
    val aircraftType: String = "",
    val isMultiPilot: Boolean = true,
    val availableTypes: List<AircraftType> = emptyList(),
    val availableRegistrations: List<String> = emptyList(),

    val picMinutes: Int = 0,
    val copilotMinutes: Int = 0,
    val dualMinutes: Int = 0,
    val instructorMinutes: Int = 0,
    val activeRole: PilotRole? = null,   // role whose minutes track total automatically

    val picName: String = "",

    val takeoffsDay: Int = 1,
    val takeoffsNight: Int = 0,
    val landingsDay: Int = 1,
    val landingsNight: Int = 0,
    val takeoffByMe: Boolean = true,
    val landingByMe: Boolean = true,
    val depIsNight: Boolean? = null,   // null = unknown (no coords / no time yet)
    val arrIsNight: Boolean? = null,

    val flightNumber: String = "",
    val remarks: String = "",

    val isSaving: Boolean = false,
    val savedFlightId: Long? = null,
    val error: String? = null,
)

private data class NightCalcInput(
    val dep: Airport,
    val arr: Airport,
    val date: LocalDate,
    val depTime: LocalTime,
    val arrTime: LocalTime,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class AddEditFlightViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flightRepository: FlightRepository,
    private val addFlight: AddFlightUseCase,
    private val updateFlight: UpdateFlightUseCase,
    private val searchAirports: SearchAirportsUseCase,
    private val getAircraftTypes: GetAircraftTypesUseCase,
    private val getAircraftType: GetAircraftTypeUseCase,
    private val getRegistrationsByType: GetRegistrationsByTypeUseCase,
    private val getAirportByIcao: GetAirportByIcaoUseCase,
    private val saveCustomAirport: SaveCustomAirportUseCase,
    private val saveAircraftType: SaveAircraftTypeUseCase,
    private val saveRegistration: SaveRegistrationUseCase,
    private val getSettings: GetSettingsUseCase,
    private val calculateNightTime: CalculateNightTimeUseCase,
) : ViewModel() {

    private val editFlightId: Long? = savedStateHandle.get<Long>("flightId")?.takeIf { it > 0 }

    private val _state = MutableStateFlow(FlightFormState(flightId = editFlightId))
    val state: StateFlow<FlightFormState> = _state.asStateFlow()

    init {
        loadAircraftTypes()
        if (editFlightId != null) loadExistingFlight(editFlightId) else prefillNewFlight()
        observeNightTimeCalculation()
    }

    /** Apply settings defaults + smart departure prefill to a fresh flight form. */
    private fun prefillNewFlight() {
        viewModelScope.launch {
            val settings = getSettings()
            // Departure = last arrival (for round trips / turnarounds), but only if the
            // last flight is recent — after more than 3 idle days, fall back to home base.
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val lastFlight = flightRepository.getMostRecentFlight()
            val lastArrival = lastFlight
                ?.takeIf { it.date.daysUntil(today) <= MAX_TURNAROUND_DAYS }
                ?.arrivalAirport
            val depIcao = (lastArrival ?: settings.homeBase).trim().uppercase()
            val depAirport = depIcao.takeIf { it.isNotBlank() }?.let { getAirportByIcao(it) }
            _state.update { s ->
                s.copy(
                    picName = settings.pilotName,
                    activeRole = settings.defaultRole,
                    depQuery = depIcao,
                    depAirport = depAirport,
                ).recomputeOps()
            }
        }
    }

    private fun loadAircraftTypes() {
        viewModelScope.launch {
            getAircraftTypes().collect { typesWithCount ->
                _state.update { it.copy(availableTypes = typesWithCount.map { t -> t.type }) }
            }
        }
    }

    private suspend fun registrationsFor(typeCode: String): List<String> =
        getRegistrationsByType(typeCode).first().map { it.registration }

    private fun loadExistingFlight(id: Long) {
        viewModelScope.launch {
            val flight = flightRepository.getFlightById(id) ?: return@launch
            // Resolve full Airport objects so the night-time observer can trigger
            val depAirport = getAirportByIcao(flight.departureAirport)
            val arrAirport = getAirportByIcao(flight.arrivalAirport)
            val regs = if (flight.aircraftType.isNotBlank())
                registrationsFor(flight.aircraftType) else emptyList()
            _state.update { s ->
                s.copy(
                    flightId = id,
                    date = flight.date,
                    depQuery = flight.departureAirport,
                    depAirport = depAirport,
                    depTime = flight.departureTime,
                    arrQuery = flight.arrivalAirport,
                    arrAirport = arrAirport,
                    arrTime = flight.arrivalTime,
                    totalMinutes = flight.totalMinutes,
                    nightMinutes = flight.nightMinutes,
                    nightIsAuto = true,
                    ifrMinutes = flight.ifrMinutes,
                    registration = flight.aircraftRegistration,
                    aircraftType = flight.aircraftType,
                    isMultiPilot = flight.isMultiPilot,
                    availableRegistrations = regs,
                    picMinutes = flight.picMinutes,
                    copilotMinutes = flight.copilotMinutes,
                    dualMinutes = flight.dualMinutes,
                    instructorMinutes = flight.instructorMinutes,
                    activeRole = null,
                    picName = flight.picName,
                    takeoffsDay = flight.takeoffsDay,
                    takeoffsNight = flight.takeoffsNight,
                    landingsDay = flight.landingsDay,
                    landingsNight = flight.landingsNight,
                    takeoffByMe = (flight.takeoffsDay + flight.takeoffsNight) > 0,
                    landingByMe = (flight.landingsDay + flight.landingsNight) > 0,
                    flightNumber = flight.flightNumber,
                    remarks = flight.remarks,
                ).classifyNight()   // display day/night, keep stored counts
            }
        }
    }

    private fun observeNightTimeCalculation() {
        viewModelScope.launch {
            _state
                .mapNotNull { s ->
                    val dep = s.depAirport ?: return@mapNotNull null
                    val arr = s.arrAirport ?: return@mapNotNull null
                    val depTime = s.depTime ?: return@mapNotNull null
                    val arrTime = s.arrTime ?: return@mapNotNull null
                    if (!s.nightIsAuto) return@mapNotNull null
                    NightCalcInput(dep, arr, s.date, depTime, arrTime)
                }
                .distinctUntilChanged()
                .debounce(400)
                .collectLatest { input ->
                    android.util.Log.d("NightCalc", "FIRING dep=${input.dep.icao}(${input.dep.latitude},${input.dep.longitude}) arr=${input.arr.icao}(${input.arr.latitude},${input.arr.longitude}) ${input.depTime}->${input.arrTime}")
                    _state.update { it.copy(isNightCalculating = true) }
                    val nightMin = calculateNightTime.execute(
                        input.dep, input.arr, input.date, input.depTime, input.arrTime,
                    )
                    android.util.Log.d("NightCalc", "RESULT nightMin=$nightMin")
                    _state.update { it.copy(nightMinutes = nightMin, isNightCalculating = false) }
                }
        }
    }

    // ── Route ─────────────────────────────────────────────────────────────────

    fun onDateChange(date: LocalDate) = _state.update { it.copy(date = date).recomputeOps() }

    fun onDepQueryChange(query: String) {
        _state.update { it.copy(depQuery = query, depAirport = null, depSuggestions = emptyList()) }
        viewModelScope.launch {
            // Try exact ICAO match first (4-letter code typed directly without dropdown)
            if (query.length == 4 && query.all { it.isLetter() }) {
                val exact = getAirportByIcao(query)
                if (exact != null) {
                    _state.update { it.copy(depAirport = exact, depSuggestions = emptyList()).recomputeOps() }
                    return@launch
                }
            }
            if (query.length >= 2) {
                val results = searchAirports(query)
                _state.update { it.copy(depSuggestions = results) }
            }
        }
    }

    fun onDepAirportSelected(airport: Airport) {
        _state.update { it.copy(
            depQuery = airport.icao,
            depAirport = airport,
            depSuggestions = emptyList(),
        ).recomputeOps() }
    }

    fun onDepTimeChange(time: LocalTime?) {
        _state.update { s -> s.withTotal(computeTotal(time, s.arrTime)).copy(depTime = time).recomputeOps() }
    }

    fun onArrQueryChange(query: String) {
        _state.update { it.copy(arrQuery = query, arrAirport = null, arrSuggestions = emptyList()) }
        viewModelScope.launch {
            if (query.length == 4 && query.all { it.isLetter() }) {
                val exact = getAirportByIcao(query)
                if (exact != null) {
                    _state.update { it.copy(arrAirport = exact, arrSuggestions = emptyList()).recomputeOps() }
                    return@launch
                }
            }
            if (query.length >= 2) {
                val results = searchAirports(query)
                _state.update { it.copy(arrSuggestions = results) }
            }
        }
    }

    fun onArrAirportSelected(airport: Airport) {
        _state.update { it.copy(
            arrQuery = airport.icao,
            arrAirport = airport,
            arrSuggestions = emptyList(),
        ).recomputeOps() }
    }

    fun onArrTimeChange(time: LocalTime?) {
        _state.update { s -> s.withTotal(computeTotal(s.depTime, time)).copy(arrTime = time).recomputeOps() }
    }

    /** Persist a user-created airport, then select it for departure or arrival. */
    fun addCustomAirport(airport: Airport, isDeparture: Boolean) {
        viewModelScope.launch {
            saveCustomAirport(airport)
            if (isDeparture) onDepAirportSelected(airport) else onArrAirportSelected(airport)
        }
    }

    private fun computeTotal(dep: LocalTime?, arr: LocalTime?): Int {
        if (dep == null || arr == null) return 0
        val depMin = dep.hour * 60 + dep.minute
        val arrMin = arr.hour * 60 + arr.minute
        return if (arrMin >= depMin) arrMin - depMin else 1440 - depMin + arrMin
    }

    /** Set total and, if a role is active, mirror it into that role's minutes. */
    private fun FlightFormState.withTotal(total: Int): FlightFormState {
        val base = copy(totalMinutes = total)
        return when (activeRole) {
            PilotRole.PIC        -> base.copy(picMinutes = total)
            PilotRole.COPILOT    -> base.copy(copilotMinutes = total)
            PilotRole.DUAL       -> base.copy(dualMinutes = total)
            PilotRole.INSTRUCTOR -> base.copy(instructorMinutes = total)
            null                 -> base
        }
    }

    fun onTotalMinutesChange(minutes: Int) = _state.update { it.withTotal(minutes) }

    // ── Take-off / landing day-night classification ─────────────────────────────

    /** Determine whether departure / arrival fall in aeronautical night (display). */
    private fun FlightFormState.classifyNight(): FlightFormState {
        val dt = depTime
        val at = arrTime
        val depN = depAirport?.let { a -> dt?.let { calculateNightTime.isNightAt(a, date, it) } }
        val arrDate = if (dt != null && at != null && at < dt)
            date.plus(1, DateTimeUnit.DAY) else date
        val arrN = arrAirport?.let { a -> at?.let { calculateNightTime.isNightAt(a, arrDate, it) } }
        return copy(depIsNight = depN, arrIsNight = arrN)
    }

    /** Turn the "by me" toggles + night classification into the day/night counts. */
    private fun FlightFormState.applyOps(): FlightFormState = copy(
        takeoffsNight = if (takeoffByMe && depIsNight == true) 1 else 0,
        takeoffsDay   = if (takeoffByMe && depIsNight != true) 1 else 0,
        landingsNight = if (landingByMe && arrIsNight == true) 1 else 0,
        landingsDay   = if (landingByMe && arrIsNight != true) 1 else 0,
    )

    private fun FlightFormState.recomputeOps(): FlightFormState = classifyNight().applyOps()

    fun onTakeoffByMeChange(v: Boolean) = _state.update { it.copy(takeoffByMe = v).recomputeOps() }
    fun onLandingByMeChange(v: Boolean) = _state.update { it.copy(landingByMe = v).recomputeOps() }

    // ── Times ─────────────────────────────────────────────────────────────────

    fun onNightMinutesChange(minutes: Int) = _state.update {
        it.copy(nightMinutes = minutes, nightIsAuto = false)
    }

    fun onNightAutoReset() = _state.update { it.copy(nightIsAuto = true) }

    fun onIfrMinutesChange(minutes: Int) = _state.update { it.copy(ifrMinutes = minutes) }

    // ── Aircraft ──────────────────────────────────────────────────────────────

    /** Pick a type → set engine-derived multi-crew and load its registrations. */
    fun onTypeSelected(typeCode: String) {
        _state.update { it.copy(aircraftType = typeCode, registration = "", availableRegistrations = emptyList()) }
        viewModelScope.launch {
            val type = getAircraftType(typeCode)
            val regs = registrationsFor(typeCode)
            _state.update { s ->
                s.copy(
                    availableRegistrations = regs,
                    isMultiPilot = type?.engineType?.let { it == EngineType.MULTI } ?: s.isMultiPilot,
                )
            }
        }
    }

    fun onRegistrationSelected(reg: String) = _state.update { it.copy(registration = reg.uppercase()) }

    fun onMultiPilotChange(value: Boolean) = _state.update { it.copy(isMultiPilot = value) }

    /** Persist a new aircraft type (the types Flow auto-refreshes), then select it. */
    fun addAircraftType(type: AircraftType) {
        viewModelScope.launch {
            saveAircraftType(type)
            onTypeSelected(type.typeCode.trim().uppercase())
        }
    }

    /** Persist a new registration under the current type, then select it. */
    fun addRegistration(registration: String) {
        val typeCode = _state.value.aircraftType
        val reg = registration.trim().uppercase()
        if (typeCode.isBlank() || reg.isBlank()) return
        viewModelScope.launch {
            saveRegistration(Aircraft(registration = reg, typeCode = typeCode))
            val regs = registrationsFor(typeCode)
            _state.update { it.copy(availableRegistrations = regs, registration = reg) }
        }
    }

    // ── Role shortcuts ────────────────────────────────────────────────────────

    fun applyRoleCaptain() = _state.update { s ->
        s.copy(activeRole = PilotRole.PIC, picMinutes = s.totalMinutes, copilotMinutes = 0, dualMinutes = 0, instructorMinutes = 0)
    }

    fun applyRoleFO() = _state.update { s ->
        s.copy(activeRole = PilotRole.COPILOT, copilotMinutes = s.totalMinutes, picMinutes = 0, dualMinutes = 0, instructorMinutes = 0)
    }

    fun applyRoleStudent() = _state.update { s ->
        s.copy(activeRole = PilotRole.DUAL, dualMinutes = s.totalMinutes, picMinutes = 0, copilotMinutes = 0, instructorMinutes = 0)
    }

    fun applyRoleInstructor() = _state.update { s ->
        s.copy(activeRole = PilotRole.INSTRUCTOR, instructorMinutes = s.totalMinutes, picMinutes = 0, copilotMinutes = 0, dualMinutes = 0)
    }

    // Manual edits release the automatic role tracking
    fun onPicMinutesChange(v: Int) = _state.update { it.copy(picMinutes = v, activeRole = null) }
    fun onCopilotMinutesChange(v: Int) = _state.update { it.copy(copilotMinutes = v, activeRole = null) }
    fun onDualMinutesChange(v: Int) = _state.update { it.copy(dualMinutes = v, activeRole = null) }
    fun onInstructorMinutesChange(v: Int) = _state.update { it.copy(instructorMinutes = v, activeRole = null) }


    // ── Misc ──────────────────────────────────────────────────────────────────

    fun onPicNameChange(v: String) = _state.update { it.copy(picName = v) }
    fun onFlightNumberChange(v: String) = _state.update { it.copy(flightNumber = v) }
    fun onRemarksChange(v: String) = _state.update { it.copy(remarks = v) }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save() {
        val s = _state.value
        if (s.isSaving) return
        if (s.depQuery.isBlank() || s.arrQuery.isBlank()) {
            _state.update { it.copy(error = "Departure and arrival airports are required") }
            return
        }
        if (s.depTime == null || s.arrTime == null) {
            _state.update { it.copy(error = "Departure and arrival times are required") }
            return
        }
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val flight = Flight(
                id = s.flightId ?: 0L,
                date = s.date,
                departureAirport = s.depQuery.trim().uppercase(),
                departureTime = s.depTime,
                arrivalAirport = s.arrQuery.trim().uppercase(),
                arrivalTime = s.arrTime,
                aircraftType = s.aircraftType,
                aircraftRegistration = s.registration,
                totalMinutes = s.totalMinutes,
                nightMinutes = s.nightMinutes,
                ifrMinutes = s.ifrMinutes,
                picMinutes = s.picMinutes,
                copilotMinutes = s.copilotMinutes,
                dualMinutes = s.dualMinutes,
                instructorMinutes = s.instructorMinutes,
                takeoffsDay = s.takeoffsDay,
                takeoffsNight = s.takeoffsNight,
                landingsDay = s.landingsDay,
                landingsNight = s.landingsNight,
                isMultiPilot = s.isMultiPilot,
                picName = s.picName.trim(),
                flightNumber = s.flightNumber,
                remarks = s.remarks,
            )
            val savedId = if (s.flightId == null) {
                addFlight(flight)
            } else {
                updateFlight(flight)
                s.flightId
            }
            _state.update { it.copy(isSaving = false, savedFlightId = savedId) }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private companion object {
        /** Beyond this gap since the last flight, prefill departure with home base. */
        const val MAX_TURNAROUND_DAYS = 3
    }
}
