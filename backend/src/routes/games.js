const express = require('express');
const router = express.Router();
const { authenticateToken } = require('../middleware/authMiddleware');
const locationService = require('../services/locationService');
const database = require('../database/connection');

// Create single player game
router.post('/single', authenticateToken, async (req, res) => {
  try {
    const { difficulty, category, rounds = 5 } = req.body;
    const userId = req.user.id;
    
    // Get random locations for the game
    const locations = await locationService.getRandomLocations({
      count: rounds,
      difficulty,
      category
    });
    
    if (locations.length < rounds) {
      return res.status(400).json({
        error: 'Not enough locations available',
        message: `Only ${locations.length} locations found, need ${rounds}`
      });
    }
    
    // Create game session
    const result = await database.query(`
      INSERT INTO game_sessions (session_type, created_by, status, settings, location_ids, max_players)
      VALUES ('single', $1, 'active', $2, $3, 1)
      RETURNING id, created_at
    `, [
      userId,
      JSON.stringify({ difficulty, category, rounds }),
      locations.map(loc => loc.id)
    ]);
    
    const gameSession = result.rows[0];
    
    res.json({
      success: true,
      data: {
        gameId: gameSession.id,
        locations: locations,
        settings: { difficulty, category, rounds },
        createdAt: gameSession.created_at
      }
    });
    
  } catch (error) {
    console.error('❌ Create single game failed:', error.message);
    res.status(500).json({
      error: 'Failed to create game',
      message: 'Internal server error'
    });
  }
});

// Submit game result
router.put('/:id/result', authenticateToken, async (req, res) => {
  try {
    const gameId = parseInt(req.params.id);
    const userId = req.user.id;
    const { totalScore, totalDistance, accuracy, roundsData, timeTaken } = req.body;
    
    if (!roundsData || !Array.isArray(roundsData)) {
      return res.status(400).json({
        error: 'Invalid game result data',
        message: 'roundsData must be an array'
      });
    }
    
    // Verify game belongs to user
    const gameCheck = await database.query(`
      SELECT id FROM game_sessions 
      WHERE id = $1 AND created_by = $2
    `, [gameId, userId]);
    
    if (gameCheck.rows.length === 0) {
      return res.status(404).json({
        error: 'Game not found',
        message: 'Game does not exist or does not belong to user'
      });
    }
    
    // Insert game result
    const result = await database.query(`
      INSERT INTO game_results (session_id, user_id, total_score, total_distance, accuracy, rounds_completed, time_taken, rounds_data)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
      RETURNING id, created_at
    `, [
      gameId,
      userId,
      totalScore,
      totalDistance,
      accuracy,
      roundsData.length,
      timeTaken,
      JSON.stringify(roundsData)
    ]);
    
    // Update game session as finished
    await database.query(`
      UPDATE game_sessions 
      SET status = 'finished', finished_at = NOW()
      WHERE id = $1
    `, [gameId]);
    
    // Update user stats
    await database.query(`
      UPDATE users 
      SET total_score = total_score + $1, 
          games_played = games_played + 1
      WHERE id = $2
    `, [totalScore, userId]);
    
    res.json({
      success: true,
      data: {
        resultId: result.rows[0].id,
        gameId: gameId,
        totalScore,
        submittedAt: result.rows[0].created_at
      }
    });
    
  } catch (error) {
    console.error('❌ Submit game result failed:', error.message);
    res.status(500).json({
      error: 'Failed to submit game result',
      message: 'Internal server error'
    });
  }
});

module.exports = router;