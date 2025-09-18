# 🌍 GeoGuess - Frontend Dokumentation

**Version:** 1.0.0  
**Stand:** September 2025  
**Architektur:** MVVM, Single-Activity, Compose, Google Maps SDK
Github: https://github.com/parwely/GeoGuesserClone
---

## 📋 Inhaltsverzeichnis

1. [Architektur-Übersicht](#architektur)
2. [Technologie-Stack](#technologien)
3. [UI-Komponenten & Spielfluss](#ui)
4. [Netzwerk & API-Integration](#api)
5. [State Management & ViewModel](#state)
6. [Fehlerbehandlung & Fallbacks](#fallbacks)
7. [Testing-Strategien](#testing)
8. [Build & Deployment](#build)
9. [Best Practices & Besonderheiten](#bestpractices)

---

## 🏗️ Architektur-Übersicht {#architektur}

Das Frontend ist als moderne Android-App in **Kotlin** mit **Jetpack Compose** und dem **MVVM-Pattern** aufgebaut. Die gesamte Spiellogik ist im `GameViewModel` gekapselt, die UI wird über Compose-`@Composable`-Funktionen gesteuert. Die App nutzt ein **Single-Activity-Pattern** (`GameActivity`), in der alle Spielmodi und UI-Overlays orchestriert werden.

**Schichtenmodell:**

```
┌─────────────────────────────────────┐
│           Android UI (Compose)      │
├─────────────────────────────────────┤
│         ViewModel (MVVM)            │
├─────────────────────────────────────┤
│        Repository Layer             │
├─────────────────────────────────────┤
│      Retrofit API (GameApi)         │
└─────────────────────────────────────┘
```

---

## 🛠️ Technologie-Stack {#technologien}

- **Kotlin** (Sprache)
- **Jetpack Compose** (UI)
- **Google Maps SDK** (Street View, MapView)
- **Retrofit2** (Netzwerk)
- **Hilt** (Dependency Injection)
- **Coroutines/Flow** (Async, State)
- **Material3** (Design)
- **JUnit/Espresso** (Testing)

---

## 🎮 UI-Komponenten & Spielfluss {#ui}

### Haupt-Activity: `GameActivity.kt`

```kotlin
@AndroidEntryPoint
class GameActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val gameModeString = intent.getStringExtra("GAME_MODE") ?: "CLASSIC"
        val gameMode = try { GameMode.valueOf(gameModeString) } catch (e: Exception) { GameMode.CLASSIC }
        setContent {
            GeoGuessTheme {
                val gameState by gameViewModel.gameState.collectAsState()
                val uiState by gameViewModel.uiState.collectAsState()
                LaunchedEffect(Unit) { gameViewModel.startGame(gameMode) }
                StreetViewGameScreen(
                    gameState = gameState,
                    uiState = uiState,
                    onGuess = { lat, lng -> gameViewModel.submitGuess(lat, lng) },
                    onNextRound = { gameViewModel.nextRound() },
                    onShowMap = { gameViewModel.showGuessMap() },
                    onHideMap = { gameViewModel.hideGuessMap() },
                    onStreetViewReady = { gameViewModel.onStreetViewReady() },
                    onBack = { finish() },
                    onClearError = { gameViewModel.clearError() },
                    onEndEndlessGame = { gameViewModel.endEndlessGame() }
                )
            }
        }
    }
}
```

### Interaktive Street View-Komponente

```kotlin
@Composable
fun InteractiveStreetViewWithFallback(
    location: LocationEntity,
    onStreetViewReady: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // ...
    // Timeout-Mechanismus, Backend-Validierung, Fallback zu statischem Bild
    // Google Maps SDK Integration
}
```

### Guess Map mit Fallback

```kotlin
@Composable
fun GuessMapView(
    onGuessSelected: (Double, Double) -> Unit,
    onMapClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ...
    // GoogleMap, Marker, Touch-Events, Lifecycle-Handling
}
```

---

## 🌐 Netzwerk & API-Integration {#api}

Die App nutzt ein Repository-Pattern mit Retrofit für alle Backend-Operationen:

```kotlin
interface GameApi {
    @GET("game/newRound")
    suspend fun newRound(@Query("difficulty") difficulty: Int? = null, @Query("category") category: String? = null): Response<BackendRoundResponse>
    @POST("game/guess")
    suspend fun submitGuess(@Body guess: GuessRequest): Response<BackendScoreResponse>
}
```

Das `GameRepository` kapselt die API-Calls und Fehlerbehandlung:

```kotlin
@Singleton
class GameRepository @Inject constructor(private val gameApi: GameApi) {
    suspend fun startNewRound(difficulty: Int? = null, category: String? = null): Result<NewRoundResponse> { /* ... */ }
    suspend fun submitGuess(guess: GuessRequest): Result<ScoreResponse> { /* ... */ }
}
```

---

## 🔄 State Management & ViewModel {#state}

Das zentrale `GameViewModel` steuert den Spielfluss, verwaltet State und orchestriert die UI:

```kotlin
@HiltViewModel
class GameViewModel @Inject constructor(private val gameRepository: GameRepository) : ViewModel() {
    val gameState: StateFlow<GameState>
    val uiState: StateFlow<GameUiState>
    fun startGame(gameMode: GameMode = GameMode.CLASSIC) { /* ... */ }
    private suspend fun startNewRound() { /* ... */ }
    fun submitGuess(lat: Double, lng: Double) { /* ... */ }
    // ...
}
```

---

## 🛡️ Fehlerbehandlung & Fallbacks {#fallbacks}

- **Street View Timeout**: Nach 20s automatischer Fallback auf statisches Bild
- **Emulator Detection**: Nach 15s ohne Map-Click wird ein Fallback-UI angeboten
- **Backend-Validierung**: Vor Initialisierung wird geprüft, ob Street View für die Location verfügbar ist
- **UI-Feedback**: Fehler werden per Snackbar angezeigt

---

## 🧪 Testing-Strategien {#testing}

- **Unit-Tests** für ViewModel und Repository (JUnit)
- **UI-Tests** für Compose-Komponenten (Espresso, Compose Test)
- **Mocking** von Netzwerk- und Datenquellen

---

## ⚙️ Build & Deployment {#build}

- **Gradle** für Build-Management
- **Proguard** für Release-Optimierung
- **Build Variants** für Debug/Release

---

## ⭐ Best Practices & Besonderheiten {#bestpractices}

- **MVVM Pattern** für klare Trennung von UI und Logik
- **Single-Activity mit Compose** für flexible UI
- **Lifecycle-Handling** für MapView/StreetView
- **Adaptive Fallbacks** für Emulator und Street View
- **Konsistente Fehlerbehandlung und Logging**

---

**Ende der Dokumentation**
