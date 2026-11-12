package com.focusquest.app.data

class TaskRepository(private val dao: TaskDao) {
    val tasks = dao.observeAll()
    val totalXp = dao.observeTotalXp()

    suspend fun refresh() { dao.getAllOnce() }

    suspend fun add(title: String, description: String, priority: String, category: String = "Pessoal", dueDate: String? = null, goalId: Long? = null) {
        val xp = when (priority) {
            "Crítica" -> 100
            "Alta" -> 50
            "Média" -> 25
            else -> 10
        }
        dao.insert(TaskEntity(title = title, description = description, priority = priority, xp = xp, category = category, dueDate = dueDate, goalId = goalId))
    }

    suspend fun complete(id: Long) {
        dao.complete(id, System.currentTimeMillis())
    }

    suspend fun addFocusSession(minutes: Int) {
        if (minutes <= 0) return
        dao.insertFocusSession(FocusSessionEntity(durationMinutes = minutes, xp = minutes * 2))
    }

    suspend fun delete(id: Long) { dao.deleteSubtasksForTask(id); dao.delete(id) }

    val goals = dao.observeGoals()
    val habits = dao.observeHabits()
    val notes = dao.observeNotes()
    fun subtasks(taskId: Long) = dao.observeSubtasks(taskId)
    suspend fun addGoal(title: String, description: String, horizon: String, targetDate: String?) =
        dao.insertGoal(GoalEntity(title = title, description = description, horizon = horizon, targetDate = targetDate))
    suspend fun completeGoal(id: Long, completed: Boolean) = dao.setGoalCompleted(id, completed)
    suspend fun deleteGoal(id: Long) = dao.deleteGoal(id)
    suspend fun addHabit(title: String, frequency: String) = dao.insertHabit(HabitEntity(title = title, frequency = frequency))
    suspend fun completeHabit(habit: HabitEntity) {
        val today = java.time.LocalDate.now().toString()
        if (habit.lastCompletedDate == today) return
        val previousDate = habit.lastCompletedDate?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
        val daysSinceLast = previousDate?.let { java.time.temporal.ChronoUnit.DAYS.between(it, java.time.LocalDate.now()) }
        val continues = when (habit.frequency) {
            "Semanal" -> daysSinceLast != null && daysSinceLast in 1L..7L
            "Dias úteis" -> daysSinceLast != null && (daysSinceLast == 1L || (daysSinceLast == 3L && previousDate?.dayOfWeek == java.time.DayOfWeek.FRIDAY))
            else -> daysSinceLast == 1L
        }
        val streak = if (continues) habit.currentStreak + 1 else 1
        dao.completeHabit(habit.id, today, streak)
    }
    suspend fun archiveHabit(id: Long) = dao.archiveHabit(id)
    suspend fun saveNote(id: Long?, title: String, content: String) {
        val now = System.currentTimeMillis()
        if (id == null) dao.insertNote(NoteEntity(title = title, content = content, updatedAt = now))
        else dao.updateNote(id, title, content, now)
    }
    suspend fun deleteNote(id: Long) = dao.deleteNote(id)
    suspend fun addSubtask(taskId: Long, title: String) = dao.insertSubtask(SubtaskEntity(taskId = taskId, title = title))
    suspend fun completeSubtask(id: Long, completed: Boolean) = dao.setSubtaskCompleted(id, completed)
    suspend fun deleteSubtask(id: Long) = dao.deleteSubtask(id)
}
