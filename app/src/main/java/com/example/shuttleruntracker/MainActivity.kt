package com.example.shuttleruntracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.shuttleruntracker.ui.theme.ShuttleRunTrackerTheme
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: TimerViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                viewModel.handleStartPauseResume()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This line installs the custom splash screen
        installSplashScreen()

        setContent {
            ShuttleRunTrackerTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val timerState by viewModel.timerState.collectAsState()
                    val time by viewModel.time.collectAsState()
                    val shuttles by viewModel.shuttles.collectAsState()
                    val distance by viewModel.distance.collectAsState()
                    val runHistory by viewModel.runHistory.collectAsState()

                    ShuttleTrackerScreen(
                        timerState = timerState,
                        timeInMillis = time,
                        shuttles = shuttles,
                        distance = distance,
                        runHistory = runHistory,
                        onStartPauseResume = { checkAndRequestLocationPermission() },
                        onFinish = { viewModel.handleFinish() },
                        onReset = { viewModel.handleReset() },
                        onIncrementShuttle = { viewModel.handleShuttleIncrement() },
                        onDeleteRun = { run -> viewModel.deleteRun(run) }
                    )
                }
            }
        }
    }

    private fun checkAndRequestLocationPermission() {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocationPermission) {
            viewModel.handleStartPauseResume()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}

@Composable
fun ShuttleTrackerScreen(
    timerState: TimerState,
    timeInMillis: Long,
    shuttles: Int,
    distance: Double,
    runHistory: List<Run>,
    onStartPauseResume: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    onIncrementShuttle: () -> Unit,
    onDeleteRun: (Run) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 450.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Header()
                TimerDisplay(
                    timeInMillis = timeInMillis,
                    shuttles = shuttles,
                    distance = distance,
                    timerState = timerState,
                    onIncrementShuttle = onIncrementShuttle
                )
                ControlButtons(
                    timerState = timerState,
                    onStartPauseResume = onStartPauseResume,
                    onFinish = onFinish,
                    onReset = onReset
                )
                RunHistory(
                    history = runHistory,
                    onDeleteRun = onDeleteRun
                )
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Run") },
        text = { Text("Are you sure you want to permanently delete this run?") },
        confirmButton = {
            Button(onClick = { onConfirm(); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Delete")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDistance(meters: Double): String {
    return if (meters < 1000) {
        "${meters.toInt()} m"
    } else {
        val kilometers = meters / 1000
        "${DecimalFormat("#.##").format(kilometers)} km"
    }
}

@Composable
fun Header() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Shuttle Run Tracker", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
        Text("Build your stamina, one sprint at a time.", fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun TimerDisplay(
    timeInMillis: Long,
    shuttles: Int,
    distance: Double,
    timerState: TimerState,
    onIncrementShuttle: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60
    val milliseconds = (timeInMillis % 1000) / 10

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (timerState == TimerState.RUNNING) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIncrementShuttle()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoxWithConstraints(contentAlignment = Alignment.Center) {
                val timeStyle = TextStyle(
                    fontSize = if (maxWidth < 250.dp) 44.sp else 56.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6EE7B7)
                )
                val fontSizeValue = timeStyle.fontSize.value

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = String.format("%02d", minutes), style = timeStyle)
                    Text(text = ":", style = timeStyle.copy(fontSize = (fontSizeValue * 0.8).sp))
                    Text(text = String.format("%02d", seconds), style = timeStyle)
                    Text(text = ":", style = timeStyle.copy(fontSize = (fontSizeValue * 0.8).sp))
                    Text(text = String.format("%02d", milliseconds), style = timeStyle)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth > 250.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(label = "Shuttles", value = "$shuttles")
                        StatItem(label = "Distance", value = formatDistance(distance))
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatItem(label = "Shuttles", value = "$shuttles")
                        StatItem(label = "Distance", value = formatDistance(distance))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when (timerState) {
                    TimerState.RUNNING -> "TAP TO ADD SHUTTLE"
                    TimerState.PAUSED -> "PAUSED"
                    TimerState.STOPPED -> "TAP START TO BEGIN"
                },
                fontSize = 12.sp, color = Color.Gray, letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
fun ControlButtons(
    timerState: TimerState,
    onStartPauseResume: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit
) {
    when (timerState) {
        TimerState.STOPPED -> Button(onClick = onStartPauseResume, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) {
            Text("Start", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        TimerState.RUNNING -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onStartPauseResume, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))) {
                Text("Pause", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onFinish, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                Text("Finish", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        TimerState.PAUSED -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onStartPauseResume, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) {
                Text("Resume", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onFinish, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                Text("Finish", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    if (timerState != TimerState.STOPPED) {
        TextButton(onClick = onReset) {
            Text("Reset", color = Color.Gray)
        }
    }
}

@Composable
fun RunHistory(history: List<Run>, onDeleteRun: (Run) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var runToDelete by remember { mutableStateOf<Run?>(null) }

    if (showDialog && runToDelete != null) {
        ConfirmDeleteDialog(
            onConfirm = { onDeleteRun(runToDelete!!) },
            onDismiss = { showDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalDivider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))
        Text("Run History", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
        Spacer(modifier = Modifier.height(8.dp))
        if (history.isEmpty()) {
            Text("Your completed runs will appear here.", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp).background(Color(0xFF111827), shape = RoundedCornerShape(8.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(history) { index, run ->
                    RunHistoryItem(
                        run = run,
                        index = history.size - index,
                        onDeleteClick = {
                            runToDelete = run
                            showDialog = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RunHistoryItem(run: Run, index: Int, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Run #$index", fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                Text(run.date, fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(run.duration, fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = Color.White)
                Row {
                    Text("${run.shuttles} shuttles", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(formatDistance(run.distanceInMeters), fontSize = 12.sp, color = Color.Gray)
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Run", tint = Color.Gray)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun DefaultPreviewSmall() {
    ShuttleTrackerScreen(
        timerState = TimerState.RUNNING,
        timeInMillis = 94_400L,
        shuttles = 5,
        distance = 1234.5,
        runHistory = listOf(
            Run(1, "04/08/2025", "01:30:55", 10, 2500.0)
        ),
        onStartPauseResume = {},
        onFinish = {},
        onReset = {},
        onIncrementShuttle = {},
        onDeleteRun = {}
    )
}