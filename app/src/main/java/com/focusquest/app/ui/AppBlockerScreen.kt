package com.focusquest.app.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
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
import com.focusquest.app.viewmodel.FocusViewModel

private data class LaunchableApp(val label: String, val packageName: String)

@Composable
fun AppBlockerScreen(vm: FocusViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isTimerRunning by vm.isTimerRunning.collectAsState()
    var serviceEnabled by remember { mutableStateOf(FocusBlockerPreferences.isAccessibilityServiceEnabled(context)) }
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
                serviceEnabled = FocusBlockerPreferences.isAccessibilityServiceEnabled(context)
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

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (serviceEnabled) "Serviço de acessibilidade ativado" else "Permissão necessária",
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    if (serviceEnabled)
                        "O Focus Quest pode identificar os aplicativos selecionados enquanto uma sessão de foco estiver ativa."
                    else
                        "Ative o serviço do Focus Quest nas configurações de Acessibilidade do Android para permitir o bloqueio.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (serviceEnabled) "Abrir configurações de acessibilidade" else "Ativar acessibilidade")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (isTimerRunning) "Sessão de foco em andamento" else "Bloqueio aguardando uma sessão",
                    fontWeight = FontWeight.Bold,
                    color = if (isTimerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Os aplicativos selecionados só serão bloqueados enquanto o cronômetro de foco estiver ativo. Pausar ou encerrar a sessão libera os aplicativos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text("Escolha os aplicativos que deseja bloquear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                                    } else {
                                        selectedPackages.remove(app.packageName)
                                    }
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
