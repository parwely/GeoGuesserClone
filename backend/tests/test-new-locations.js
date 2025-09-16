const fetch = require("node-fetch");

async function testNewLocationSystem() {
  try {
    console.log("🔍 Testing new guaranteed Street View location system...\n");

    // Test locations list
    console.log("1. Testing locations endpoint...");
    const locationsResponse = await fetch(
      "http://localhost:3000/api/locations"
    );
    const locations = await locationsResponse.json();

    console.log(`✅ Found ${locations.length} locations`);
    console.log("\nFirst 5 locations:");
    locations.slice(0, 5).forEach((location, index) => {
      console.log(`${index + 1}. ${location.name} (${location.country})`);
      console.log(
        `   Coordinates: ${location.coordinates.latitude}, ${location.coordinates.longitude}`
      );
      console.log(
        `   Category: ${location.category}, Difficulty: ${location.difficulty}`
      );
    });

    // Test Street View endpoint for Times Square (should be first location)
    if (locations.length > 0) {
      const timesSquare = locations[0];
      console.log(`\n2. Testing Street View for ${timesSquare.name}...`);

      const streetViewResponse = await fetch(
        `http://localhost:3000/api/locations/${timesSquare.id}/streetview`
      );
      const streetViewData = await streetViewResponse.json();

      console.log("Street View response:");
      console.log("- Static available:", streetViewData.static?.available);
      if (streetViewData.static?.url) {
        console.log(
          "- Static URL:",
          streetViewData.static.url.substring(0, 100) + "..."
        );
      }
      console.log(
        "- Interactive available:",
        streetViewData.interactive?.available
      );
      console.log("- Mobile fallback used:", streetViewData.fallbackUsed);

      // Test the reliable endpoint
      console.log(
        `\n3. Testing reliable Street View endpoint for ${timesSquare.name}...`
      );
      const reliableResponse = await fetch(
        `http://localhost:3000/api/locations/${timesSquare.id}/streetview/reliable`
      );
      const reliableData = await reliableResponse.json();

      console.log("Reliable Street View response:");
      console.log("- Coverage validated:", reliableData.validated);
      console.log("- Has coverage:", reliableData.hasCoverage);
      console.log("- Status:", reliableData.status);
      if (reliableData.panoId) {
        console.log("- Panorama ID:", reliableData.panoId);
      }
    }

    console.log("\n🎉 Location system test complete!");
  } catch (error) {
    console.error("❌ Test failed:", error.message);
  }
}

testNewLocationSystem();
