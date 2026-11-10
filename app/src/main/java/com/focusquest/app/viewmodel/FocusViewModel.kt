package com.focusquest.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focusquest.app.data.FocusDatabase
import com.focusquest.app.data.TaskEntity
import com.focusquest.app.data.TaskRepository
import com.focusquest.app.blocker.FocusBlockerPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FocusViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = TaskRepository(FocusDatabase.get(app).taskDao())

    val tasks = repository.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val totalXp = repository.totalXp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _selectedMinutes = MutableStateFlow(25)
    val selectedMinutes = _selectedMinutes.asStateFlow()
    private val _remainingMillis = MutableStateFlow(25L * 60_000L)
    val remainingMillis = _remainingMillis.asStateFlow()
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()
    private val _lastSessionXp = MutableStateFlow<Int?>(null)
    val lastSessionXp = _lastSessionXp.asStateFlow()

    private var timerJob: Job? = null
    private var endAtMillis: Long = 0L

    suspend fun refreshTasks() { repository.refresh() }

    fun addTask(title: String, description: String, priority: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.add(title.trim(), description.trim(), priority) }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch { repository.complete(task.id) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { repository.delete(task.id) }
    }

    fun selectFocusDuration(minutes: Int) {
        if (_isTimerRunning.value || minutes !in listOf(15, 25, 45, 60)) return
        _selectedMinutes.value = minutes
        _remainingMillis.value = minutes * 60_000L
        _lastSessionXp.value = null
    }

    fun startOrResumeFocus() {
        if (_isTimerRunning.value) return
        if (_remainingMillis.value <= 0L) _remainingMillis.value = _selectedMinutes.value * 60_000L
        endAtMillis = System.currentTimeMillis() + _remainingMillis.value
        _isTimerRunning.value = true
        FocusBlockerPreferences.setFocusActive(getApplication(), true, endAtMillis)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value) {
                val left = (endAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                _remainingMillis.value = left
                if (left == 0L) {
                    _isTimerRunning.value = false
                    FocusBlockerPreferences.setFocusActive(getApplication(), false)
                    val minutes = _selectedMinutes.value
                    repository.addFocusSession(minutes)
                    _lastSessionXp.value = minutes * 2
                    break
                }
                delay(250L)
            }
        }
    }

    fun pauseFocus() {
        if (!_isTimerRunning.value) return
        _remainingMillis.value = (endAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        _isTimerRunning.value = false
        FocusBlockerPreferences.setFocusActive(getApplication(), false)
        timerJob?.cancel()
    }

    fun resetFocus() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        FocusBlockerPreferences.setFocusActive(getApplication(), false)
        _remainingMillis.value = _selectedMinutes.value * 60_000L
        _lastSessionXp.value = null
    }

    fun dismissSessionReward() {
        _lastSessionXp.value = null
    }

    override fun onCleared() {
        FocusBlockerPreferences.setFocusActive(getApplication(), false)
        timerJob?.cancel()
        super.onCleared()
    }
}
