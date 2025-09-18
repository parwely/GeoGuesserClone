// Test gameService functions
const gameService = require("./src/services/gameService");
const database = require("./src/database/connection");

async function testGameService() {
  console.log("🎮 Testing gameService functions...");

  try {
    await database.connect();
    console.log("✅ Database connected");

    // Test 1: Get random game location
    console.log("\n1. Testing getRandomGameLocation...");
    const location = await gameService.getRandomGameLocation();

    if (location) {
      console.log("✅ Got random location:", {
        id: location.id,
        name: location.name,
        country: location.country,
        hasCoords: !!location.coordinates,
        hasPanoId: !!location.pano_id,
      });

      // Test 2: Create round
      console.log("\n2. Testing createRound...");
      const round = await gameService.createRound(location.id, null);
      console.log("✅ Round created:", {
        id: round.id,
        created_at: round.created_at,
      });

      // Test 3: Get round by ID
      console.log("\n3. Testing getRoundById...");
      const roundDetails = await gameService.getRoundById(round.id);
      console.log("✅ Round details retrieved:", {
        id: roundDetails.id,
        status: roundDetails.status,
        locationName: roundDetails.location.name,
      });

      // Test 4: Process guess
      console.log("\n4. Testing processGuess...");
      const guessResult = await gameService.processGuess({
        roundId: round.id,
        guessLat: location.coordinates.latitude + 0.1, // Slightly off
        guessLng: location.coordinates.longitude + 0.1,
        userId: null,
      });

      console.log("✅ Guess processed:", {
        distance: guessResult.distanceMeters,
        score: guessResult.score,
        maxScore: guessResult.maxPossibleScore,
      });

      console.log("\n🎉 All gameService tests passed!");
    } else {
      console.log("❌ No location returned from getRandomGameLocation");
    }
  } catch (error) {
    console.error("❌ GameService test failed:", error.message);
    console.error("Stack:", error.stack);
  } finally {
    process.exit(0);
  }
}

testGameService();
