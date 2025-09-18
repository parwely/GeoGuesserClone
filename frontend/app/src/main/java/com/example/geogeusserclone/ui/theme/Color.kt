/**
 * Color.kt
 *
 * Diese Datei definiert die Farbpalette für die GeoGuess-App basierend auf Material Design 3.
 * Sie enthält sowohl Light- als auch Dark-Mode-Varianten aller verwendeten Farben.
 *
 * Architektur-Integration:
 * - Design System: Zentrale Farbdefinitionen für konsistentes UI-Design
 * - Material 3: Moderne Farbpalette nach Material Design 3 Guidelines
 * - Accessibility: Kontrastreiche Farben für bessere Lesbarkeit
 * - Theme Support: Separate Farbsets für Light/Dark Mode
 * - Brand Identity: App-spezifische Farbanpassungen für GeoGuess
 */
package com.example.geogeusserclone.ui.theme

import androidx.compose.ui.graphics.Color

// ===== MATERIAL DESIGN 3 BASIS-FARBEN =====

/**
 * Purple80 - Primäre Akzentfarbe für Dark Mode
 * Heller Lila-Ton mit hohem Kontrast gegen dunkle Hintergründe
 */
val Purple80 = Color(0xFFD0BCFF)

/**
 * PurpleGrey80 - Sekundäre Farbe für Dark Mode
 * Gedämpfter Grau-Lila-Ton für weniger prominente UI-Elemente
 */
val PurpleGrey80 = Color(0xFFCCC2DC)

/**
 * Pink80 - Tertiäre Akzentfarbe für Dark Mode
 * Warmer Rosa-Ton für spezielle Highlights und Aktionen
 */
val Pink80 = Color(0xFFEFB8C8)

/**
 * Purple40 - Primäre Akzentfarbe für Light Mode
 * Dunklerer Lila-Ton mit gutem Kontrast gegen helle Hintergründe
 */
val Purple40 = Color(0xFF6650a4)

/**
 * PurpleGrey40 - Sekundäre Farbe für Light Mode
 * Gedämpfter Grau-Lila-Ton für subtile UI-Elemente
 */
val PurpleGrey40 = Color(0xFF625b71)

/**
 * Pink40 - Tertiäre Akzentfarbe für Light Mode
 * Dunklerer Rosa-Ton für Highlights und Call-to-Action-Elemente
 */
val Pink40 = Color(0xFF7D5260)

// ===== GEOGÜESS-SPEZIFISCHE FARBPALETTE =====

/**
 * GeoGuess Blue - Hauptfarbe der App
 * Vertrauenserweckender Blau-Ton der geografische Themen unterstützt
 */
val GeoBlue = Color(0xFF1976D2)
val GeoBlueDark = Color(0xFF90CAF9)

/**
 * Geographic Green - Für Karten und Natur-Elemente
 * Natürlicher Grün-Ton für Location-Marker und Erfolgs-Anzeigen
 */
val GeoGreen = Color(0xFF4CAF50)
val GeoGreenDark = Color(0xFF81C784)

/**
 * Warning Orange - Für Zeitwarnungen und Aufmerksamkeit
 * Signalfarbe für Blitz-Modus und Zeit-kritische Elemente
 */
val GeoOrange = Color(0xFFFF9800)
val GeoOrangeDark = Color(0xFFFFB74D)

/**
 * Error Red - Für Fehler und Negativ-Feedback
 * Klare Signalfarbe für Probleme und falsche Antworten
 */
val GeoRed = Color(0xFFf44336)
val GeoRedDark = Color(0xFFEF5350)

/**
 * Success Gold - Für Achievements und Perfekte Scores
 * Premium-Farbe für Belohnungen und Bestleistungen
 */
val GeoGold = Color(0xFFFFD700)
val GeoGoldDark = Color(0xFFFFE082)

// ===== NEUTRAL-FARBEN FÜR UI-HIERARCHIE =====

/**
 * Surface Colors - Für Karten und Container
 * Subtile Hintergrundfarben die Inhalte hervorheben
 */
val SurfaceLight = Color(0xFFFFFBFE)
val SurfaceDark = Color(0xFF121212)

/**
 * Overlay Colors - Für Semi-transparente Overlays
 * Ermöglichen Inhalts-Überlagerung mit erhaltener Lesbarkeit
 */
val OverlayLight = Color(0x80FFFFFF)
val OverlayDark = Color(0x80000000)

/**
 * Border Colors - Für Umrandungen und Trennlinien
 * Subtile Abgrenzungen zwischen UI-Elementen
 */
val BorderLight = Color(0xFFE0E0E0)
val BorderDark = Color(0xFF424242)

// ===== SPIELMODUS-SPEZIFISCHE FARBEN =====

/**
 * Classic Mode Colors - Für traditionelles Gameplay
 * Beruhigende Farben die Konzentration fördern
 */
val ClassicBlue = Color(0xFF2196F3)
val ClassicBlueDark = Color(0xFF64B5F6)

/**
 * Blitz Mode Colors - Für zeitdruckbasierte Spiele
 * Energetische Farben die Urgenz vermitteln
 */
val BlitzOrange = Color(0xFFFF5722)
val BlitzOrangeDark = Color(0xFFFF8A65)

/**
 * Endless Mode Colors - Für Marathon-Sessions
 * Ausdauernde Farben die Langzeit-Gameplay unterstützen
 */
val EndlessPurple = Color(0xFF9C27B0)
val EndlessPurpleDark = Color(0xFFBA68C8)

// ===== SCORE-BASIERTE FARBEN =====

/**
 * Score Rating Colors - Für Performance-Feedback
 * Farbkodierte Bewertungen von Rateversuchen
 */
val ScorePerfect = Color(0xFF4CAF50)      // Perfekte Punktzahl (4500+)
val ScoreExcellent = Color(0xFF8BC34A)    // Ausgezeichnet (4000+)
val ScoreGood = Color(0xFFCDDC39)         // Gut (3000+)
val ScoreOkay = Color(0xFFFFEB3B)         // Okay (2000+)
val ScorePoor = Color(0xFFFF9800)         // Schlecht (1000+)
val ScoreVeryPoor = Color(0xFFFF5722)     // Sehr schlecht (<1000)

// ===== ACCESSIBILITY-OPTIMIERTE FARBEN =====

/**
 * High Contrast Colors - Für verbesserte Lesbarkeit
 * WCAG AA-konforme Farbkombinationen
 */
val HighContrastText = Color(0xFF000000)
val HighContrastBackground = Color(0xFFFFFFFF)
val HighContrastAccent = Color(0xFF0066CC)

/**
 * Low Vision Support - Für Sehbehinderungen
 * Verstärkte Kontraste und größere Farbunterschiede
 */
val LowVisionPrimary = Color(0xFF000080)
val LowVisionSecondary = Color(0xFF800000)
val LowVisionAccent = Color(0xFF008000)
