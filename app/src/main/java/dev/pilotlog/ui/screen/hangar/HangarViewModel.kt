// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Didier Moraine
package dev.pilotlog.ui.screen.hangar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pilotlog.domain.model.AircraftType
import dev.pilotlog.domain.model.AircraftTypeWithCount
import dev.pilotlog.domain.model.EngineType
import dev.pilotlog.domain.usecase.aircraft.DeleteAircraftTypeUseCase
import dev.pilotlog.domain.usecase.aircraft.GetAircraftTypesUseCase
import dev.pilotlog.domain.usecase.aircraft.SaveAircraftTypeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TypeFormState(
    val originalCode: String? = null,   // null = adding new
    val typeCode: String = "",
    val typeName: String = "",
    val engineType: EngineType = EngineType.MULTI,
)

data class HangarUiState(
    val types: List<AircraftTypeWithCount> = emptyList(),
    val isLoading: Boolean = true,
    val editForm: TypeFormState? = null,
)

@HiltViewModel
class HangarViewModel @Inject constructor(
    private val getTypes: GetAircraftTypesUseCase,
    private val saveType: SaveAircraftTypeUseCase,
    private val deleteType: DeleteAircraftTypeUseCase,
) : ViewModel() {

    private val _formState = MutableStateFlow<TypeFormState?>(null)

    val uiState: StateFlow<HangarUiState> = combine(
        getTypes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()),
        _formState,
    ) { types, form ->
        HangarUiState(types = types, isLoading = false, editForm = form)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HangarUiState())

    fun openAddForm() = _formState.update { TypeFormState() }

    fun openEditForm(type: AircraftType) = _formState.update {
        TypeFormState(
            originalCode = type.typeCode,
            typeCode = type.typeCode,
            typeName = type.typeName,
            engineType = type.engineType,
        )
    }

    fun dismissForm() = _formState.update { null }

    fun onTypeCodeChange(v: String) = _formState.update { it?.copy(typeCode = v.uppercase()) }
    fun onTypeNameChange(v: String) = _formState.update { it?.copy(typeName = v) }
    fun onEngineTypeChange(v: EngineType) = _formState.update { it?.copy(engineType = v) }

    fun saveForm() {
        val form = _formState.value ?: return
        if (form.typeCode.isBlank()) return
        viewModelScope.launch {
            // If the code was renamed, drop the old row
            form.originalCode?.let { if (it != form.typeCode.trim().uppercase()) deleteType(it) }
            saveType(
                AircraftType(
                    typeCode = form.typeCode,
                    typeName = form.typeName.ifBlank { form.typeCode },
                    engineType = form.engineType,
                ),
            )
            _formState.update { null }
        }
    }

    fun delete(type: AircraftType) {
        viewModelScope.launch { deleteType(type.typeCode) }
    }
}
