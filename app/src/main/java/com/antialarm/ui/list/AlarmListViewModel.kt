package com.antialarm.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antialarm.data.model.Alarm
import com.antialarm.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val repository: AlarmRepository
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = repository.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastDeletedAlarm: Alarm? = null

    fun toggleAlarm(id: Int, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlarm(id, enabled)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        lastDeletedAlarm = alarm
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }

    fun undoDelete() {
        lastDeletedAlarm?.let { alarm ->
            viewModelScope.launch {
                repository.saveAlarm(alarm)
            }
            lastDeletedAlarm = null
        }
    }
}
