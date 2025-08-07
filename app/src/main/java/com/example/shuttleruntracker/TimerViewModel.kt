package com.example.shuttleruntracker // Or your package name

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val runDao = AppDatabase.getDatabase(application).runDao()
    private val locationService = LocationService(application)

    private val _time = MutableStateFlow(0L)
    val time: StateFlow<Long> = _time.asStateFlow()

    // --- USE THE NEW ENUM FOR STATE ---
    private val _timerState = MutableStateFlow(TimerState.STOPPED)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _shuttles = MutableStateFlow(0)
    val shuttles: StateFlow<Int> = _shuttles.asStateFlow()

    private val _distance = MutableStateFlow(0.0)
    val distance: StateFlow<Double> = _distance.asStateFlow()

    val runHistory: StateFlow<List<Run>> = runDao.getAllRuns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var timerJob: Job? = null
    private var locationJob: Job? = null
    private var lastLocation: Location? = null

    fun deleteRun(run: Run) {
        viewModelScope.launch {
            runDao.deleteRun(run)
        }
    }

    fun handleStartPauseResume() {
        when (_timerState.value) {
            TimerState.STOPPED -> startTimerAndLocation()
            TimerState.RUNNING -> pauseTimerAndLocation()
            TimerState.PAUSED -> resumeTimerAndLocation()
        }
    }

    fun handleFinish() {
        stopAndSaveRun()
        resetTimer()
    }

    private fun startTimerAndLocation() {
        _timerState.value = TimerState.RUNNING
        lastLocation = null

        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis() - _time.value
            while (_timerState.value == TimerState.RUNNING) {
                _time.value = System.currentTimeMillis() - startTime
                delay(10)
            }
        }
        startLocationUpdates()
    }

    private fun pauseTimerAndLocation() {
        _timerState.value = TimerState.PAUSED
        locationJob?.cancel()
    }

    private fun resumeTimerAndLocation() {
        _timerState.value = TimerState.RUNNING
        lastLocation = null // Prevent calculating distance from paused location

        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis() - _time.value
            while (_timerState.value == TimerState.RUNNING) {
                _time.value = System.currentTimeMillis() - startTime
                delay(10)
            }
        }
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        locationJob = locationService.requestLocationUpdates()
            .onEach { newLocation ->
                if (_timerState.value == TimerState.RUNNING) {
                    lastLocation?.let {
                        _distance.value += it.distanceTo(newLocation)
                    }
                    lastLocation = newLocation
                }
            }
            .launchIn(viewModelScope)
    }

    private fun stopAndSaveRun() {
        if (_time.value > 0) {
            val newRun = Run(
                date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                duration = formatTime(_time.value),
                shuttles = _shuttles.value,
                distanceInMeters = _distance.value
            )
            viewModelScope.launch {
                runDao.insertRun(newRun)
            }
        }
    }

    private fun resetTimer() {
        _timerState.value = TimerState.STOPPED
        timerJob?.cancel()
        locationJob?.cancel()
        _time.value = 0L
        _shuttles.value = 0
        _distance.value = 0.0
    }

    fun handleReset() {
        resetTimer()
    }

    fun handleShuttleIncrement() {
        if (_timerState.value == TimerState.RUNNING) {
            _shuttles.value += 1
        }
    }

    fun formatTime(timeInMillis: Long): String {
        val minutes = (timeInMillis / 60000)
        val seconds = (timeInMillis % 60000) / 1000
        val milliseconds = (timeInMillis % 1000) / 10
        return String.format("%02d:%02d:%02d", minutes, seconds, milliseconds)
    }
}