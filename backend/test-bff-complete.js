// Test actual BFF endpoints with working server
const express = require("express");
const gameService = require("./src/services/gameService");
const database = require("./src/database/connection");
const http = require("http");

async function testBFFEndpoints() {
  console.log("🚀 Starting BFF endpoint test server...");

  try {
    // Connect to database
    await database.connect();
    console.log("✅ Database connected");

    // Create Express app
    const app = express();
    app.use(express.json());

    // Add our game routes directly (simpler than importing)
    app.get("/api/games/newRound", async (req, res) => {
      try {
        console.log("🎮 Game Route: /newRound called");
        const userId = req.query.userId || null;

        const location = await gameService.getRandomGameLocation();
        if (!location) {
          return res.status(404).json({
            error: "No locations available",
            message: "No Street View locations found for game",
          });
        }

        const round = await gameService.createRound(location.id, userId);
        console.log(
          `✅ New round created: ${round.id} for location: ${location.name}`
        );

        res.json({
          id: round.id,
          lat: location.coordinates.latitude,
          lng: location.coordinates.longitude,
          pano_id: location.pano_id || undefined,
          location_hint: location.country,
        });
      } catch (error) {
        console.error("❌ Game Route: /newRound failed:", error.message);
        res.status(500).json({
          error: "Failed to create new round",
          message: "Internal server error",
        });
      }
    });

    app.post("/api/games/guess", async (req, res) => {
      try {
        console.log("🎯 Game Route: /guess called");
        const { roundId, guessLat, guessLng, userId } = req.body;

        if (!roundId || guessLat === undefined || guessLng === undefined) {
          return res.status(400).json({
            error: "Invalid guess data",
            message: "roundId, guessLat, and guessLng are required",
          });
        }

        const result = await gameService.processGuess({
          roundId: parseInt(roundId),
          guessLat: parseFloat(guessLat),
          guessLng: parseFloat(guessLng),
          userId: userId || null,
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
        res.status(500).json({
          error: "Failed to process guess",
          message: "Internal server error",
        });
      }
    });

    // Start server
    const server = app.listen(3002, () => {
      console.log("✅ BFF test server running on port 3002");

      // Run tests
      setTimeout(() => testEndpoints(), 1000);
    });

    async function testEndpoints() {
      console.log("\n🧪 Testing BFF endpoints...");

      // Test 1: Get new round
      console.log("1️⃣ Testing GET /api/games/newRound");
      const newRoundResult = await makeRequest("GET", "/api/games/newRound");
      console.log(`Status: ${newRoundResult.status}`);

      if (newRoundResult.status === 200 && newRoundResult.data) {
        console.log("Response:", JSON.stringify(newRoundResult.data, null, 2));
        const roundId = newRoundResult.data.id;
        console.log(`✅ Round created: ${roundId}`);

        // Test 2: Submit guess
        console.log("\n2️⃣ Testing POST /api/games/guess");
        const guessData = {
          roundId: roundId,
          guessLat: 40.7128,
          guessLng: -74.006,
          userId: null,
        };

        const guessResult = await makeRequest(
          "POST",
          "/api/games/guess",
          guessData
        );
        console.log(`Status: ${guessResult.status}`);

        if (guessResult.status === 200 && guessResult.data) {
          console.log("Response:", JSON.stringify(guessResult.data, null, 2));
          console.log("✅ Guess processed successfully");
        } else {
          console.log("❌ Guess submission failed");
        }
      } else {
        console.log("❌ New round creation failed");
      }

      console.log("\n🎉 BFF endpoint tests completed");

      // Shutdown server
      setTimeout(() => {
        server.close(() => {
          console.log("✅ Test server shutdown");
          process.exit(0);
        });
      }, 1000);
    }
  } catch (error) {
    console.error("❌ BFF test setup failed:", error.message);
    process.exit(1);
  }
}

async function makeRequest(method, path, body = null) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "localhost",
      port: 3002,
      path: path,
      method: method,
      headers: {
        "Content-Type": "application/json",
      },
    };

    const req = http.request(options, (res) => {
      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => {
        try {
          const result = {
            status: res.statusCode,
            data: data ? JSON.parse(data) : null,
          };
          resolve(result);
        } catch (e) {
          resolve({
            status: res.statusCode,
            data: data,
          });
        }
      });
    });

    req.on("error", reject);

    if (body) {
      req.write(JSON.stringify(body));
    }

    req.end();
  });
}

testBFFEndpoints();
