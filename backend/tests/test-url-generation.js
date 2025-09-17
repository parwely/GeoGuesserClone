/**
 * Test der korrigierten Street View URL-Generierung
 */

// Mock API Key setzen
process.env.GOOGLE_STREETVIEW_API_KEY = "TEST_API_KEY";

const streetViewService = require("../src/services/streetViewService");

console.log("🧪 Testing Corrected Street View URL Generation\n");

async function testCorrectedURLGeneration() {
  try {
    // Test coordinates (Berlin, Germany)
    const testLat = 52.52;
    const testLng = 13.405;

    console.log(`📍 Testing with coordinates: ${testLat}, ${testLng}\n`);

    // Test 1: Normale heading als Nummer
    console.log("1️⃣ Testing normal numeric heading...");
    try {
      const url1 = streetViewService.generateInteractiveStreetViewUrl(
        testLat,
        testLng,
        233,
        0,
        90
      );
      console.log("✅ URL with heading 233:");
      console.log("   ", url1);
      console.log(
        "   Heading in URL:",
        url1.match(/heading=(\d+)/)?.[1] || "NOT FOUND"
      );
    } catch (error) {
      console.log("❌ Error:", error.message);
    }

    console.log();

    // Test 2: Objekt als heading (sollte korrigiert werden)
    console.log("2️⃣ Testing object heading (should be sanitized)...");
    try {
      const objectHeading = {
        heading: 180,
        zoom: null,
        fallbackType: "static",
      };
      const url2 = streetViewService.generateInteractiveStreetViewUrl(
        testLat,
        testLng,
        objectHeading,
        0,
        90
      );
      console.log("✅ URL with object heading:");
      console.log("   ", url2);
      console.log(
        "   Heading in URL:",
        url2.match(/heading=(\d+)/)?.[1] || "NOT FOUND"
      );
    } catch (error) {
      console.log("❌ Error:", error.message);
    }

    console.log();

    // Test 3: String als heading
    console.log("3️⃣ Testing string heading...");
    try {
      const url3 = streetViewService.generateInteractiveStreetViewUrl(
        testLat,
        testLng,
        "90",
        0,
        90
      );
      console.log('✅ URL with string heading "90":');
      console.log("   ", url3);
      console.log(
        "   Heading in URL:",
        url3.match(/heading=(\d+)/)?.[1] || "NOT FOUND"
      );
    } catch (error) {
      console.log("❌ Error:", error.message);
    }

    console.log();

    // Test 4: generateStreetViewResponse (full response)
    console.log("4️⃣ Testing full Street View response...");
    try {
      const response = streetViewService.generateStreetViewResponse(
        testLat,
        testLng,
        270,
        0,
        90,
        true
      );
      console.log("✅ Full response generated:");
      console.log("   embedUrl:", response.embedUrl.substring(0, 100) + "...");
      console.log(
        "   Heading in embedUrl:",
        response.embedUrl.match(/heading=(\d+)/)?.[1] || "NOT FOUND"
      );
      console.log("   Has nativeConfig:", !!response.nativeConfig);
      console.log("   nativeConfig type:", typeof response.nativeConfig);

      // Parse nativeConfig to check content
      if (response.nativeConfig) {
        try {
          const config = JSON.parse(response.nativeConfig);
          console.log("   nativeConfig.heading:", config.config.pov.heading);
        } catch (e) {
          console.log("   nativeConfig parse error:", e.message);
        }
      }
    } catch (error) {
      console.log("❌ Error:", error.message);
    }

    console.log();

    // Test 5: Sanitization methods directly
    console.log("5️⃣ Testing sanitization methods...");
    console.log(
      "   sanitizeHeading(233):",
      streetViewService.sanitizeHeading(233)
    );
    console.log(
      '   sanitizeHeading("45"):',
      streetViewService.sanitizeHeading("45")
    );
    console.log(
      "   sanitizeHeading({heading: 90}):",
      streetViewService.sanitizeHeading({ heading: 90 })
    );
    console.log(
      "   sanitizeHeading(null):",
      streetViewService.sanitizeHeading(null)
    );
    console.log(
      "   sanitizeHeading(undefined):",
      streetViewService.sanitizeHeading(undefined)
    );

    console.log("\n🎉 URL Generation testing completed!");
    console.log("\n📝 Results Summary:");
    console.log("- Numeric headings should work correctly");
    console.log("- Object headings should be sanitized to numbers");
    console.log("- String headings should be parsed to numbers");
    console.log('- URLs should NOT contain "[object Object]"');
  } catch (error) {
    console.error("❌ Testing failed:", error.message);
  }
}

testCorrectedURLGeneration();
