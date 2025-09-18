// Test Android guess endpoint compatibility
const http = require("http");

async function makeRequest(method, path, body = null) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "localhost",
      port: 3000,
      path: path,
      method: method,
      headers: { "Content-Type": "application/json" },
    };

    const req = http.request(options, (res) => {
      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => {
        try {
          resolve({
            status: res.statusCode,
            data: data ? JSON.parse(data) : null,
          });
        } catch (e) {
          resolve({
            status: res.statusCode,
            data: data,
          });
        }
      });
    });

    req.on("error", reject);
    req.setTimeout(10000, () => {
      req.destroy();
      reject(new Error("Request timeout"));
    });

    if (body) {
      req.write(JSON.stringify(body));
    }

    req.end();
  });
}

async function testAndroidGuessWorkflow() {
  console.log("🎮 Testing Complete Android App Workflow");
  console.log("=".repeat(50));

  try {
    // Step 1: Get new round using Android path
    console.log("1️⃣ Getting new round via /api/game/newRound");
    const newRoundResult = await makeRequest("GET", "/api/game/newRound");

    if (newRoundResult.status !== 200) {
      console.log("❌ Failed to get new round");
      return;
    }

    const roundData = newRoundResult.data;
    console.log(`✅ Round created: ${roundData.id}`);
    console.log(`   Location: ${roundData.lat}, ${roundData.lng}`);
    console.log(`   Pano ID: ${roundData.pano_id}`);
    console.log(`   Hint: ${roundData.location_hint}`);

    // Step 2: Submit guess using Android path
    console.log("\n2️⃣ Submitting guess via /api/game/guess");
    const guessData = {
      roundId: roundData.id,
      guessLat: roundData.lat + 0.01, // Close guess
      guessLng: roundData.lng + 0.01,
      userId: null,
    };

    const guessResult = await makeRequest("POST", "/api/game/guess", guessData);

    if (guessResult.status !== 200) {
      console.log("❌ Failed to submit guess");
      console.log("Response:", guessResult.data);
      return;
    }

    const scoreData = guessResult.data;
    console.log(`✅ Guess processed successfully!`);
    console.log(`   Distance: ${scoreData.distanceMeters}m`);
    console.log(`   Score: ${scoreData.score}/${scoreData.maxPossibleScore}`);
    console.log(
      `   Actual location: ${scoreData.actual.name}, ${scoreData.actual.country}`
    );
    console.log(
      `   Actual coords: ${scoreData.actual.lat}, ${scoreData.actual.lng}`
    );

    console.log("\n🎉 Complete Android workflow test successful!");
    console.log("\n📱 Android app should now work with these endpoints:");
    console.log("   GET  /api/game/newRound");
    console.log("   POST /api/game/guess");
  } catch (error) {
    console.error("❌ Test failed:", error.message);
  }
}

// Wait for server to be ready, then test
setTimeout(testAndroidGuessWorkflow, 2000);
