package com.focusquest.app.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusquest.app.data.TaskEntity
import com.focusquest.app.permissions.PermissionHelper
import com.focusquest.app.viewmodel.FocusViewModel


private enum class Page(val label: String) {
    HOME("Início"), TASKS("Tarefas"), JOURNEY("Jornada"), PROFILE("Perfil")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusQuestApp(vm: FocusViewModel) {
    var page by remember { mutableStateOf(Page.HOME) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val xp by vm.totalXp.collectAsStateWithLifecycle()

    fun goBack() {
        page = when (page) {
            Page.HOME -> Page.HOME
            Page.TASKS, Page.JOURNEY, Page.PROFILE -> Page.HOME
        }
    }

    BackHandler(enabled = page != Page.HOME || showCreateMenu) {
        if (showCreateMenu) showCreateMenu = false else goBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF171A22),
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                NavigationBarItem(
                    selected = page == Page.HOME,
                    onClick = { page = Page.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                    label = { Text("Início", maxLines = 1) }
                )
                NavigationBarItem(
                    selected = page == Page.TASKS,
                    onClick = { page = Page.TASKS },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "Tarefas") },
                    label = { Text("Tarefas", maxLines = 1) }
                )
                // A posição central é uma ação, não uma aba de destino.
                NavigationBarItem(
                    selected = false,
                    onClick = { showCreateMenu = true },
                    icon = {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "Criar", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                            }
                        }
                    },
                    label = { Text("Criar", maxLines = 1, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = page == Page.JOURNEY,
                    onClick = { page = Page.JOURNEY },
                    icon = { Icon(Icons.Default.SportsMartialArts, contentDescription = "Jornada") },
                    label = { Text("Jornada", maxLines = 1) }
                )
                NavigationBarItem(
                    selected = page == Page.PROFILE,
                    onClick = { page = Page.PROFILE },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil", maxLines = 1) }
                )
            }
        }
    ) { padding ->
        // Gesto de borda: arrastar da esquerda para a direita retorna à tela anterior.
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(page, showCreateMenu) {
                    var startX = 0f
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            startX = offset.x
                            totalDrag = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (startX <= 36.dp.toPx() && dragAmount > 0f) {
                                totalDrag += dragAmount
                                if (totalDrag > 90.dp.toPx()) {
                                    if (showCreateMenu) showCreateMenu = false else goBack()
                                    totalDrag = 0f
                                }
                            }
                            change.consume()
                        },
                        onDragEnd = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f }
                    )
                }
        ) {
            when (page) {
                Page.HOME -> DashboardScreen(tasks, xp, Modifier.fillMaxSize(), onNavigate = { page = it }, onComplete = { vm.completeTask(it) }, onRefresh = { vm.refreshTasks() })
                Page.TASKS -> TasksScreen(tasks, vm, Modifier.fillMaxSize())
                Page.JOURNEY -> FocusScreen(vm, xp, Modifier.fillMaxSize())
                Page.PROFILE -> ProfileScreen(xp, Modifier.fillMaxSize())
            }
        }
    }

    if (showCreateMenu) {
        ModalBottomSheet(onDismissRequest = { showCreateMenu = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("O que vamos iniciar?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                ListItem(
                    headlineContent = { Text("Nova tarefa") },
                    supportingContent = { Text("Transforme uma atividade em missão") },
                    leadingContent = { Icon(Icons.Default.Checklist, null) },
                    modifier = Modifier.clickable { showCreateMenu = false; showAddTask = true }
                )
                ListItem(
                    headlineContent = { Text("Sessão de foco") },
                    supportingContent = { Text("Comece uma sessão e ganhe experiência") },
                    leadingContent = { Icon(Icons.Default.Timer, null) },
                    modifier = Modifier.clickable { showCreateMenu = false; page = Page.JOURNEY }
                )
                ListItem(
                    headlineContent = { Text("Ver tarefas") },
                    supportingContent = { Text("Organize suas missões cadastradas") },
                    leadingContent = { Icon(Icons.Default.Flag, null) },
                    modifier = Modifier.clickable { showCreateMenu = false; page = Page.TASKS }
                )
            }
        }
    }

    if (showAddTask) {
        AddTaskDialog(onDismiss = { showAddTask = false }, onSave = { title, desc, priority ->
            vm.addTask(title, desc, priority)
            showAddTask = false
        })
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    tasks: List<TaskEntity>,
    xp: Int,
    modifier: Modifier = Modifier,
    onNavigate: (Page) -> Unit,
    onComplete: (TaskEntity) -> Unit,
    onRefresh: suspend () -> Unit
) {
    val done = tasks.count { it.isCompleted }
    val pending = tasks.count { !it.isCompleted }
    val level = (xp / 500) + 1
    val progress = (xp % 500) / 500f

    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                onRefresh()
                refreshing = false
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("FOCUS QUEST", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("Seu progresso começa hoje.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Organize. Concentre-se. Evolua.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("NÍVEL $level", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("$xp XP acumulados", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Text("${500 - (xp % 500)} XP para o próximo nível", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Pendentes", pending.toString(), Modifier.weight(1f))
                MetricCard("Concluídas", done.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Missões recentes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { onNavigate(Page.TASKS) }) { Text("Ver todas") }
            }
        }
        if (tasks.isEmpty()) {
            item { EmptyState("Nenhuma missão cadastrada", "Adicione sua primeira tarefa para começar a evoluir.") }
        } else {
            items(tasks.take(4), key = { it.id }) { task -> TaskRow(task, onComplete = { onComplete(task) }) }
        }
    }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksScreen(tasks: List<TaskEntity>, vm: FocusViewModel, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SUAS MISSÕES", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("Tarefas", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
            FilledTonalIconButton(onClick = { showDialog = true }) { Icon(Icons.Default.Add, "Adicionar tarefa") }
        }
        Spacer(Modifier.height(16.dp))
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    vm.refreshTasks()
                    refreshing = false
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    EmptyState("Sua lista está vazia", "Puxe a tela para atualizar ou crie uma missão.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskRow(task, onComplete = { vm.completeTask(task) }, onDelete = { vm.deleteTask(task) })
                    }
                }
            }
        }
    }
    if (showDialog) AddTaskDialog(onDismiss = { showDialog = false }, onSave = { title, desc, priority ->
        vm.addTask(title, desc, priority)
        showDialog = false
    })
}

