package com.focusquest.app.data

class TaskRepository(private val dao: TaskDao) {
    val tasks = dao.observeAll()
    val totalXp = dao.observeTotalXp()

    suspend fun refresh() { dao.getAllOnce() }

    suspend fun add(title: String, description: String, priority: String) {
        val xp = when (priority) {
            "Crítica" -> 100
            "Alta" -> 50
            "Média" -> 25
            else -> 10
        }
        dao.insert(TaskEntity(title = title, description = description, priority = priority, xp = xp))
    }

    suspend fun complete(id: Long) {
        dao.complete(id, System.currentTimeMillis())
    }

    suspend fun addFocusSession(minutes: Int) {
        if (minutes <= 0) return
        dao.insertFocusSession(FocusSessionEntity(durationMinutes = minutes, xp = minutes * 2))
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
