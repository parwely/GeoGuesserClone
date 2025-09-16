const http = require("http");

function makeRequest(url) {
  return new Promise((resolve, reject) => {
    const req = http.get(url, (res) => {
      let data = "";
      res.on("data", (chunk) => {
        data += chunk;
      });
      res.on("end", () => {
        try {
          const jsonData = JSON.parse(data);
          resolve(jsonData);
        } catch (error) {
          resolve(data);
        }
      });
    });
    req.on("error", reject);
  });
}

async function testLocationSystem() {
  try {
    console.log("🔍 Testing guaranteed Street View location system...\n");

    // Test locations endpoint
    console.log("1. Testing locations endpoint...");
    const locations = await makeRequest("http://localhost:3000/api/locations");

    if (Array.isArray(locations)) {
      console.log(`✅ Found ${locations.length} locations`);
      console.log("\nFirst 5 locations:");
      locations.slice(0, 5).forEach((location, index) => {
        console.log(`${index + 1}. ${location.name} (${location.country})`);
        console.log(
          `   Coordinates: ${location.coordinates.latitude}, ${location.coordinates.longitude}`
        );
      });

      // Test Street View for Times Square (should be first)
      if (locations.length > 0) {
        const firstLocation = locations[0];
        console.log(`\n2. Testing Street View for ${firstLocation.name}...`);

        const streetViewData = await makeRequest(
          `http://localhost:3000/api/locations/${firstLocation.id}/streetview`
        );
        console.log("Street View response:");
        console.log("- Static available:", streetViewData.static?.available);
        if (streetViewData.static?.url) {
          console.log(
            "- Static URL exists:",
            streetViewData.static.url.includes("maps.googleapis.com")
          );
        }
        console.log(
          "- Mobile fallback used:",
          streetViewData.fallbackUsed || false
        );
      }
    } else {
      console.log("❌ Unexpected response:", typeof locations);
    }

    console.log("\n🎉 Basic location test complete!");
  } catch (error) {
    console.error("❌ Test failed:", error.message);
  }
}

testLocationSystem();
