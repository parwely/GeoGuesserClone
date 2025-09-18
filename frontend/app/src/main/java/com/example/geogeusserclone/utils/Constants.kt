/**
 * Constants.kt
 *
 * Diese Datei enthält alle globalen Konstanten für die GeoGuess-App.
 * Sie definiert Konfigurationswerte für Netzwerk, Spiel-Parameter,
 * Score-Berechnung und andere App-weite Einstellungen.
 *
 * Architektur-Integration:
 * - Configuration Management: Zentrale Stelle für alle App-Konfigurationen
 * - Environment Separation: Unterschiedliche Werte für Debug/Release
 * - Network Configuration: API-URLs und Timeout-Einstellungen
 * - Game Balance: Score-Thresholds und Spiel-Parameter
 * - Security: Sichere Verwaltung von API-Keys und Endpoints
 */
package com.example.geogeusserclone.utils

/**
 * Globale Konstanten für die GeoGuess-App
 *
 * Alle konfigurierbaren Werte der App werden hier zentral verwaltet.
 * Dies ermöglicht einfache Anpassungen ohne Code-Änderungen in
 * verschiedenen Teilen der App.
 */
object Constants {

    // ===== NETZWERK-KONFIGURATION =====

    /**
     * Backend-API Basis-URL
     *
     * Hauptendpunkt für alle API-Calls zum Google Maps Backend.
     * Verschiedene URLs für unterschiedliche Umgebungen.
     */
    const val BASE_URL = "http://10.0.2.2:3000/api/" // Android Emulator lokale Entwicklung
    // Alternative für echtes Gerät: const val BASE_URL = "http://YOUR_LOCAL_IP:3000/api/"
    // Production: const val BASE_URL = "https://your-backend.com/api/"

    /**
     * Mapillary Fallback-API URL
     *
     * Wird nur verwendet wenn das Haupt-Backend nicht verfügbar ist.
     * Bietet alternative Quelle für Street View-ähnliche Bilder.
     */
    const val MAPILLARY_BASE_URL = "https://graph.mapillary.com/"

    /**
     * Mapillary Access Token
     *
     * SICHERHEITSHINWEIS: API-Schlüssel sollten NIEMALS im Code stehen!
     * Verwende BuildConfig oder Environment Variables für Production.
     */
    const val MAPILLARY_ACCESS_TOKEN = "" // ENTFERNT - Verwende BuildConfig.MAPILLARY_API_KEY

    // ===== NETZWERK-TIMEOUTS =====

    /** Timeout für Verbindungsaufbau in Sekunden */
    const val CONNECT_TIMEOUT = 30L

    /** Timeout für Daten-Empfang in Sekunden */
    const val READ_TIMEOUT = 30L

    /** Timeout für Daten-Senden in Sekunden */
    const val WRITE_TIMEOUT = 30L

    // ===== BACKEND-STRATEGIE =====

    /** Backend-First Mode - Mapillary nur als Fallback */
    const val ENABLE_OFFLINE_MODE = false // Backend ist verfügbar

    /** Wartezeit bevor Fallback zu Mapillary aktiviert wird */
    const val BACKEND_FALLBACK_DELAY_MS = 5000L // 5 Sekunden für Backend-Antwort

    // ===== SPIEL-KONFIGURATION =====

    /** Maximale Zeit pro Runde in Millisekunden (2 Minuten) */
    const val MAX_ROUND_TIME_MS = 120000L

    /** Maximaler Zeit-Bonus für schnelle Antworten */
    const val TIME_BONUS_MAX = 500

    /** Blitz-Modus: Zeit pro Runde in Millisekunden (30 Sekunden) */
    const val BLITZ_ROUND_TIME_MS = 30000L

    /** Blitz-Modus: Anzahl Runden */
    const val BLITZ_TOTAL_ROUNDS = 10

    /** Endlos-Modus: Minimaler Score um Streak fortzusetzen */
    const val ENDLESS_MIN_SCORE_FOR_STREAK = 2000

    /** Endlos-Modus: Bonus-Punkte pro Streak-Level */
    const val ENDLESS_STREAK_BONUS = 100

    // ===== SCORE-THRESHOLDS =====

    /** Maximaler Score pro Runde */
    const val MAX_SCORE_PER_ROUND = 5000

    /** Score-Threshold für "Perfekte" Runden */
    const val PERFECT_SCORE_THRESHOLD = 4500

    /** Score-Threshold für "Sehr gut" Bewertung */
    const val EXCELLENT_SCORE_THRESHOLD = 4000

    /** Score-Threshold für "Gut" Bewertung */
    const val GOOD_SCORE_THRESHOLD = 3000

    /** Score-Threshold für "OK" Bewertung */
    const val OK_SCORE_THRESHOLD = 2000

    // ===== DISTANZ-BERECHNUNG =====

    /** Maximale Distanz für vollen Score in Metern */
    const val MAX_DISTANCE_FOR_FULL_SCORE = 25.0

