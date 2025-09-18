// Test Street View Check Endpoint für Android App
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

async function testStreetViewCheck() {
  console.log("🔍 Testing Street View Check Endpoint");
  console.log("=".repeat(50));

  try {
    // Test 1: Get a new round first to get a location ID
    console.log("1️⃣ Getting new round to get location ID...");
    const newRoundResult = await makeRequest("GET", "/api/game/newRound");

    if (newRoundResult.status !== 200) {
      console.log("❌ Failed to get new round for testing");
      return;
    }

    console.log(`✅ Got round ${newRoundResult.data.id} with location data`);
    console.log(
      `   Location: ${newRoundResult.data.lat}, ${newRoundResult.data.lng}`
    );

    // Test 2: Check Street View availability using the Android path
    const locationId = 24; // Test with the ID from your error message
    console.log(`\n2️⃣ Testing Street View check for location ${locationId}`);
    const checkResult = await makeRequest(
      "GET",
      `/api/game/streetview/check/${locationId}`
    );

    console.log(`Status: ${checkResult.status}`);

    if (checkResult.status === 200) {
      console.log("✅ Street View check successful!");
      console.log(`   Available: ${checkResult.data.available}`);
      console.log(
        `   Location: ${checkResult.data.name}, ${checkResult.data.country}`
      );
      console.log(
        `   Coordinates: ${checkResult.data.lat}, ${checkResult.data.lng}`
      );
      console.log(`   Pano ID: ${checkResult.data.pano_id}`);
      console.log(`   Cached: ${checkResult.data.cached}`);
    } else if (checkResult.status === 404) {
      console.log(`❌ Location ${locationId} not found in database`);
      console.log("Response:", checkResult.data);
    } else {
      console.log("❌ Street View check failed");
      console.log("Response:", checkResult.data);
    }

    // Test 3: Test with an existing location from the new round
    console.log(`\n3️⃣ Testing with a known existing location...`);

    // Get location ID from database for testing
    const testLocationResult = await makeRequest(
      "GET",
      "/api/game/streetview/check/1"
    );
    console.log(`Test location 1 status: ${testLocationResult.status}`);

    if (testLocationResult.status === 200) {
      console.log("✅ Test location check successful!");
      console.log(`   Available: ${testLocationResult.data.available}`);
      console.log(`   Name: ${testLocationResult.data.name}`);
    }

    console.log("\n🎉 Street View check endpoint test completed!");
    console.log("\n📱 Android app can now use:");
    console.log("   GET /api/game/streetview/check/{locationId}");
    console.log("   This will prevent Street View timeout issues!");
  } catch (error) {
    console.error("❌ Test failed:", error.message);
  }
}

// Wait for server to be ready, then test
setTimeout(testStreetViewCheck, 2000);
