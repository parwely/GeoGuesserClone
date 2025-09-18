/**
 * RepositoryModule.kt
 *
 * Diese Datei konfiguriert alle Repository-Abhängigkeiten für die GeoGuess-App.
 * Sie stellt die Repository-Schicht über Hilt Dependency Injection bereit
 * und verbindet die Datenquellen (API, Datenbank) mit der Geschäftslogik.
 *
 * Architektur-Integration:
 * - Repository Pattern: Abstrakte Schicht zwischen ViewModels und Datenquellen
 * - Dependency Injection: Hilt-basierte Bereitstellung aller Repositories
 * - Data Layer: Verbindet Netzwerk- und Datenbankzugriff
 * - Testability: Ermöglicht einfaches Mocking für Unit-Tests
 * - Single Source of Truth: Zentrale Datenverwaltung
 */
package com.example.geogeusserclone

import com.example.geogeusserclone.data.repositories.GameRepository
import com.example.geogeusserclone.data.repositories.LocationRepository
import com.example.geogeusserclone.data.repositories.UserRepository
import com.example.geogeusserclone.data.database.dao.GameDao
import com.example.geogeusserclone.data.database.dao.GuessDao
import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.dao.UserDao
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.GameApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-Modul für Repository-Konfiguration
 *
 * Stellt alle Repository-Implementierungen als Singletons bereit.
 * Repositories fungieren als Single Source of Truth und abstrahieren
 * Datenquellen für die ViewModels.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Stellt UserRepository als Singleton bereit
     *
     * Das UserRepository verwaltet alle benutzerbezogenen Operationen:
     * - Authentifizierung (Login/Registrierung)
     * - Benutzerstatistiken und -profile
     * - Session-Management
     * - Offline/Online-Synchronisation
     *
     * @param apiService Netzwerk-Service für Backend-Kommunikation
     * @param userDao Datenbank-DAO für lokale Benutzerdaten
     * @return Konfigurierte UserRepository-Instanz
     */
    @Provides
    @Singleton
    fun provideUserRepository(
        apiService: ApiService,
        userDao: UserDao
    ): UserRepository {
        return UserRepository(apiService, userDao)
    }

    /**
     * Stellt LocationRepository als Singleton bereit
     *
     * Das LocationRepository verwaltet alle Location-bezogenen Operationen:
     * - Zufällige Location-Generierung
     * - Street View-Verfügbarkeitsprüfung
     * - Location-Caching und -Preloading
     * - Fallback-Strategien (Backend → Mapillary → Offline)
     * - Performance-Optimierungen für Location-Suche
     *
     * @param apiService Netzwerk-Service für Backend-Kommunikation
     * @param locationDao Datenbank-DAO für lokale Location-Daten
     * @return Konfigurierte LocationRepository-Instanz
     */
    @Provides
    @Singleton
    fun provideLocationRepository(
        apiService: ApiService,
        locationDao: LocationDao
    ): LocationRepository {
        return LocationRepository(apiService, locationDao)
    }

    /**
     * Stellt GameRepository als Singleton bereit
     *
     * Das GameRepository verwaltet alle spielbezogenen Operationen:
     * - Rundenmanagement (neue Runden, Score-Berechnung)
     * - Spielmodus-spezifische Logik
     * - Backend-Integration für Spiel-APIs
     * - Leaderboards und Statistiken
     * - Game Session-Verwaltung
     *
     * @param gameApi Spezielle Game-API für Spiel-Operationen
     * @return Konfigurierte GameRepository-Instanz
     */
    @Provides
    @Singleton
    fun provideGameRepository(
        gameApi: GameApi
    ): GameRepository {
        return GameRepository(gameApi)
    }
}