@Composable
private fun TaskRow(task: TaskEntity, onComplete: () -> Unit, onDelete: () -> Unit = {}) {
    val priorityColor = when (task.priority) {
        "Crítica" -> Color(0xFFFF626D)
        "Alta" -> Color(0xFFFFB454)
        "Média" -> Color(0xFF73B7FF)
        else -> Color(0xFF9AA0AE)
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!task.isCompleted) {
                Checkbox(checked = false, onCheckedChange = { onComplete() })
            } else {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.SemiBold, color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                Text("${task.priority} • ${task.xp} XP", color = priorityColor, style = MaterialTheme.typography.labelSmall)
                if (task.description.isNotBlank()) Text(task.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (!task.isCompleted) IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Excluir", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Média") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova missão") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Nome da atividade") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Descrição (opcional)") })
                Text("Prioridade", fontWeight = FontWeight.SemiBold)
                listOf("Crítica", "Alta", "Média", "Baixa").forEach {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = priority == it, onClick = { priority = it })
                        Text(it)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(title, description, priority) }, enabled = title.isNotBlank()) { Text("Salvar missão") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}


@Composable
private fun FocusScreen(vm: FocusViewModel, totalXp: Int, modifier: Modifier = Modifier) {
    val selectedMinutes by vm.selectedMinutes.collectAsStateWithLifecycle()
    val remainingMillis by vm.remainingMillis.collectAsStateWithLifecycle()
    val isRunning by vm.isTimerRunning.collectAsStateWithLifecycle()
    val rewardXp by vm.lastSessionXp.collectAsStateWithLifecycle()

    val totalMillis = selectedMinutes * 60_000L
    val progress = (1f - remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    val totalSeconds = (remainingMillis / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    val level = totalXp / 500 + 1
    val hero = when {
        level >= 20 -> "🧙"
        level >= 10 -> "🛡️"
        level >= 5 -> "⚔️"
        else -> "🧑‍🚀"
    }
    val heroTitle = when {
        level >= 20 -> "Mestre da Jornada"
        level >= 10 -> "Guardião do Conhecimento"
        level >= 5 -> "Aventureiro Experiente"
        else -> "Aprendiz de Aventureiro"
    }
    val questStage = when {
        progress >= 0.90f -> "Missão quase concluída"
        progress >= 0.60f -> "Você está avançando na missão"
        progress >= 0.30f -> "A jornada está em andamento"
        progress > 0f -> "Sua aventura começou"
        else -> "Prepare-se para sua próxima missão"
    }

    Column(
        modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(22.dp))
        Text("CENTRAL DE AVENTURAS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("Evolua a cada missão.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Concentre-se, conquiste XP e fortaleça seu personagem.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(230.dp)) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 9.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(hero, style = MaterialTheme.typography.displayLarge)
                        Text(heroTitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(String.format("%02d:%02d", minutes, seconds), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text(if (isRunning) "EM CONCENTRAÇÃO" else "TEMPO DE FOCO", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Text(questStage, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (isRunning) "Mantenha a concentração para concluir esta missão."
                    else "Escolha uma duração e inicie sua próxima aventura.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45, 60).forEach { value ->
                        FilterChip(
                            selected = selectedMinutes == value,
                            onClick = { vm.selectFocusDuration(value) },
                            enabled = !isRunning,
                            label = { Text("${value}m") }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { if (isRunning) vm.pauseFocus() else vm.startOrResumeFocus() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isRunning) "Pausar" else if (progress > 0f) "Continuar" else "Começar")
                    }
                    OutlinedIconButton(
                        onClick = { vm.resetFocus() },
                        enabled = !isRunning || progress > 0f,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reiniciar sessão")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✨", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Recompensa da missão", fontWeight = FontWeight.SemiBold)
                    Text("${selectedMinutes * 2} XP ao concluir • 2 XP por minuto", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Nesta versão, mantenha o app aberto durante a sessão.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }

    if (rewardXp != null) {
        AlertDialog(
            onDismissRequest = { vm.dismissSessionReward() },
            icon = { Text("🏆", style = MaterialTheme.typography.displayMedium) },
            title = { Text("Missão concluída!") },
            text = { Text("Você concluiu sua sessão de foco e conquistou ${rewardXp} XP para evoluir na Jornada.") },
            confirmButton = { Button(onClick = { vm.dismissSessionReward() }) { Text("Continuar") } }
        )
    }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (granted) Icons.Default.CheckCircle else Icons.Default.Lock, null, tint = if (granted) Color(0xFF65D69A) else MaterialTheme.colorScheme.primary)
            Text(title, modifier = Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.Medium)
            Text(if (granted) "Concedido" else "Configurar", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ProfileScreen(xp: Int, modifier: Modifier = Modifier) {
    val level = xp / 500 + 1
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("SUA JORNADA", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text("Perfil", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(58.dp))
                Text("Explorador", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Nível $level • $xp XP", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Sua conta e ferramentas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        listOf(
            Triple(Icons.Default.QueryStats, "Estatísticas", "Seu desempenho e evolução"),
            Triple(Icons.Default.History, "Histórico", "Sessões de foco e tarefas concluídas"),
            Triple(Icons.Default.Notifications, "Notificações", "Lembretes e avisos"),
            Triple(Icons.Default.Settings, "Configurações", "Preferências, aparência e bloqueio de apps")
        ).forEach { (icon, title, subtitle) ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                ListItem(
                    headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(subtitle) },
                    leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Assignment, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
