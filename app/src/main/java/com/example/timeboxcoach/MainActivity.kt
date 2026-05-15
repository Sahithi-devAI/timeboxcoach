package com.example.timeboxcoach

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.timeboxcoach.ui.theme.TimeBoxCoachTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimeBoxCoachTheme {
                TimeBoxCoachApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeBoxCoachApp() {
    val context = LocalContext.current
    var showAddScreen by remember { mutableStateOf(false) }
    var showStatsScreen by remember { mutableStateOf(false) }
    var rules by remember { mutableStateOf(BlockRulesManager.getRules(context)) }

    BackHandler(enabled = showAddScreen || showStatsScreen) {
        showAddScreen = false
        showStatsScreen = false
    }

    when {
        showAddScreen -> AddRuleScreen(
            onSave = { rule ->
                BlockRulesManager.addRule(context, rule)
                rules = BlockRulesManager.getRules(context)
                showAddScreen = false
            },
            onBack = { showAddScreen = false }
        )
        showStatsScreen -> StatsScreen(onBack = { showStatsScreen = false })
        else -> RulesListScreen(
            rules = rules,
            onAddClick = { showAddScreen = true },
            onDelete = { pkg ->
                BlockRulesManager.removeRule(context, pkg)
                rules = BlockRulesManager.getRules(context)
            },
            onStatsClick = { showStatsScreen = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesListScreen(
    rules: List<BlockRule>,
    onAddClick: () -> Unit,
    onDelete: (String) -> Unit,
    onStatsClick: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TimeBox Coach") },
                actions = {
                    TextButton(onClick = onStatsClick) { Text("Stats") }
                    IconButton(onClick = { shareLogs(context) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share logs")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add rule")
            }
        }
    ) { padding ->
        if (rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No blocked apps yet.\nTap + to add one.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(rules, key = { it.packageName }) { rule ->
                    ListItem(
                        headlineContent = { Text(rule.appName) },
                        supportingContent = {
                            Text("Allowed: ${rule.allowedStart}–${rule.allowedEnd}")
                        },
                        trailingContent = {
                            IconButton(onClick = { onDelete(rule.packageName) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

// ── Stats Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val stats by produceState<StatsParser.Stats?>(null) {
        value = withContext(Dispatchers.IO) { StatsParser.parse(context) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Patterns") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Back") }
                }
            )
        }
    ) { padding ->
        if (stats == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            StatsContent(stats = stats!!, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
fun StatsContent(stats: StatsParser.Stats, modifier: Modifier = Modifier) {
    val total = stats.thisWeekTotal
    val prev = stats.lastWeekTotal

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Hero
        item {
            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "This week",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "$total impulse opens",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (prev > 0) {
                        val diff = total - prev
                        val (color, text) = when {
                            diff > 0 -> Color(0xFFC0392B) to "▲ $diff more than last week ($prev)"
                            diff < 0 -> Color(0xFF27AE60) to "▼ ${-diff} fewer than last week ($prev)"
                            else -> Color.Gray to "Same as last week ($prev)"
                        }
                        Text(text, fontSize = 13.sp, color = color, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // By app
        if (stats.byApp.isNotEmpty()) {
            item {
                SectionTitle("By App")
                stats.byApp.forEach { (app, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(app, style = MaterialTheme.typography.bodyMedium)
                        Text("$count", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        // Time of day
        item {
            SectionTitle("When You Reached for Your Phone")
            val timeSlots = listOf(
                "Morning  6am–12pm" to stats.morning,
                "Afternoon 12pm–5pm" to stats.afternoon,
                "Evening  5pm–9pm" to stats.evening,
                "Night    9pm–6am" to stats.night
            )
            timeSlots.forEach { (label, count) ->
                TimeBar(label = label, count = count, total = total)
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(24.dp))
        }

        // Recent reasons
        if (stats.recentEntries.isNotEmpty()) {
            item { SectionTitle("What You Told Yourself") }
            items(stats.recentEntries) { entry ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Row {
                        Text(
                            StatsParser.formatTimestamp(entry.timestamp),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.width(90.dp)
                        )
                        Text(
                            entry.appName,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Text(
                        entry.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider(color = Color(0xFFEEEEEE))
            }
            item { Spacer(Modifier.height(24.dp)) }
        } else {
            item {
                Text(
                    "No opens recorded this week.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun TimeBar(label: String, count: Int, total: Int) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, color = Color.Gray)
            Text("$count", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(5.dp))
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(Color(0xFF4285F4), RoundedCornerShape(5.dp))
                )
            }
        }
    }
}

// ── Existing screens (unchanged) ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleScreen(onSave: (BlockRule) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<Pair<String, String>?>(null) }
    var startTime by remember { mutableStateOf("17:00") }
    var endTime by remember { mutableStateOf("17:30") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedApp != null) { selectedApp = null }

    val installedApps by produceState<List<Pair<String, String>>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                pm.queryIntentActivities(intent, 0)
            }
            activities
                .map { it.activityInfo }
                .filter { it.packageName != context.packageName }
                .map { info -> Pair(info.packageName, info.loadLabel(pm).toString()) }
                .distinctBy { it.first }
                .sortedBy { it.second }
        }
    }

    val filtered = if (searchQuery.isBlank()) installedApps
    else installedApps.filter {
        it.second.contains(searchQuery, ignoreCase = true) ||
                it.first.contains(searchQuery, ignoreCase = true)
    }

    if (showStartPicker) {
        ShowTimePicker(
            initialTime = startTime,
            onTimeSet = { startTime = it; showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        ShowTimePicker(
            initialTime = endTime,
            onTimeSet = { endTime = it; showEndPicker = false },
            onDismiss = { showEndPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedApp == null) "Pick an App" else "Set Time Window") },
                navigationIcon = {
                    TextButton(
                        onClick = if (selectedApp != null) ({ selectedApp = null }) else onBack
                    ) {
                        Text(if (selectedApp != null) "← Apps" else "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (selectedApp == null) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search apps") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                if (installedApps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn {
                        items(filtered, key = { it.first }) { (pkg, name) ->
                            ListItem(
                                headlineContent = { Text(name) },
                                supportingContent = {
                                    Text(pkg, style = MaterialTheme.typography.bodySmall)
                                },
                                modifier = Modifier.clickable { selectedApp = Pair(pkg, name) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            } else {
                val (pkg, name) = selectedApp!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(name, style = MaterialTheme.typography.titleLarge)
                    Text(pkg, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(32.dp))
                    Text("Allowed time window", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showStartPicker = true }) {
                            Text("Start: $startTime")
                        }
                        OutlinedButton(onClick = { showEndPicker = true }) {
                            Text("End: $endTime")
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { onSave(BlockRule(pkg, name, startTime, endTime)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Rule")
                    }
                }
            }
        }
    }
}

fun shareLogs(context: Context) {
    val file = File(context.filesDir, "reason_logs.json")
    if (!file.exists()) {
        Toast.makeText(context, "No logs yet", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share usage logs"))
}

@Composable
fun ShowTimePicker(initialTime: String, onTimeSet: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val parts = initialTime.split(":").map { it.toIntOrNull() ?: 0 }
    DisposableEffect(Unit) {
        val dialog = TimePickerDialog(
            context,
            { _, hour, minute -> onTimeSet("%02d:%02d".format(hour, minute)) },
            parts.getOrElse(0) { 0 },
            parts.getOrElse(1) { 0 },
            true
        )
        dialog.setOnDismissListener { onDismiss() }
        dialog.show()
        onDispose { if (dialog.isShowing) dialog.dismiss() }
    }
}
