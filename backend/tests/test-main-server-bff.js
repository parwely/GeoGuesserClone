// Test BFF endpoints on main server (port 3000)
const http = require("http");

async function makeRequest(method, path, body = null) {
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

async function testMainServer() {
  console.log("🧪 Testing BFF endpoints on main server (port 3000)...");

  try {
    // Test health first
    console.log("0️⃣ Testing health endpoint");
    const healthResult = await makeRequest("GET", "/health");
    console.log(`Health Status: ${healthResult.status}`);

    if (healthResult.status === 200) {
      console.log("✅ Server is healthy");

      // Test new round
      console.log("\n1️⃣ Testing GET /api/games/newRound");
      const newRoundResult = await makeRequest("GET", "/api/games/newRound");
      console.log(`Status: ${newRoundResult.status}`);

      if (newRoundResult.status === 200 && newRoundResult.data) {
        console.log("Response:", JSON.stringify(newRoundResult.data, null, 2));
        const roundId = newRoundResult.data.id;
        console.log(`✅ Round created: ${roundId}`);

        // Test guess
        console.log("\n2️⃣ Testing POST /api/games/guess");
        const guessData = {
          roundId: roundId,
          guessLat: newRoundResult.data.lat + 0.1, // Close guess
          guessLng: newRoundResult.data.lng + 0.1,
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
          console.log(
            `✅ Guess processed: ${guessResult.data.distanceMeters}m = ${guessResult.data.score} points`
          );
        } else {
          console.log("❌ Guess submission failed:", guessResult.data);
        }
      } else {
        console.log("❌ New round creation failed:", newRoundResult.data);
      }
    } else {
      console.log("❌ Server health check failed");
    }
  } catch (error) {
    console.error("❌ Test failed:", error.message);
  }

  console.log("\n🎉 Main server BFF tests completed!");
}

// Wait a bit for server to be ready, then test
setTimeout(testMainServer, 2000);
