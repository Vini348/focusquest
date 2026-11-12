package com.focusquest.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focusquest.app.data.FocusDatabase
import com.focusquest.app.data.TaskEntity
import com.focusquest.app.data.TaskRepository
import com.focusquest.app.blocker.FocusBlockerPreferences
import com.focusquest.app.blocker.UsageAccessBlockerService
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
    val goals = repository.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val habits = repository.habits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notes = repository.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun addTask(title: String, description: String, priority: String, category: String = "Pessoal", dueDate: String? = null, goalId: Long? = null) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.add(title.trim(), description.trim(), priority, category, dueDate, goalId) }
    }

    fun addGoal(title: String, description: String, horizon: String, targetDate: String?) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.addGoal(title.trim(), description.trim(), horizon, targetDate) }
    }
    fun completeGoal(id: Long, completed: Boolean) { viewModelScope.launch { repository.completeGoal(id, completed) } }
    fun deleteGoal(id: Long) { viewModelScope.launch { repository.deleteGoal(id) } }
    fun addHabit(title: String, frequency: String) { if (title.isNotBlank()) viewModelScope.launch { repository.addHabit(title.trim(), frequency) } }
    fun completeHabit(habit: com.focusquest.app.data.HabitEntity) { viewModelScope.launch { repository.completeHabit(habit) } }
    fun archiveHabit(id: Long) { viewModelScope.launch { repository.archiveHabit(id) } }
    fun saveNote(id: Long?, title: String, content: String) { if (title.isNotBlank()) viewModelScope.launch { repository.saveNote(id, title.trim(), content.trim()) } }
    fun deleteNote(id: Long) { viewModelScope.launch { repository.deleteNote(id) } }
    fun subtasks(taskId: Long) = repository.subtasks(taskId)
    fun addSubtask(taskId: Long, title: String) { if (title.isNotBlank()) viewModelScope.launch { repository.addSubtask(taskId, title.trim()) } }
    fun completeSubtask(id: Long, completed: Boolean) { viewModelScope.launch { repository.completeSubtask(id, completed) } }
    fun deleteSubtask(id: Long) { viewModelScope.launch { repository.deleteSubtask(id) } }

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
        UsageAccessBlockerService.start(getApplication())
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value) {
                val left = (endAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                _remainingMillis.value = left
                if (left == 0L) {
                    _isTimerRunning.value = false
                    FocusBlockerPreferences.setFocusActive(getApplication(), false)
                    UsageAccessBlockerService.stop(getApplication())
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
        UsageAccessBlockerService.stop(getApplication())
        timerJob?.cancel()
    }

    fun resetFocus() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        FocusBlockerPreferences.setFocusActive(getApplication(), false)
        UsageAccessBlockerService.stop(getApplication())
        _remainingMillis.value = _selectedMinutes.value * 60_000L
        _lastSessionXp.value = null
    }

    fun dismissSessionReward() {
        _lastSessionXp.value = null
    }

    override fun onCleared() {
        FocusBlockerPreferences.setFocusActive(getApplication(), false)
        UsageAccessBlockerService.stop(getApplication())
        UsageAccessBlockerService.stop(getApplication())
        timerJob?.cancel()
        super.onCleared()
    }
}
