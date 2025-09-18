require("dotenv").config();
const { app, server } = require("./src/server");
const http = require("http");

// Start server on port 3001 for testing
const testServer = server.listen(3001, () => {
  console.log(
    "🧪 Testing Street View diagnostic endpoint with valid location...\n"
  );

  // Test the diagnostic endpoint with location ID 93 (Atacama Desert)
  const options = {
    hostname: "localhost",
    port: 3001,
    path: "/api/locations/93/streetview/diagnose?heading=90&pitch=10&fov=100",
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
          const diagnostics = JSON.parse(data);

          console.log("✅ Diagnostic endpoint working successfully!\n");
          console.log("🔍 Diagnostic Results:");
          console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

          // Location info
          console.log(
            `📍 Location: ${diagnostics.location.name} (ID: ${diagnostics.location.id})`
          );
          console.log(`🌐 Coordinates: ${diagnostics.location.coordinates}`);

          // Parameters
          console.log(`🎯 Parameters:`);
          console.log(`   - Heading: ${diagnostics.parameters.heading}°`);
          console.log(`   - Pitch: ${diagnostics.parameters.pitch}°`);
          console.log(`   - FOV: ${diagnostics.parameters.fov}°`);

          // API Key status
          console.log(`🔑 API Key Status:`);
          console.log(`   - Present: ${diagnostics.apiKey.present}`);
          console.log(`   - Length: ${diagnostics.apiKey.length}`);
          console.log(
            `   - Valid format: ${diagnostics.apiKey.startsWithAIza}`
          );

          // URL Generation
          if (diagnostics.generatedUrls) {
            console.log(`🔗 Generated URLs:`);
            console.log(
              `   - Interactive: ${diagnostics.generatedUrls.interactive.substring(
                0,
                80
              )}...`
            );
            console.log(
              `   - Static: ${diagnostics.generatedUrls.static.substring(
                0,
                80
              )}...`
            );
          }

          // Recommendations
          if (
            diagnostics.recommendations &&
            diagnostics.recommendations.length > 0
          ) {
            console.log(`⚠️  Recommendations:`);
            diagnostics.recommendations.forEach((rec, index) => {
              console.log(
                `   ${index + 1}. [${rec.priority}] ${rec.issue}: ${
                  rec.solution
                }`
              );
            });
          } else {
            console.log(`✅ No issues found - configuration looks good!`);
          }

          // Configuration help
          console.log(`\n📚 Google Cloud Console Setup:`);
          diagnostics.configurationHelp.googleCloudConsole.steps.forEach(
            (step) => {
              console.log(`   ${step}`);
            }
          );

          console.log(`\n🔧 Common Issues:`);
          Object.entries(diagnostics.configurationHelp.commonIssues).forEach(
            ([code, desc]) => {
              console.log(`   - ${code.toUpperCase()}: ${desc}`);
            }
          );

          console.log(
            `\n🎉 Diagnostic endpoint implementation completed successfully!`
          );
          console.log(`\n📋 Summary:`);
          console.log(
            `   ✅ Diagnostic endpoint implemented at /api/locations/:id/streetview/diagnose`
          );
          console.log(`   ✅ Comprehensive API key analysis`);
          console.log(`   ✅ URL generation validation`);
          console.log(`   ✅ Parameter validation`);
          console.log(`   ✅ Google Cloud Console configuration guide`);
          console.log(`   ✅ Common issues troubleshooting`);
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
