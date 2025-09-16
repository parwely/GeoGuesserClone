/**
 * Simple test script for Street View API endpoints
 */

const http = require("http");

function makeRequest(path) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "localhost",
      port: 3000,
      path: path,
      method: "GET",
    };

    const req = http.request(options, (res) => {
      let data = "";
      res.on("data", (chunk) => {
        data += chunk;
      });
      res.on("end", () => {
        try {
          resolve({
            status: res.statusCode,
            data: JSON.parse(data),
          });
        } catch (e) {
          resolve({
            status: res.statusCode,
            data: data,
          });
        }
      });
    });

    req.on("error", (err) => {
      reject(err);
    });

    req.end();
  });
}

async function testEndpoints() {
  console.log("🧪 Testing Street View API Endpoints\n");

  try {
    // Test 1: Basic random location
    console.log("1️⃣ Testing basic random location...");
    const basicResponse = await makeRequest("/api/locations/random?count=1");
    console.log("✅ Status:", basicResponse.status);

    if (
      basicResponse.data.success &&
      basicResponse.data.data.locations.length > 0
    ) {
      const locationId = basicResponse.data.data.locations[0].id;
      console.log("✅ Location ID:", locationId);

      // Test 2: Interactive Street View
      console.log("\n2️⃣ Testing interactive Street View...");
      const interactiveResponse = await makeRequest(
        `/api/locations/${locationId}/streetview?interactive=true`
      );
      console.log("✅ Status:", interactiveResponse.status);
      console.log(
        "✅ Has Street View data:",
        !!interactiveResponse.data.data?.streetView
      );
      console.log(
        "✅ Street View type:",
        interactiveResponse.data.data?.streetView?.type
      );

      // Test 3: Static Street View (backward compatibility)
      console.log(
        "\n3️⃣ Testing static Street View (backward compatibility)..."
      );
      const staticResponse = await makeRequest(
        `/api/locations/${locationId}/streetview?interactive=false`
      );
      console.log("✅ Status:", staticResponse.status);
      console.log(
        "✅ Has Street View URL:",
        !!staticResponse.data.data?.streetViewUrl
      );

      // Test 4: Random with Street View included
      console.log("\n4️⃣ Testing random location with Street View included...");
      const randomWithSVResponse = await makeRequest(
        "/api/locations/random?count=1&includeStreetView=true"
      );
      console.log("✅ Status:", randomWithSVResponse.status);
      console.log(
        "✅ Has Street View in location:",
        !!randomWithSVResponse.data.data?.locations[0]?.streetView
      );

      console.log("\n🎉 All basic tests passed!");
    } else {
      console.log("❌ Failed to get basic location data");
    }
  } catch (error) {
    console.error("❌ Test error:", error.message);
  }
}

testEndpoints();
