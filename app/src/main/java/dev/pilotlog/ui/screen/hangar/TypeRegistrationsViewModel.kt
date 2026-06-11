// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.hangar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilotlog.domain.model.Aircraft
import dev.pilotlog.domain.model.AircraftType
import dev.pilotlog.domain.usecase.aircraft.DeleteRegistrationUseCase
import dev.pilotlog.domain.usecase.aircraft.GetAircraftTypeUseCase
import dev.pilotlog.domain.usecase.aircraft.GetRegistrationsByTypeUseCase
import dev.pilotlog.domain.usecase.aircraft.SaveRegistrationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TypeRegistrationsUiState(
    val typeCode: String = "",
    val type: AircraftType? = null,
    val registrations: List<Aircraft> = emptyList(),
    val addingRegistration: String? = null,   // non-null = add sheet open with current text
)

@HiltViewModel
class TypeRegistrationsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getRegistrationsByType: GetRegistrationsByTypeUseCase,
    private val getType: GetAircraftTypeUseCase,
    private val saveRegistration: SaveRegistrationUseCase,
    private val deleteRegistration: DeleteRegistrationUseCase,
) : ViewModel() {

    private val typeCode: String = savedStateHandle.get<String>("typeCode").orEmpty()

    private val _type = MutableStateFlow<AircraftType?>(null)
    private val _adding = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TypeRegistrationsUiState> = combine(
        getRegistrationsByType(typeCode)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()),
        _type,
        _adding,
    ) { regs, type, adding ->
        TypeRegistrationsUiState(
            typeCode = typeCode,
            type = type,
            registrations = regs,
            addingRegistration = adding,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TypeRegistrationsUiState(typeCode = typeCode),
    )

    init {
        viewModelScope.launch { _type.update { getType(typeCode) } }
    }

    fun openAddForm() = _adding.update { "" }
    fun onAddTextChange(v: String) = _adding.update { v.uppercase() }
    fun dismissAddForm() = _adding.update { null }

    fun saveRegistration() {
        val reg = _adding.value?.trim().orEmpty()
        if (reg.isBlank()) return
        viewModelScope.launch {
            saveRegistration(Aircraft(registration = reg, typeCode = typeCode))
            _adding.update { null }
        }
    }

    fun delete(aircraft: Aircraft) {
        viewModelScope.launch { deleteRegistration(aircraft.registration) }
    }
}
