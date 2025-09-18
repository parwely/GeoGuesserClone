// Test both route paths to ensure Android compatibility
const http = require("http");

async function makeRequest(path) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "localhost",
      port: 3000,
      path: path,
      method: "GET",
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

    req.end();
  });
}

async function testRoutePaths() {
  console.log("🧪 Testing Android App Route Compatibility");
  console.log("=".repeat(50));

  try {
    // Test the route the Android app is calling
    console.log("1️⃣ Testing Android app route: /api/game/newRound");
    const androidResult = await makeRequest("/api/game/newRound");
    console.log(`Status: ${androidResult.status}`);

    if (androidResult.status === 200) {
      console.log("✅ Android route working!");
      console.log(`Round ID: ${androidResult.data.id}`);
      console.log(
        `Location: ${androidResult.data.lat}, ${androidResult.data.lng}`
      );
    } else {
      console.log("❌ Android route failed");
      console.log("Response:", androidResult.data);
    }

    // Test the original backend route
    console.log("\n2️⃣ Testing backend route: /api/games/newRound");
    const backendResult = await makeRequest("/api/games/newRound");
    console.log(`Status: ${backendResult.status}`);

    if (backendResult.status === 200) {
      console.log("✅ Backend route working!");
      console.log(`Round ID: ${backendResult.data.id}`);
    } else {
      console.log("❌ Backend route failed");
    }

    console.log("\n🎉 Route compatibility test completed!");
  } catch (error) {
    console.error("❌ Test failed:", error.message);
  }
}

// Wait for server to be ready, then test
setTimeout(testRoutePaths, 3000);
