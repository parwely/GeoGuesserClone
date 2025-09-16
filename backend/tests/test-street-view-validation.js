require("dotenv").config();
const streetViewService = require("../src/services/streetViewService");

async function testStreetViewValidation() {
  console.log("🔍 Testing Street View coverage validation...\n");

  // Test individual validation
  console.log("1. Testing individual location validation:");
  const timesSquare = await streetViewService.validateStreetViewCoverage(
    40.758896,
    -73.98513
  );
  console.log("Times Square result:", timesSquare);

  const badLocation = await streetViewService.validateStreetViewCoverage(
    36.2578,
    136.9061
  );
  console.log("Bad location (old rural Japan) result:", badLocation);

  // Test multiple locations validation
  console.log("\n2. Testing multiple locations validation:");
  const testLocations = [
    { lat: 40.758896, lng: -73.98513, name: "Times Square" },
    { lat: 51.510067, lng: -0.133869, name: "Piccadilly Circus" },
    { lat: 35.659515, lng: 139.70031, name: "Shibuya Crossing" },
    { lat: 36.2578, lng: 136.9061, name: "Rural Japan (should fail)" },
  ];

  const validationResults = await streetViewService.validateMultipleLocations(
    testLocations
  );

  console.log("\nValidation Summary:");
  validationResults.forEach((result) => {
    const status = result.validation.hasCoverage ? "✅" : "❌";
    console.log(
      `${status} ${result.location.name}: ${result.validation.status}`
    );
    if (result.validation.panoId) {
      console.log(`   Panorama ID: ${result.validation.panoId}`);
    }
  });

  console.log("\n🎉 Street View validation test complete!");
}

testStreetViewValidation().catch(console.error);
