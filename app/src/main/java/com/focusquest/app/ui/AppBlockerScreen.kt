package com.focusquest.app.ui

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.focusquest.app.blocker.FocusBlockerPreferences
import com.focusquest.app.permissions.PermissionHelper
import com.focusquest.app.viewmodel.FocusViewModel

private data class LaunchableApp(val label: String, val packageName: String)

@Composable
fun AppBlockerScreen(vm: FocusViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isTimerRunning by vm.isTimerRunning.collectAsState()
    var usageEnabled by remember { mutableStateOf(PermissionHelper.hasUsageAccess(context)) }
    var overlayEnabled by remember { mutableStateOf(PermissionHelper.hasOverlayAccess(context)) }
    val selectedPackages = remember {
        mutableStateListOf<String>().apply {
            addAll(FocusBlockerPreferences.getBlockedPackages(context))
        }
    }
    val apps = remember {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null
                LaunchableApp(
                    label = info.loadLabel(context.packageManager)?.toString() ?: packageName,
                    packageName = packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageEnabled = PermissionHelper.hasUsageAccess(context)
                overlayEnabled = PermissionHelper.hasOverlayAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun persistSelection() {
        FocusBlockerPreferences.setBlockedPackages(context, selectedPackages.toSet())
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
            Column(Modifier.weight(1f)) {
                Text("PROTEÇÃO DE FOCO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text("Bloqueio de aplicativos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Privacidade em primeiro lugar", fontWeight = FontWeight.Bold)
                }
                Text(
                    "O Focus Quest usa o Acesso ao uso para identificar somente qual aplicativo está em primeiro plano. Ele não lê mensagens, senhas, textos, telas ou notificações e não envia esse histórico para servidores.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "A permissão de sobreposição é usada apenas para mostrar a tela de foco sobre um aplicativo selecionado.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Acesso ao uso", fontWeight = FontWeight.Bold)
                Text(
                    if (usageEnabled) "Permissão ativada" else "Permissão ainda não concedida",
                    color = if (usageEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text("Necessário para detectar qual aplicativo está aberto durante a sessão de foco.", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = { context.startActivity(PermissionHelper.usageAccessIntent(context)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (usageEnabled) "Revisar acesso ao uso" else "Conceder acesso ao uso") }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Sobreposição", fontWeight = FontWeight.Bold)
                Text(
                    if (overlayEnabled) "Permissão ativada" else "Permissão ainda não concedida",
                    color = if (overlayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text("Necessária para exibir a tela de bloqueio por cima do aplicativo selecionado.", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = { context.startActivity(PermissionHelper.overlayIntent(context)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (overlayEnabled) "Revisar sobreposição" else "Permitir sobreposição") }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (isTimerRunning) "Sessão de foco em andamento" else "Bloqueio aguardando uma sessão",
                    fontWeight = FontWeight.Bold,
                    color = if (isTimerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "O monitoramento funciona somente durante uma sessão iniciada por você. Pausar ou encerrar a sessão interrompe o monitoramento e remove a tela de bloqueio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!usageEnabled || !overlayEnabled) {
                    Text("Para bloquear aplicativos, conceda as duas permissões acima.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text("Aplicativos que deseja bloquear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("${selectedPackages.size} selecionado(s)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)

        if (apps.isEmpty()) {
            Text("Não foi possível listar os aplicativos instalados neste dispositivo.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    val checked = app.packageName in selectedPackages
                    ListItem(
                        headlineContent = { Text(app.label, maxLines = 1) },
                        supportingContent = { Text(app.packageName, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        trailingContent = {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { value ->
                                    if (value) {
                                        if (app.packageName !in selectedPackages) selectedPackages.add(app.packageName)
                                    } else selectedPackages.remove(app.packageName)
                                    persistSelection()
                                }
                            )
                        },
                        modifier = Modifier.clickable {
                            if (checked) selectedPackages.remove(app.packageName)
                            else selectedPackages.add(app.packageName)
                            persistSelection()
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
