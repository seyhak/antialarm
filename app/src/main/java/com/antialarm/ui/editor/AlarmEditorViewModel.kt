package com.antialarm.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antialarm.data.model.Alarm
import com.antialarm.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlarmEditorState(
        val hour: Int = 8,
        val minute: Int = 0,
        val label: String = "",
        val repeatDays: Int = 0,
        val soundUri: String? = null,
        val soundName: String = "Default",
        val mathDifficulty: Int = 1,
        val snoozeMinutes: Int = 5,
        val vibrate: Boolean = true,
        val isEditing: Boolean = false,
        val isSaving: Boolean = false,
        val isLoaded: Boolean = false
)

@HiltViewModel
class AlarmEditorViewModel
@Inject
constructor(private val repository: AlarmRepository, savedStateHandle: SavedStateHandle) :
        ViewModel() {

    private val alarmId: Int = savedStateHandle.get<Int>("alarmId") ?: -1

    private val _state = MutableStateFlow(AlarmEditorState())
    val state: StateFlow<AlarmEditorState> = _state.asStateFlow()

    private var existingAlarm: Alarm? = null

    init {
        if (alarmId > 0) {
            viewModelScope.launch {
                repository.getAlarmById(alarmId)?.let { alarm ->
                    existingAlarm = alarm
                    _state.value =
                            AlarmEditorState(
                                    hour = alarm.hour,
                                    minute = alarm.minute,
                                    label = alarm.label,
                                    repeatDays = alarm.repeatDays,
                                    soundUri = alarm.soundUri,
                                    soundName = if (alarm.soundUri != null) "Custom" else "Default",
                                    mathDifficulty = alarm.mathDifficulty,
                                    snoozeMinutes = alarm.snoozeMinutes,
                                    vibrate = alarm.vibrate,
                                    isEditing = true,
                                    isLoaded = true
                            )
                }
                        ?: run { _state.value = _state.value.copy(isLoaded = true) }
            }
        } else {
            _state.value = _state.value.copy(isLoaded = true)
        }
    }

    fun updateTime(hour: Int, minute: Int) {
        _state.value = _state.value.copy(hour = hour, minute = minute)
    }

    fun updateLabel(label: String) {
        _state.value = _state.value.copy(label = label)
    }

    fun toggleDay(dayFlag: Int) {
        val current = _state.value.repeatDays
        val newDays = current xor dayFlag
        _state.value = _state.value.copy(repeatDays = newDays)
    }

    fun updateSoundUri(uri: String?, name: String) {
        _state.value = _state.value.copy(soundUri = uri, soundName = name)
    }

    fun updateMathDifficulty(difficulty: Int) {
        _state.value = _state.value.copy(mathDifficulty = difficulty)
    }

    fun updateSnoozeMinutes(minutes: Int) {
        _state.value = _state.value.copy(snoozeMinutes = minutes)
    }

    fun updateVibrate(vibrate: Boolean) {
        _state.value = _state.value.copy(vibrate = vibrate)
    }

    fun saveAlarm(onComplete: () -> Unit) {
        _state.value = _state.value.copy(isSaving = true)
        viewModelScope.launch {
            val s = _state.value
            val alarm =
                    Alarm(
                            id = existingAlarm?.id ?: 0,
                            hour = s.hour,
                            minute = s.minute,
                            label = s.label,
                            isEnabled = true,
                            soundUri = s.soundUri,
                            repeatDays = s.repeatDays,
                            mathDifficulty = s.mathDifficulty,
                            snoozeMinutes = s.snoozeMinutes,
                            vibrate = s.vibrate
                    )
            repository.saveAlarm(alarm)
            onComplete()
        }
    }
}
