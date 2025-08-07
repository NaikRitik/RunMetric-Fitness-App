# RunMetric - Fitness Shuttle Run Tracker

**RunMetric** is an Android app focused on helping footballers (and fitness enthusiasts) build stamina through shuttle‑run tracking. It features live timing, shuttle counting, distance tracking, and persistent run history.

---

##  Features

-  **Live Timer**: High-precision stopwatch for tracking shuttle run duration.
-  **Shuttle Counter**: Tap the screen to log completed shuttle runs during active timing.
-  **Distance Measurement**: Continuously calculates distance covered using GPS.
-  **Persistent Run History**: Stores run logs (date, time, shuttles, distance) locally using Room.
-  **Intuitive UI with Jetpack Compose**: Clean, modern user interface built using Compose.
-  **Permission‑Aware Flow**: Optimized to request and handle location permissions seamlessly during start.

---

##  Tech Stack

-  **Kotlin** – Language for modern Android development  
- ⏱ **Jetpack Compose** – Declarative UI toolkit  
-  **Coroutines + StateFlow** – Asynchronous timing and UI state management  
-  **Room Database** – Local storage of run data  
-  **MVVM Architecture** – Clean separation of logic via `TimerViewModel`  
-  **Material 3 Components** – UI design consistency and theme support  

---

##  Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/NaikRitik/RunMetric-Fitness-App.git
   cd RunMetric-Fitness-App
