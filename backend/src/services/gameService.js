const database = require("../database/connection");
const streetViewService = require("./streetViewService");

// Import fetch for Node.js (if using Node < 18, install node-fetch)
const fetch = globalThis.fetch || require("node-fetch");

/**
 * Game Service for BFF Pattern
 * Handles game logic, location selection, scoring, and round management
 * Server handles game state, client handles Street View rendering with Maps SDK
 */

// Maximum score for a perfect guess (0 meters)
const MAX_SCORE = 5000;

/**
 * Get a random location with Street View coverage for game
 * @returns {Object} Location object with coordinates, name, country, pano_id
 */
async function getRandomGameLocation() {
  try {
    console.log("🎲 GameService: Getting random game location");

    // Get a random location with Street View coverage
    // Prefer locations that already have validated pano_id
    const result = await database.query(`
      SELECT l.id, l.name, l.country, l.coordinates, l.has_pano, l.pano_id
      FROM locations l 
      WHERE l.has_pano IS NOT FALSE 
        AND ST_IsValid(l.coordinates)
      ORDER BY RANDOM() 
      LIMIT 1
    `);

    if (result.rows.length === 0) {
      console.warn(
        "⚠️ GameService: No locations found with Street View coverage"
      );
      return null;
    }

    const location = result.rows[0];

    // Convert PostGIS point to lat/lng
    const coordsResult = await database.query(
      `
      SELECT ST_Y($1) as latitude, ST_X($1) as longitude
    `,
      [location.coordinates]
    );

    location.coordinates = {
      latitude: parseFloat(coordsResult.rows[0].latitude),
      longitude: parseFloat(coordsResult.rows[0].longitude),
    };

    // If we don't have a pano_id, try to validate and get one
    if (!location.pano_id) {
      try {
        const validation = await validateStreetViewLocation(
          location.coordinates.latitude,
          location.coordinates.longitude
        );

        if (validation.available) {
          location.pano_id = validation.pano_id;

          // Update the location record with validation results
          await database.query(
            `
            UPDATE locations 
            SET has_pano = $1, pano_id = $2, last_checked = NOW()
            WHERE id = $3
          `,
            [true, validation.pano_id, location.id]
          );

          console.log(
            `✅ GameService: Validated and updated location ${location.id} with pano_id: ${validation.pano_id}`
          );
        } else {
          // Mark as no Street View and try again
          await database.query(
            `
            UPDATE locations 
            SET has_pano = FALSE, last_checked = NOW()
            WHERE id = $1
          `,
            [location.id]
          );

          console.warn(
            `❌ GameService: Location ${location.id} has no Street View, marked as unavailable`
          );
          return getRandomGameLocation(); // Retry with different location
        }
      } catch (validationError) {
        console.error(
          "❌ GameService: Street View validation failed:",
          validationError.message
        );
        // Continue with location anyway, client will handle if no Street View
      }
    }

    console.log(
      `✅ GameService: Selected location: ${location.name}, ${location.country}`
    );
    return location;
  } catch (error) {
    console.error(
      "❌ GameService: getRandomGameLocation failed:",
      error.message
    );
    throw error;
  }
}

/**
 * Create a new game round
 * @param {number} locationId - ID of the location for this round
 * @param {number|null} userId - Optional user ID
 * @returns {Object} Round object with id and creation timestamp
 */
async function createRound(locationId, userId = null) {
  try {
    console.log(
      `🆕 GameService: Creating round for location ${locationId}, user ${userId}`
    );

    const result = await database.query(
      `
      INSERT INTO rounds (location_id, user_id, created_at, status)
      VALUES ($1, $2, NOW(), 'active')
      RETURNING id, created_at
    `,
      [locationId, userId]
    );

    const round = result.rows[0];
    console.log(`✅ GameService: Created round ${round.id}`);

    return round;
  } catch (error) {
    console.error("❌ GameService: createRound failed:", error.message);
    throw error;
  }
}

/**
 * Process a guess for a round
 * @param {Object} guessData - {roundId, guessLat, guessLng, userId}
 * @returns {Object} Result with distance, score, and actual location
 */
