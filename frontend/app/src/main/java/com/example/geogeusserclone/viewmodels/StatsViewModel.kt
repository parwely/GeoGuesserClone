package com.example.geogeusserclone.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.repositories.GameRepository
import com.example.geogeusserclone.data.repositories.GameStats
import com.example.geogeusserclone.data.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsState(
    val isLoading: Boolean = false,
    val userGames: List<GameEntity> = emptyList(),
    val gameStats: GameStats? = null,
    val error: String? = null,
    val selectedTimeRange: TimeRange = TimeRange.ALL_TIME
)

enum class TimeRange {
    ALL_TIME, LAST_WEEK, LAST_MONTH, LAST_YEAR
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) : BaseViewModel<StatsState>(StatsState()) {

    init {
        loadUserStats()
    }

    private fun loadUserStats() {
        viewModelScope.launch {
            val currentUser = userRepository.getCurrentUser()
            if (currentUser == null) {
                setState(state.value.copy(error = "Benutzer nicht angemeldet"))
                return@launch
            }

            setState(state.value.copy(isLoading = true))

            try {
                // Load user games
                gameRepository.getGamesByUser(currentUser.id).collectLatest { games ->
                    val filteredGames = filterGamesByTimeRange(games, state.value.selectedTimeRange)
                    setState(state.value.copy(userGames = filteredGames))
                }

                // Load game stats
                val stats = gameRepository.getUserStats(currentUser.id)
                setState(state.value.copy(
                    isLoading = false,
                    gameStats = stats,
                    error = null
                ))

            } catch (e: Exception) {
                setState(state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Statistiken konnten nicht geladen werden"
                ))
            }
        }
    }

    fun changeTimeRange(timeRange: TimeRange) {
        setState(state.value.copy(selectedTimeRange = timeRange))
        loadUserStats()
    }

    private fun filterGamesByTimeRange(games: List<GameEntity>, timeRange: TimeRange): List<GameEntity> {
        val currentTime = System.currentTimeMillis()
        val filterTime = when (timeRange) {
            TimeRange.ALL_TIME -> 0L
            TimeRange.LAST_WEEK -> currentTime - (7 * 24 * 60 * 60 * 1000L)
            TimeRange.LAST_MONTH -> currentTime - (30 * 24 * 60 * 60 * 1000L)
            TimeRange.LAST_YEAR -> currentTime - (365 * 24 * 60 * 60 * 1000L)
        }

        return games.filter { it.timestamp >= filterTime }
    }

    fun getAverageScoreByGameMode(): Map<String, Double> {
        val games = state.value.userGames.filter { it.isCompleted }
        return games.groupBy { it.gameMode }
            .mapValues { (_, gameList) ->
                if (gameList.isEmpty()) 0.0
                else gameList.map { it.score }.average()
            }
    }

    fun getBestGamesByMode(): Map<String, GameEntity?> {
        val games = state.value.userGames.filter { it.isCompleted }
        return games.groupBy { it.gameMode }
            .mapValues { (_, gameList) ->
                gameList.maxByOrNull { it.score }
            }
    }

    fun getRecentGames(limit: Int = 10): List<GameEntity> {
        return state.value.userGames
            .filter { it.isCompleted }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    fun clearError() {
        setState(state.value.copy(error = null))
    }
}