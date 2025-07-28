package com.example.geogeusserclone.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.models.GameStats
import com.example.geogeusserclone.data.models.UserStats
import com.example.geogeusserclone.data.repositories.GameRepository
import com.example.geogeusserclone.data.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val gameStats: GameStats = GameStats(),
    val userStats: List<UserStats> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadUserStats()
    }

    private fun loadUserStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val currentUser = userRepository.getCurrentUser()
                if (currentUser != null) {
                    // Lade Spiele-Statistiken
                    gameRepository.getGameHistory(currentUser.id).collect { games: List<GameEntity> ->
                        val gameStats = calculateGameStats(games)
                        _uiState.update {
                            it.copy(
                                gameStats = gameStats,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Benutzer nicht angemeldet"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Fehler beim Laden der Statistiken: ${e.message}"
                    )
                }
            }
        }
    }

    private fun calculateGameStats(games: List<GameEntity>): GameStats {
        if (games.isEmpty()) return GameStats()

        val completedGames = games.filter { it.isCompleted }
        val totalGames = completedGames.size
        val totalScore = completedGames.sumOf { it.score }
        val averageScore = if (totalGames > 0) totalScore.toDouble() / totalGames else 0.0
        val bestScore = completedGames.maxOfOrNull { it.score } ?: 0

        // Monatliche Statistiken berechnen
        val monthlyStats = completedGames
            .groupBy { game ->
                val date = java.util.Date(game.completedAt ?: game.createdAt)
                val calendar = java.util.Calendar.getInstance()
                calendar.time = date
                "${calendar.get(java.util.Calendar.MONTH + 1)}-${calendar.get(java.util.Calendar.YEAR)}"
            }
            .map { (monthYear, monthGames) ->
                val parts = monthYear.split("-")
                val month = parts[0]
                val year = parts[1].toInt()

                com.example.geogeusserclone.data.models.MonthlyGameStats(
                    month = getMonthName(month.toInt()),
                    year = year,
                    gamesPlayed = monthGames.size,
                    totalScore = monthGames.sumOf { it.score },
                    averageScore = monthGames.map { it.score }.average()
                )
            }
            .sortedByDescending { it.year * 12 + getMonthNumber(it.month) }

        return GameStats(
            totalGames = totalGames,
            totalScore = totalScore,
            averageScore = averageScore,
            bestScore = bestScore,
            monthlyStats = monthlyStats
        )
    }

    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "Januar"
            2 -> "Februar"
            3 -> "März"
            4 -> "April"
            5 -> "Mai"
            6 -> "Juni"
            7 -> "Juli"
            8 -> "August"
            9 -> "September"
            10 -> "Oktober"
            11 -> "November"
            12 -> "Dezember"
            else -> "Unbekannt"
        }
    }

    private fun getMonthNumber(monthName: String): Int {
        return when (monthName) {
            "Januar" -> 1
            "Februar" -> 2
            "März" -> 3
            "April" -> 4
            "Mai" -> 5
            "Juni" -> 6
            "Juli" -> 7
            "August" -> 8
            "September" -> 9
            "Oktober" -> 10
            "November" -> 11
            "Dezember" -> 12
            else -> 1
        }
    }

    fun refreshStats() {
        loadUserStats()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}