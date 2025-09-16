/**
 * Test script for the new interactive Street View API endpoints
 */

const axios = require("axios");

const BASE_URL = "http://localhost:3000";

async function testInteractiveStreetViewEndpoints() {
  console.log("🧪 Testing Interactive Street View API Endpoints\n");

  try {
    // Test 1: Get random location with interactive Street View
    console.log("1️⃣ Testing /api/locations/random?includeStreetView=true");
    const randomResponse = await axios.get(
      `${BASE_URL}/api/locations/random?includeStreetView=true`
    );
    console.log("✅ Random location with Street View:", {
      success: randomResponse.data.success,
      locationId: randomResponse.data.data.locations[0]?.id,
      hasStreetView: !!randomResponse.data.data.locations[0]?.streetView,
      streetViewType: randomResponse.data.data.locations[0]?.streetView?.type,
    });

    const locationId = randomResponse.data.data.locations[0]?.id;

    if (locationId) {
      // Test 2: Get interactive Street View for specific location
      console.log(
        "\n2️⃣ Testing /api/locations/:id/streetview?interactive=true"
      );
      const interactiveResponse = await axios.get(
        `${BASE_URL}/api/locations/${locationId}/streetview?interactive=true`
      );
      console.log("✅ Interactive Street View:", {
        success: interactiveResponse.data.success,
        streetViewType: interactiveResponse.data.data.streetView?.type,
        hasEmbedUrl: !!interactiveResponse.data.data.streetView?.embedUrl,
        hasStaticFallback:
          !!interactiveResponse.data.data.streetView?.staticFallback,
      });

      // Test 3: Get enhanced interactive Street View
      console.log("\n3️⃣ Testing /api/locations/:id/streetview/interactive");
      const enhancedResponse = await axios.get(
        `${BASE_URL}/api/locations/${locationId}/streetview/interactive`
      );
      console.log("✅ Enhanced Interactive Street View:", {
        success: enhancedResponse.data.success,
        hasEmbedUrl: !!enhancedResponse.data.data.interactive?.embedUrl,
        navigationEnabled:
          enhancedResponse.data.data.interactive?.navigationEnabled,
        quality: enhancedResponse.data.data.metadata?.quality,
      });

      // Test 4: Test navigation endpoint
      console.log("\n4️⃣ Testing POST /api/locations/streetview/navigate");
      const location = randomResponse.data.data.locations[0];
      const navigationData = {
        latitude: location.coordinates.latitude,
        longitude: location.coordinates.longitude,
        direction: "north",
        distance: 100,
        zoom: 1.5,
      };

      const navigationResponse = await axios.post(
        `${BASE_URL}/api/locations/streetview/navigate`,
        navigationData
      );
      console.log("✅ Street View Navigation:", {
        success: navigationResponse.data.success,
        originalLat:
          navigationResponse.data.data.navigation?.originalPosition.latitude,
        newLat: navigationResponse.data.data.navigation?.newPosition.latitude,
        direction: navigationResponse.data.data.navigation?.direction,
        hasStreetView: !!navigationResponse.data.data.streetView,
      });

      // Test 5: Test backward compatibility with static URLs
      console.log(
        "\n5️⃣ Testing backward compatibility /api/locations/:id/streetview?interactive=false"
      );
      const staticResponse = await axios.get(
        `${BASE_URL}/api/locations/${locationId}/streetview?interactive=false`
      );
      console.log("✅ Static Street View (Backward Compatibility):", {
        success: staticResponse.data.success,
        hasStreetViewUrl: !!staticResponse.data.data.streetViewUrl,
        urlType: staticResponse.data.data.streetViewUrl?.includes(
          "googleapis.com"
        )
          ? "static"
          : "unknown",
      });
    }

    console.log(
      "\n🎉 All interactive Street View tests completed successfully!"
    );
  } catch (error) {
    console.error("\n❌ Test failed:", error.response?.data || error.message);

    if (error.code === "ECONNREFUSED") {
      console.log("\n💡 Server may not be running. Start it with: npm start");
    }
  }
}

// Run the tests
testInteractiveStreetViewEndpoints();
