// Test script for BFF game endpoints
const http = require("http");

async function testEndpoint(path, method = "GET", body = null) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "localhost",
      port: 3000,
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
            headers: res.headers,
            data: data ? JSON.parse(data) : null,
          };
          resolve(result);
        } catch (e) {
          resolve({
            status: res.statusCode,
            headers: res.headers,
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

async function runTests() {
  console.log("🧪 Testing BFF Game Endpoints\n");

  try {
    // Test 1: Get new round
    console.log("1️⃣ Testing GET /api/games/newRound");
    const newRoundResult = await testEndpoint("/api/games/newRound");
    console.log(`Status: ${newRoundResult.status}`);
    console.log("Response:", JSON.stringify(newRoundResult.data, null, 2));

    if (newRoundResult.status === 200 && newRoundResult.data.id) {
      const roundId = newRoundResult.data.id;
      console.log(`✅ Round created: ${roundId}\n`);

      // Test 2: Submit guess
      console.log("2️⃣ Testing POST /api/games/guess");
      const guessData = {
        roundId: roundId,
        guessLat: 40.7128,
        guessLng: -74.006,
        userId: null,
      };

      const guessResult = await testEndpoint(
        "/api/games/guess",
        "POST",
        guessData
      );
      console.log(`Status: ${guessResult.status}`);
      console.log("Response:", JSON.stringify(guessResult.data, null, 2));

      if (guessResult.status === 200) {
        console.log("✅ Guess processed successfully\n");

        // Test 3: Get round details
        console.log("3️⃣ Testing GET /api/games/round/:id");
        const roundResult = await testEndpoint(`/api/games/round/${roundId}`);
        console.log(`Status: ${roundResult.status}`);
        console.log("Response:", JSON.stringify(roundResult.data, null, 2));

        if (roundResult.status === 200) {
          console.log("✅ Round details retrieved successfully");
        } else {
          console.log("❌ Round details test failed");
        }
      } else {
        console.log("❌ Guess submission failed");
      }
    } else {
      console.log("❌ New round creation failed");
    }
  } catch (error) {
    console.error("❌ Test failed:", error.message);
  }
}

runTests()
  .then(() => {
    console.log("\n🎉 BFF endpoint tests completed");
  })
  .catch((error) => {
    console.error("\n💥 Test suite failed:", error.message);
  });
