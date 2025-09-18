/**
 * NetworkModule.kt
 *
 * Dieses Modul konfiguriert die Netzwerk-Infrastruktur der GeoGuess-App.
 * Es verwendet Hilt für Dependency Injection und stellt alle netzwerkbezogenen
 * Komponenten für die gesamte App zur Verfügung.
 *
 * Architektur-Integration:
 * - Dependency Injection: Hilt-basierte Bereitstellung von Singleton-Instanzen
 * - Network Layer: Retrofit, OkHttp und Interceptor-Konfiguration
 * - Security: Token-Management und Authentication-Interceptors
 * - Performance: Caching und Timeout-Optimierungen
 */
package com.example.geogeusserclone

import android.content.Context
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.AuthInterceptor
import com.example.geogeusserclone.data.network.TokenManager
import com.example.geogeusserclone.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

/**
 * Hilt-Modul für Netzwerk-Abhängigkeiten
 *
 * Dieses Modul wird in der SingletonComponent installiert, was bedeutet,
 * dass alle bereitgestellten Instanzen App-weit als Singletons verfügbar sind.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Stellt den TokenManager als Singleton bereit
     *
     * Der TokenManager verwaltet Authentication-Tokens sicher im SharedPreferences
     * und ermöglicht automatische Token-Erneuerung.
     *
     * @param context Application Context für SharedPreferences-Zugriff
     * @return TokenManager-Instanz
     */
    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    /**
     * Stellt den HTTP-Cache als Singleton bereit
     *
     * Konfiguriert einen 10MB Cache für HTTP-Responses um Netzwerkaufrufe
     * zu reduzieren und die App-Performance zu verbessern.
     *
     * @param context Application Context für Cache-Directory-Zugriff
     * @return OkHttp Cache-Instanz
     */
    @Provides
    @Singleton
    fun provideCache(@ApplicationContext context: Context): Cache {
        val cacheSize = 10 * 1024 * 1024L // 10 MB Cache-Größe
        return Cache(context.cacheDir, cacheSize)
    }

    /**
     * Stellt den Authentication-Interceptor als Singleton bereit
     *
     * Dieser Interceptor fügt automatisch Authorization-Headers zu allen
     * ausgehenden Requests hinzu und verwaltet Token-Erneuerung.
     *
     * @param tokenManager TokenManager für Token-Verwaltung
     * @return AuthInterceptor-Instanz
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    /**
     * Stellt den konfigurierten OkHttp-Client als Singleton bereit
     *
     * Konfiguriert den HTTP-Client mit Interceptors für Authentication,
     * Logging, Caching und Timeout-Einstellungen.
     *
     * @param authInterceptor Interceptor für automatische Authentication
     * @param cache HTTP-Cache für Response-Caching
     * @return Konfigurierte OkHttpClient-Instanz
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        cache: Cache
    ): OkHttpClient {
        // Logging-Interceptor für Debug-Zwecke (zeigt alle HTTP-Requests/Responses)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)     // Authentication-Headers hinzufügen
            .addInterceptor(loggingInterceptor)  // Request/Response-Logging
            .cache(cache)                        // Response-Caching aktivieren
            .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)  // Verbindungs-Timeout
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)        // Lese-Timeout
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)      // Schreib-Timeout
            .build()
    }

    /**
     * Stellt die konfigurierte Retrofit-Instanz als Singleton bereit
     *
     * Retrofit ist das Haupt-HTTP-Client-Framework für API-Calls.
     * Konfiguriert mit Gson für JSON-Serialisierung und der Base-URL aus Constants.
     *
     * @param okHttpClient Konfigurierter HTTP-Client
     * @return Retrofit-Instanz für API-Calls
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)                    // Base-URL für alle API-Calls
            .client(okHttpClient)                           // Verwende konfigurierten HTTP-Client
            .addConverterFactory(GsonConverterFactory.create()) // JSON-Serialisierung mit Gson
            .build()
    }

    /**
     * Stellt das ApiService-Interface als Singleton bereit
     *
     * Das ApiService-Interface definiert alle verfügbaren API-Endpunkte
     * und wird von Retrofit automatisch implementiert.
     *
     * @param retrofit Konfigurierte Retrofit-Instanz
     * @return ApiService-Implementation für API-Aufrufe
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}