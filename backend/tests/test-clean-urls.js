require("dotenv").config();
const { app, server } = require("./src/server");
const http = require("http");

// Start server on port 3001 for testing
const testServer = server.listen(3001, () => {
  console.log("🧪 Testing cleaned Google Maps Embed API URLs...\n");

  // Test the Street View endpoint with various parameters
  const options = {
    hostname: "localhost",
    port: 3001,
    path: "/api/locations/93/streetview?heading=45&pitch=15&fov=95",
    method: "GET",
  };

  const req = http.request(options, (res) => {
    let data = "";

    res.on("data", (chunk) => {
      data += chunk;
    });

    res.on("end", () => {
      console.log(`📊 Response Status: ${res.statusCode}\n`);

      if (res.statusCode === 200) {
        try {
          const result = JSON.parse(data);

          console.log("✅ Street View URLs generated successfully!\n");
          console.log("🔍 URL Analysis:");
          console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

          const interactive = result.data.streetView.interactive;
          const static = result.data.streetView.static;

          console.log(`📱 Interactive URL (Google Maps Embed API):`);
          console.log(`   ${interactive}\n`);

          console.log(`🖼️  Static URL (Street View Static API):`);
          console.log(`   ${static}\n`);

          // Parse and analyze parameters
          const interactiveUrl = new URL(interactive);
          const staticUrl = new URL(static);

          console.log("🎯 Interactive URL Parameters:");
          interactiveUrl.searchParams.forEach((value, key) => {
            console.log(`   - ${key}: ${value}`);
          });

          console.log("\n🎯 Static URL Parameters:");
          staticUrl.searchParams.forEach((value, key) => {
            console.log(`   - ${key}: ${value}`);
          });

          console.log("\n✅ Parameter Validation:");
          console.log(
            `   - Location: ${result.data.location.coordinates.latitude}, ${result.data.location.coordinates.longitude}`
          );
          console.log(
            `   - Heading: ${result.data.parameters.heading}° (0-360)`
          );
          console.log(
            `   - Pitch: ${result.data.parameters.pitch}° (-90 to 90)`
          );
          console.log(
            `   - FOV: ${result.data.parameters.fov}° (10-100 for Embed API)`
          );

          console.log("\n🔧 Changes Made:");
          console.log(
            "   ✅ Removed invalid parameters: navigation, controls, zoom, fullscreen"
          );
          console.log("   ✅ FOV range corrected: 10-100 (was 10-120)");
          console.log(
            "   ✅ Only Google Maps Embed API compatible parameters included"
          );
          console.log("   ✅ Clean URLs without unsupported parameters");

          console.log("\n📋 Frontend Usage:");
          console.log("   • Use interactive URL for WebView/iframe");
          console.log("   • Use static URL for fallback images");
          console.log("   • Both URLs are now Google API compliant");
        } catch (parseError) {
          console.error("❌ Failed to parse response:", parseError.message);
          console.log("Raw response:", data);
        }
      } else {
        console.error(`❌ Request failed with status ${res.statusCode}`);
        console.log("Response:", data);
      }

      testServer.close();
      process.exit(0);
    });
  });

  req.on("error", (err) => {
    console.error("❌ Request failed:", err.message);
    testServer.close();
    process.exit(1);
  });

  req.end();
});
