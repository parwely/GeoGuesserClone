const http = require("http");

console.log("🔍 Street View Check Endpoint Test");
console.log("====================================");

const locationId = 89; // Big Sur Coast
console.log(`Testing: /api/game/streetview/check/${locationId}`);

const options = {
  hostname: "127.0.0.1",
  port: 3000,
  path: `/api/game/streetview/check/${locationId}`,
  method: "GET",
  timeout: 15000,
  headers: {
    "User-Agent": "Android-GeoGuessr-App",
    Accept: "application/json",
  },
};

console.log(`Request: GET http://127.0.0.1:3000${options.path}`);
console.log("");

const req = http.request(options, (res) => {
  console.log(`✅ HTTP Status: ${res.statusCode}`);
  console.log(`✅ Content-Type: ${res.headers["content-type"]}`);

  let data = "";
  res.on("data", (chunk) => {
    data += chunk;
  });

  res.on("end", () => {
    console.log("");
    console.log("📊 Response:");
    console.log("=============");

    try {
      const parsed = JSON.parse(data);
      console.log(JSON.stringify(parsed, null, 2));

      // Check response structure for Android app
      if (parsed.valid !== undefined) {
        console.log("");
        if (parsed.valid) {
          console.log("🎉 SUCCESS: Street View Check Endpoint Working!");
          console.log("✅ Android app timeout issue should be resolved");
          console.log("✅ Location has valid Street View data");
        } else {
          console.log("⚠️  Location not Street View compatible");
          console.log("✅ But endpoint is working correctly");
        }

        console.log("");
        console.log("📋 Response Analysis:");
        console.log(`   - Valid: ${parsed.valid}`);
        console.log(
          `   - Response Time: ${parsed.response_time_ms || "N/A"}ms`
        );
        console.log(`   - Location ID: ${parsed.location_id || "N/A"}`);
        console.log(`   - Cached: ${parsed.cached || false}`);
      } else {
        console.log("❌ Unexpected response structure");
        console.log("Expected: { valid: boolean, ... }");
      }
    } catch (err) {
      console.log("❌ Invalid JSON response");
      console.log("Raw response:", data);
    }
  });
});

req.on("error", (err) => {
  console.log("❌ Connection Error:", err.message);
  console.log("");
  console.log("Possible causes:");
  console.log("- Server not running");
  console.log("- Wrong port");
  console.log("- Firewall blocking connection");
});

req.on("timeout", () => {
  console.log("❌ Request timeout (15 seconds)");
  console.log("This might indicate server issues");
  req.destroy();
});

req.end();
