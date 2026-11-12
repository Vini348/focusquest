package com.focusquest.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusquest.app.data.*
import com.focusquest.app.viewmodel.FocusViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class PlannerTab(val label: String) { TODAY("Hoje"), GOALS("Metas"), HABITS("Hábitos"), NOTES("Notas"), CALENDAR("Calendário") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductivityHubScreen(vm: FocusViewModel, onBack: () -> Unit) {
    var tab by remember { mutableStateOf(PlannerTab.TODAY) }
    val goals by vm.goals.collectAsStateWithLifecycle()
    val habits by vm.habits.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    var showGoalDialog by remember { mutableStateOf(false) }
    var showHabitDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showNewNote by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Column { Text("PLANEJAMENTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Text("Organize sua jornada", fontWeight = FontWeight.Bold) } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } },
                actions = {
                    if (tab == PlannerTab.GOALS || tab == PlannerTab.HABITS || tab == PlannerTab.NOTES) {
                        IconButton(onClick = { when(tab) { PlannerTab.GOALS -> showGoalDialog = true; PlannerTab.HABITS -> showHabitDialog = true; PlannerTab.NOTES -> { editingNote = null; showNewNote = true }; else -> Unit } }) { Icon(Icons.Default.Add, "Adicionar") }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 16.dp) {
                PlannerTab.entries.forEach { item -> Tab(selected = tab == item, onClick = { tab = item }, text = { Text(item.label) }) }
            }
            when (tab) {
                PlannerTab.TODAY -> TodayContent(tasks)
                PlannerTab.GOALS -> GoalsContent(goals, tasks, vm, onAdd = { showGoalDialog = true })
                PlannerTab.HABITS -> HabitsContent(habits, vm, onAdd = { showHabitDialog = true })
                PlannerTab.NOTES -> NotesContent(notes, onAdd = { editingNote = null; showNewNote = true }, onEdit = { editingNote = it }, onDelete = vm::deleteNote)
                PlannerTab.CALENDAR -> CalendarContent(tasks)
            }
        }
    }

    if (showGoalDialog) GoalDialog(onDismiss = { showGoalDialog = false }, onSave = { title, desc, horizon, date -> vm.addGoal(title, desc, horizon, date); showGoalDialog = false })
    if (showHabitDialog) HabitDialog(onDismiss = { showHabitDialog = false }, onSave = { title, frequency -> vm.addHabit(title, frequency); showHabitDialog = false })
    if (showNewNote || editingNote != null) {
        NoteDialog(note = editingNote, onDismiss = { showNewNote = false; editingNote = null }, onSave = { id, title, content -> vm.saveNote(id, title, content); showNewNote = false; editingNote = null })
    }
}


@Composable
private fun TodayContent(tasks: List<TaskEntity>) {
    val today = LocalDate.now().toString()
    val overdue = tasks.filter { !it.isCompleted && it.dueDate != null && it.dueDate < today }
    val dueToday = tasks.filter { !it.isCompleted && it.dueDate == today }
    val priorityWithoutDate = tasks.filter { !it.isCompleted && it.dueDate == null && (it.priority == "Crítica" || it.priority == "Alta") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PlannerIntro("Seu plano para hoje", "Uma visão rápida do que merece sua atenção agora.", "☀️") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { PlannerMetricCard("Para hoje", dueToday.size.toString(), Modifier.weight(1f)); PlannerMetricCard("Atrasadas", overdue.size.toString(), Modifier.weight(1f)) } }
        item { Text("Prazos de hoje", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (dueToday.isEmpty()) item { Text("Nenhuma tarefa com prazo para hoje.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        items(dueToday, key = { "today-${it.id}" }) { task -> PlannerTaskLine(task) }
        if (overdue.isNotEmpty()) {
            item { Text("Precisam de atenção", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFF8A65)) }
            items(overdue, key = { "late-${it.id}" }) { task -> PlannerTaskLine(task) }
        }
        if (priorityWithoutDate.isNotEmpty()) {
            item { Text("Prioridades sem prazo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(priorityWithoutDate, key = { "priority-${it.id}" }) { task -> PlannerTaskLine(task) }
        }
        if (dueToday.isEmpty() && overdue.isEmpty() && priorityWithoutDate.isEmpty()) item { EmptyPlannerCard("Dia livre de pendências cadastradas", "Adicione prazos e prioridades às suas missões para montar seu plano diário.", "", {}) }
    }
}


@Composable
private fun PlannerMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) { Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun PlannerTaskLine(task: TaskEntity) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        ListItem(headlineContent = { Text(task.title, fontWeight = FontWeight.SemiBold) }, supportingContent = { Text("${task.category} • ${task.priority}${task.dueDate?.let { " • $it" } ?: ""}") }, leadingContent = { Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.primary) })
    }
}

@Composable
private fun GoalsContent(goals: List<GoalEntity>, tasks: List<TaskEntity>, vm: FocusViewModel, onAdd: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            PlannerIntro("Seus objetivos", "Transforme planos em conquistas. Defina o que deseja alcançar e acompanhe sua evolução.", "🎯")
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Metas ativas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${goals.count { !it.isCompleted }} em andamento", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (goals.isEmpty()) item { EmptyPlannerCard("Nenhuma meta criada", "Comece com um objetivo que seja importante para você.", "Criar primeira meta", onAdd) }
        items(goals, key = { it.id }) { goal ->
            val linked = tasks.filter { it.goalId == goal.id }
            val done = linked.count { it.isCompleted }
            val progress = if (linked.isEmpty()) 0f else done.toFloat() / linked.size
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(goal.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(goal.horizon + (goal.targetDate?.let { " • Até $it" } ?: ""), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        }
                        Checkbox(checked = goal.isCompleted, onCheckedChange = { vm.completeGoal(goal.id, it) })
                    }
                    if (goal.description.isNotBlank()) Text(goal.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                    Text(if (linked.isEmpty()) "Vincule tarefas a esta meta para acompanhar o progresso" else "$done de ${linked.size} tarefas concluídas", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { TextButton(onClick = { vm.deleteGoal(goal.id) }) { Icon(Icons.Default.DeleteOutline, null); Text("Excluir") } }
                }
            }
        }
    }
}

@Composable
private fun HabitsContent(habits: List<HabitEntity>, vm: FocusViewModel, onAdd: () -> Unit) {
    val today = LocalDate.now().toString()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PlannerIntro("Construa consistência", "Pequenas ações repetidas criam grandes mudanças. Marque cada hábito ao concluir.", "🌱") }
        item { Text("Seus hábitos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (habits.isEmpty()) item { EmptyPlannerCard("Nenhum hábito cadastrado", "Adicione uma atividade que deseja repetir.", "Criar primeiro hábito", onAdd) }
        items(habits, key = { it.id }) { habit ->
            val done = habit.lastCompletedDate == today
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .13f)) { Icon(Icons.Default.Repeat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp)) }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(habit.title, fontWeight = FontWeight.SemiBold)
                        Text("${habit.frequency} • 🔥 ${habit.currentStreak} dias • ${habit.totalCompletions} conclusões", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                    if (done) Icon(Icons.Default.CheckCircle, "Concluído hoje", tint = Color(0xFF65D69A))
                    else FilledTonalButton(onClick = { vm.completeHabit(habit) }) { Text("Concluir") }
                    IconButton(onClick = { vm.archiveHabit(habit.id) }) { Icon(Icons.Default.Archive, "Arquivar hábito") }
                }
            }
        }
    }
}

