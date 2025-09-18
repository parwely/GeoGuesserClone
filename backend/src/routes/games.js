const express = require("express");
const router = express.Router();
const gameService = require("../services/gameService");
const database = require("../database/connection");

// GET /api/game/newRound - Start a new game round
router.get("/newRound", async (req, res) => {
  try {
    console.log("🎮 Game Route: /newRound called");

    const userId = req.query.userId || req.user?.id || null;

    // Get a random location that has Street View coverage
    const location = await gameService.getRandomGameLocation();

    if (!location) {
      return res.status(404).json({
        error: "No locations available",
        message: "No Street View locations found for game",
      });
    }

    // Create a new round record
    const round = await gameService.createRound(location.id, userId);

    console.log(
      `✅ New round created: ${round.id} for location: ${location.name}`
    );

    res.json({
      id: round.id,
      lat: location.coordinates.latitude,
      lng: location.coordinates.longitude,
      pano_id: location.pano_id || undefined,
      location_hint: location.country, // Optional hint without giving away the answer
    });
  } catch (error) {
    console.error("❌ Game Route: /newRound failed:", error.message);
    res.status(500).json({
      error: "Failed to create new round",
      message: "Internal server error",
    });
  }
});

// POST /api/game/guess - Submit a guess for a round
router.post("/guess", async (req, res) => {
  try {
    console.log("🎯 Game Route: /guess called");

    const { roundId, guessLat, guessLng, userId } = req.body;

    // Validate required parameters
    if (!roundId || guessLat === undefined || guessLng === undefined) {
      return res.status(400).json({
        error: "Invalid guess data",
        message: "roundId, guessLat, and guessLng are required",
      });
    }

    // Validate coordinate ranges
    if (guessLat < -90 || guessLat > 90 || guessLng < -180 || guessLng > 180) {
      return res.status(400).json({
        error: "Invalid coordinates",
        message: "Latitude must be -90 to 90, longitude must be -180 to 180",
      });
    }

    // Process the guess
    const result = await gameService.processGuess({
      roundId: parseInt(roundId),
      guessLat: parseFloat(guessLat),
      guessLng: parseFloat(guessLng),
      userId: userId || req.user?.id || null,
    });

    console.log(
      `✅ Guess processed for round ${roundId}: ${result.distanceMeters}m = ${result.score} points`
    );

    res.json({
      distanceMeters: result.distanceMeters,
      score: result.score,
      actual: {
        lat: result.actualLocation.latitude,
        lng: result.actualLocation.longitude,
        name: result.actualLocation.name,
        country: result.actualLocation.country,
      },
      maxPossibleScore: result.maxPossibleScore,
    });
  } catch (error) {
    console.error("❌ Game Route: /guess failed:", error.message);

    if (error.message === "Round not found") {
      return res.status(404).json({
        error: "Round not found",
        message: `No round found with ID ${req.body.roundId}`,
      });
    }

    if (error.message === "Round already completed") {
      return res.status(400).json({
        error: "Round already completed",
        message: "This round has already been guessed",
      });
    }

    res.status(500).json({
      error: "Failed to process guess",
      message: "Internal server error",
    });
  }
});

// GET /api/game/round/:id - Get round details
router.get("/round/:id", async (req, res) => {
  try {
    const roundId = parseInt(req.params.id);

    if (isNaN(roundId)) {
      return res.status(400).json({
        error: "Invalid round ID",
        message: "Round ID must be a number",
      });
    }

    const round = await gameService.getRoundById(roundId);

    if (!round) {
      return res.status(404).json({
        error: "Round not found",
        message: `No round found with ID ${roundId}`,
      });
    }

    res.json({
      success: true,
      data: round,
    });
  } catch (error) {
    console.error("❌ Game Route: /round/:id failed:", error.message);
    res.status(500).json({
      error: "Failed to get round",
      message: "Internal server error",
    });
  }
});

module.exports = router;
