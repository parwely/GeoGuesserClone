/**
 * Manual test of the interactive Street View URLs
 * This script simulates what the new endpoints should return
 */

const streetViewService = require("../src/services/streetViewService");

console.log("🧪 Manual Testing of Interactive Street View Service\n");

async function testStreetViewService() {
  try {
    // Test coordinates (Berlin, Germany)
    const testLat = 52.52;
    const testLng = 13.405;

    console.log(`📍 Testing with coordinates: ${testLat}, ${testLng}\n`);

    // Test 1: Interactive Street View URL
    console.log("1️⃣ Testing generateInteractiveStreetViewUrl...");
    try {
      const interactiveResult =
        await streetViewService.generateInteractiveStreetViewUrl(
          testLat,
          testLng,
          { heading: 90, zoom: 1, enableNavigation: true, quality: "high" }
        );
      console.log("✅ Interactive URL generated:");
      console.log(
        "   Embed URL:",
        interactiveResult.embedUrl.substring(0, 100) + "..."
      );
      console.log(
        "   Navigation enabled:",
        interactiveResult.navigationEnabled
      );
      console.log(
        "   Controls available:",
        Object.keys(interactiveResult.controls).join(", ")
      );
    } catch (error) {
      console.log("❌ Interactive URL generation failed:", error.message);
    }

    console.log();

    // Test 2: Street View Response
    console.log("2️⃣ Testing generateStreetViewResponse...");
    try {
      const responseResult = await streetViewService.generateStreetViewResponse(
        testLat,
        testLng,
        { heading: 180, zoom: 1.2, fallbackType: "static" }
      );
      console.log("✅ Street View Response generated:");
      console.log("   Type:", responseResult.type);
      console.log("   Has embed URL:", !!responseResult.embedUrl);
      console.log("   Has static fallback:", !!responseResult.staticFallback);
    } catch (error) {
      console.log("❌ Street View Response generation failed:", error.message);
    }

    console.log();

    // Test 3: Navigation calculation
    console.log("3️⃣ Testing calculateNavigationPosition...");
    try {
      const navResult = streetViewService.calculateNavigationPosition(
        testLat,
        testLng,
        "north",
        100,
        1.5
      );
      console.log("✅ Navigation position calculated:");
      console.log("   New latitude:", navResult.latitude.toFixed(6));
      console.log("   New longitude:", navResult.longitude.toFixed(6));
      console.log("   New heading:", navResult.heading);
    } catch (error) {
      console.log("❌ Navigation calculation failed:", error.message);
    }

    console.log();

    // Test 4: Legacy static URL (should still work)
    console.log("4️⃣ Testing legacy generateUrl...");
    try {
      const staticUrl = streetViewService.generateUrl(testLat, testLng, 270);
      console.log("✅ Static URL generated:");
      console.log("   URL:", staticUrl.substring(0, 100) + "...");
      console.log("   Is static API:", staticUrl.includes("googleapis.com"));
    } catch (error) {
      console.log("❌ Static URL generation failed:", error.message);
    }

    console.log("\n🎉 Manual service testing completed!");
    console.log("\n📝 Summary:");
    console.log(
      "- Interactive URLs use Google Maps Embed API with navigation controls"
    );
    console.log("- Static URLs maintain backward compatibility");
    console.log("- Navigation calculation provides new positions for movement");
    console.log("- Service supports both interactive and static modes");
  } catch (error) {
    console.error("❌ Service testing failed:", error.message);
  }
}

testStreetViewService();