    /** Distanz-Threshold für hohen Score in Metern */
    const val HIGH_SCORE_DISTANCE_THRESHOLD = 1000.0

    /** Distanz-Threshold für mittleren Score in Metern */
    const val MEDIUM_SCORE_DISTANCE_THRESHOLD = 25000.0

    /** Maximale Distanz für Punkte in Metern */
    const val MAX_DISTANCE_FOR_POINTS = 2000000.0 // 2000 km

    // ===== CACHE-KONFIGURATION =====

    /** Maximale Anzahl gecachte Locations */
    const val MAX_CACHED_LOCATIONS = 50

    /** Cache-Lebensdauer für Locations in Millisekunden (24 Stunden) */
    const val LOCATION_CACHE_LIFETIME_MS = 24 * 60 * 60 * 1000L

    /** Maximale Anzahl Locations für Preloading */
    const val PRELOAD_LOCATION_COUNT = 10

    // ===== UI-KONFIGURATION =====

    /** Standard-Zoom-Level für Karten */
    const val DEFAULT_MAP_ZOOM = 2f

    /** Maximaler Zoom-Level für Guess-Karte */
    const val MAX_GUESS_MAP_ZOOM = 18f

    /** Animation-Dauer für UI-Übergänge in Millisekunden */
    const val UI_ANIMATION_DURATION_MS = 300L

    /** Debounce-Zeit für Search-Eingaben in Millisekunden */
    const val SEARCH_DEBOUNCE_TIME_MS = 500L

    // ===== STREET VIEW-KONFIGURATION =====

    /** Standard-Field of View für Street View */
    const val STREET_VIEW_DEFAULT_FOV = 90

    /** Standard-Pitch für Street View */
    const val STREET_VIEW_DEFAULT_PITCH = 0

    /** Suchradius für Street View-Verfügbarkeit in Metern */
    const val STREET_VIEW_SEARCH_RADIUS = 150

    /** Timeout für Street View-Loading in Millisekunden */
    const val STREET_VIEW_TIMEOUT_MS = 15000L

    // ===== FEHLERBEHANDLUNG =====

    /** Maximale Anzahl Retry-Versuche für API-Calls */
    const val MAX_RETRY_ATTEMPTS = 3

    /** Verzögerung zwischen Retry-Versuchen in Millisekunden */
    const val RETRY_DELAY_MS = 1000L

    /** Timeout für kritische Operationen in Millisekunden */
    const val CRITICAL_OPERATION_TIMEOUT_MS = 10000L

    // ===== ANALYTICS UND LOGGING =====

    /** Maximale Log-Einträge im Memory */
    const val MAX_LOG_ENTRIES = 1000

    /** Event-Tracking nur in Release-Builds */
    const val ENABLE_ANALYTICS = false // Setze auf true für Production

    /** Performance-Monitoring Sampling-Rate */
    const val PERFORMANCE_SAMPLING_RATE = 0.1 // 10% der Sessions

    // ===== FEATURE-FLAGS =====

    /** Aktiviere experimentelle Features */
    const val ENABLE_EXPERIMENTAL_FEATURES = false

    /** Aktiviere Advanced Street View-Navigation */
    const val ENABLE_STREET_VIEW_NAVIGATION = true

    /** Aktiviere Offline-Modus */
    const val ENABLE_OFFLINE_SUPPORT = true

    /** Aktiviere Social Features (falls implementiert) */
    const val ENABLE_SOCIAL_FEATURES = false

    // ===== DATENBANK-KONFIGURATION =====

    /** Datenbank-Name */
    const val DATABASE_NAME = "geoguess_database"

    /** Datenbank-Version */
    const val DATABASE_VERSION = 5

    /** Maximale Anzahl alte Spiel-Einträge behalten */
    const val MAX_GAME_HISTORY_ENTRIES = 100

    /** Auto-Cleanup Intervall für Datenbank in Millisekunden (7 Tage) */
    const val DATABASE_CLEANUP_INTERVAL_MS = 7 * 24 * 60 * 60 * 1000L

    // ===== SECURITY =====

    /** Session-Timeout in Millisekunden (24 Stunden) */
    const val SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000L

    /** Maximale Login-Versuche */
    const val MAX_LOGIN_ATTEMPTS = 5

    /** Lockout-Zeit nach zu vielen fehlgeschlagenen Login-Versuchen */
    const val LOGIN_LOCKOUT_TIME_MS = 15 * 60 * 1000L // 15 Minuten

    // ===== SPIELMODUS-SPEZIFISCHE KONSTANTEN =====

    /** Klassischer Modus: Anzahl Runden */
    const val CLASSIC_ROUNDS = 5

    /** Klassischer Modus: Kein Zeitlimit */
    const val CLASSIC_TIME_LIMIT_MS = 0L // Unbegrenzt

    /** Blitz-Modus: Bewegung deaktiviert */
    const val BLITZ_DISABLE_MOVEMENT = true

    /** Endlos-Modus: Unbegrenzte Runden */
    const val ENDLESS_UNLIMITED_ROUNDS = -1
}