async function processGuess(guessData) {
  try {
    const { roundId, guessLat, guessLng, userId } = guessData;

    console.log(`🎯 GameService: Processing guess for round ${roundId}`);

    // Get round and location data
    const roundResult = await database.query(
      `
      SELECT r.id, r.location_id, r.status, r.user_id,
             l.name, l.country, l.coordinates
      FROM rounds r
      JOIN locations l ON r.location_id = l.id
      WHERE r.id = $1
    `,
      [roundId]
    );

    if (roundResult.rows.length === 0) {
      throw new Error("Round not found");
    }

    const round = roundResult.rows[0];

    if (round.status === "completed") {
      throw new Error("Round already completed");
    }

    // Get actual location coordinates
    const coordsResult = await database.query(
      `
      SELECT ST_Y($1) as latitude, ST_X($1) as longitude
    `,
      [round.coordinates]
    );

    const actualLocation = {
      latitude: parseFloat(coordsResult.rows[0].latitude),
      longitude: parseFloat(coordsResult.rows[0].longitude),
      name: round.name,
      country: round.country,
    };

    // Calculate distance using Haversine formula
    const distanceMeters = calculateDistance(
      guessLat,
      guessLng,
      actualLocation.latitude,
      actualLocation.longitude
    );

    // Calculate score based on distance
    const score = calculateScore(distanceMeters);

    // Store the guess
    await database.query(
      `
      INSERT INTO guesses (round_id, user_id, guess_lat, guess_lng, 
                          actual_lat, actual_lng, distance_meters, score, created_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW())
    `,
      [
        roundId,
        userId,
        guessLat,
        guessLng,
        actualLocation.latitude,
        actualLocation.longitude,
        Math.round(distanceMeters), // Convert to integer
        score,
      ]
    );

    // Mark round as completed
    await database.query(
      `
      UPDATE rounds 
      SET status = 'completed', completed_at = NOW()
      WHERE id = $1
    `,
      [roundId]
    );

    console.log(
      `✅ GameService: Guess processed - Distance: ${distanceMeters}m, Score: ${score}/${MAX_SCORE}`
    );

    return {
      distanceMeters: Math.round(distanceMeters),
      score: score,
      actualLocation: actualLocation,
      maxPossibleScore: MAX_SCORE,
    };
  } catch (error) {
    console.error("❌ GameService: processGuess failed:", error.message);
    throw error;
  }
}

/**
 * Get round details by ID
 * @param {number} roundId - Round ID
 * @returns {Object} Round details with location and guess data
 */
async function getRoundById(roundId) {
  try {
    console.log(`📋 GameService: Getting round ${roundId} details`);

    const result = await database.query(
      `
      SELECT r.id, r.status, r.created_at, r.completed_at,
             l.name as location_name, l.country as location_country,
             ST_Y(l.coordinates) as actual_lat, ST_X(l.coordinates) as actual_lng,
             g.guess_lat, g.guess_lng, g.distance_meters, g.score
      FROM rounds r
      JOIN locations l ON r.location_id = l.id
      LEFT JOIN guesses g ON r.id = g.round_id
      WHERE r.id = $1
    `,
      [roundId]
    );

    if (result.rows.length === 0) {
      return null;
    }

    const round = result.rows[0];

    return {
      id: round.id,
      status: round.status,
      created_at: round.created_at,
      completed_at: round.completed_at,
      location: {
        name: round.location_name,
        country: round.location_country,
        lat: parseFloat(round.actual_lat),
        lng: parseFloat(round.actual_lng),
      },
      guess: round.guess_lat
        ? {
            lat: parseFloat(round.guess_lat),
            lng: parseFloat(round.guess_lng),
            distance_meters: round.distance_meters,
            score: round.score,
          }
        : null,
    };
  } catch (error) {
    console.error("❌ GameService: getRoundById failed:", error.message);
    throw error;
  }
}

/**
 * Validate Street View availability for a location using Metadata API
 * @param {number} lat - Latitude
 * @param {number} lng - Longitude
 * @returns {Object} {available: boolean, pano_id?: string}
 */
async function validateStreetViewLocation(lat, lng) {
  try {
    console.log(`🔍 GameService: Validating Street View for ${lat}, ${lng}`);

    const serverApiKey = process.env.SERVER_GOOGLE_KEY;
    if (!serverApiKey) {
      console.warn(
        "⚠️ GameService: SERVER_GOOGLE_KEY not found, using placeholder"
      );
      return {
        available: true,
        pano_id: `pano_${lat}_${lng}`.replace(/[.-]/g, "_"),
      };
    }

    const metadataUrl = `https://maps.googleapis.com/maps/api/streetview/metadata?location=${lat},${lng}&key=${serverApiKey}`;

    const response = await fetch(metadataUrl);
    const data = await response.json();

    if (data.status === "OK") {
      console.log(
        `✅ GameService: Street View available - pano_id: ${data.pano_id}`
      );
      return {
        available: true,
        pano_id: data.pano_id,
      };
    } else {
      console.log(
        `❌ GameService: Street View not available - status: ${data.status}`
      );
      return { available: false };
    }
  } catch (error) {
    console.error(
      "❌ GameService: Street View validation failed:",
      error.message
    );
    return { available: false };
  }
}

/**
 * Calculate distance between two points using Haversine formula
 * @param {number} lat1 - First latitude
 * @param {number} lng1 - First longitude
 * @param {number} lat2 - Second latitude
 * @param {number} lng2 - Second longitude
 * @returns {number} Distance in meters
 */
function calculateDistance(lat1, lng1, lat2, lng2) {
  const R = 6371000; // Earth's radius in meters
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

/**
 * Calculate score based on distance
 * Uses exponential decay: closer guesses get disproportionately more points
 * @param {number} distanceMeters - Distance in meters
 * @returns {number} Score (0 to MAX_SCORE)
 */
function calculateScore(distanceMeters) {
  if (distanceMeters === 0) return MAX_SCORE;

  // Exponential decay with distance
  // At 1km = ~3679 points, 5km = ~1839 points, 25km = ~368 points
  const score = Math.round(MAX_SCORE * Math.exp(-distanceMeters / 2000));
  return Math.max(0, Math.min(MAX_SCORE, score));
}

module.exports = {
  getRandomGameLocation,
  createRound,
  processGuess,
  getRoundById,
  validateStreetViewLocation,
  calculateDistance,
  calculateScore,
  MAX_SCORE,
};
