/**
 * Theme.kt
 *
 * Diese Datei definiert das Material Design 3 Theme-System für die GeoGuess-App.
 * Sie enthält Farbschemata, Typografie und Theme-Konfigurationen für Light/Dark Mode.
 *
 * Architektur-Integration:
 * - Design System: Zentrale Definition aller visuellen Elemente
 * - Material 3: Moderne Material Design 3 Implementierung
 * - Dark/Light Mode: Automatische Theme-Umschaltung basierend auf Systemeinstellungen
 * - Konsistenz: Einheitliches Erscheinungsbild in der gesamten App
 */
package com.example.geogeusserclone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

/**
 * Light Mode Farbschema
 *
 * Definiert alle Farben für den hellen Modus der App.
 * Basiert auf Material Design 3 Guidelines mit angepassten Akzentfarben.
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),        // Hauptfarbe (Blau)
    secondary = Color(0xFF03DAC6),      // Sekundärfarbe (Türkis)
    tertiary = Color(0xFF3700B3),       // Tertiärfarbe (Lila)
    background = Color(0xFFFFFBFE),     // Hintergrundfarbe
    surface = Color(0xFFFFFBFE),        // Oberflächenfarbe
    onPrimary = Color.White,            // Text auf Hauptfarbe
    onSecondary = Color.Black,          // Text auf Sekundärfarbe
    onTertiary = Color.White,           // Text auf Tertiärfarbe
    onBackground = Color(0xFF1C1B1F),   // Text auf Hintergrund
    onSurface = Color(0xFF1C1B1F),      // Text auf Oberflächen
)

/**
 * Dark Mode Farbschema
 *
 * Definiert alle Farben für den dunklen Modus der App.
 * Optimiert für geringe Beleuchtung und bessere Akku-Laufzeit bei OLED-Displays.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),        // Helleres Blau für Dark Mode
    secondary = Color(0xFF03DAC6),      // Türkis bleibt gleich
    tertiary = Color(0xFFBB86FC),       // Helleres Lila
    background = Color(0xFF121212),     // Dunkler Hintergrund
    surface = Color(0xFF121212),        // Dunkle Oberflächen
    onPrimary = Color.Black,            // Dunkler Text auf heller Hauptfarbe
    onSecondary = Color.Black,          // Dunkler Text auf Sekundärfarbe
    onTertiary = Color.Black,           // Dunkler Text auf Tertiärfarbe
    onBackground = Color(0xFFE1E2E1),   // Heller Text auf dunklem Hintergrund
    onSurface = Color(0xFFE1E2E1),      // Heller Text auf dunklen Oberflächen
)

/**
 * Haupt-Theme Composable für die GeoGuess-App
 *
 * Wendet das entsprechende Farbschema basierend auf den Systemeinstellungen an
 * und stellt Material Design 3 Komponenten zur Verfügung.
 *
 * @param darkTheme Ob der dunkle Modus verwendet werden soll (Standard: Systemeinstellung)
 * @param content Der UI-Inhalt, der mit dem Theme gestylt werden soll
 */
@Composable
fun GeoGuessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Wähle das entsprechende Farbschema basierend auf dem Theme-Modus
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    // Wende Material Design 3 Theme mit ausgewähltem Farbschema an
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(), // Standard Material 3 Typografie
        content = content
    )
}

/**
 * Rückwärtskompatibilitäts-Alias für das Theme
 *
 * Ermöglicht die Verwendung des alten Theme-Namens ohne Breaking Changes
 * in existierendem Code.
 *
 * @param darkTheme Ob der dunkle Modus verwendet werden soll
 * @param content Der UI-Inhalt, der mit dem Theme gestylt werden soll
 */
@Composable
fun GeoGeusserCloneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    GeoGuessTheme(darkTheme = darkTheme, content = content)
}