@Composable
private fun NotesContent(notes: List<NoteEntity>, onAdd: () -> Unit, onEdit: (NoteEntity) -> Unit, onDelete: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PlannerIntro("Central de ideias", "Guarde lembretes, pensamentos e informações importantes em um só lugar.", "📝") }
        item { OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Nova nota") } }
        if (notes.isEmpty()) item { EmptyPlannerCard("Seu espaço está vazio", "Crie uma nota rápida para não perder uma ideia.", "Criar nota", onAdd) }
        items(notes, key = { it.id }) { note ->
            Card(Modifier.fillMaxWidth().clickable { onEdit(note) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(note.title, fontWeight = FontWeight.Bold)
                        Text(note.content, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onDelete(note.id) }) { Icon(Icons.Default.DeleteOutline, "Excluir nota") }
                }
            }
        }
    }
}

@Composable
private fun CalendarContent(tasks: List<TaskEntity>) {
    val dated = tasks.filter { !it.dueDate.isNullOrBlank() }.groupBy { it.dueDate!! }.toSortedMap()
    val today = LocalDate.now().toString()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { PlannerIntro("Agenda de missões", "Acompanhe os prazos das suas tarefas. Cadastre uma data ao criar cada missão.", "🗓️") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Hoje", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy")), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${tasks.count { it.dueDate == today && !it.isCompleted }} tarefas pendentes para hoje", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (dated.isEmpty()) item { EmptyPlannerCard("Nenhum prazo definido", "Adicione datas às suas tarefas para montar sua agenda.", "", {}) }
        dated.forEach { (date, itemsForDate) ->
            item { Text(date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            items(itemsForDate, key = { it.id }) { task ->
                ListItem(
                    headlineContent = { Text(task.title, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("${task.category} • ${task.priority} • ${if (task.isCompleted) "Concluída" else "Pendente"}") },
                    leadingContent = { Icon(if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.Event, null, tint = if (task.isCompleted) Color(0xFF65D69A) else MaterialTheme.colorScheme.primary) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PlannerIntro(title: String, subtitle: String, emoji: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.displaySmall)
            Column(Modifier.padding(start = 14.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun EmptyPlannerCard(title: String, subtitle: String, action: String, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (action.isNotBlank()) TextButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun GoalDialog(onDismiss: () -> Unit, onSave: (String, String, String, String?) -> Unit) {
    var title by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var horizon by remember { mutableStateOf("Médio prazo") }; var date by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nova meta") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("O que deseja alcançar?") }, singleLine = true)
            OutlinedTextField(description, { description = it }, label = { Text("Descrição (opcional)") })
            Text("Horizonte", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("Curto prazo", "Médio prazo", "Longo prazo").forEach { FilterChip(selected = horizon == it, onClick = { horizon = it }, label = { Text(it, style = MaterialTheme.typography.labelSmall) }) } }
            OutlinedTextField(date, { date = it }, label = { Text("Prazo (AAAA-MM-DD, opcional)") }, singleLine = true)
        }
    }, confirmButton = { Button(enabled = title.isNotBlank() && (date.isBlank() || runCatching { LocalDate.parse(date) }.isSuccess), onClick = { onSave(title, description, horizon, date.takeIf { it.isNotBlank() }) }) { Text("Salvar meta") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun HabitDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }; var frequency by remember { mutableStateOf("Diário") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Novo hábito") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Nome do hábito") }, singleLine = true)
            Text("Frequência", fontWeight = FontWeight.SemiBold)
            listOf("Diário", "Dias úteis", "Semanal").forEach { Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = frequency == it, onClick = { frequency = it }); Text(it) } }
        }
    }, confirmButton = { Button(enabled = title.isNotBlank(), onClick = { onSave(title, frequency) }) { Text("Criar hábito") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun NoteDialog(note: NoteEntity?, onDismiss: () -> Unit, onSave: (Long?, String, String) -> Unit) {
    var title by remember(note) { mutableStateOf(note?.title ?: "") }; var content by remember(note) { mutableStateOf(note?.content ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (note == null) "Nova nota" else "Editar nota") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true)
            OutlinedTextField(content, { content = it }, label = { Text("Escreva sua nota") }, minLines = 4)
        }
    }, confirmButton = { Button(enabled = title.isNotBlank(), onClick = { onSave(note?.id, title, content) }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
