const http = require("http");

console.log("🔍 Direct HTTP Test");
console.log("====================");

const options = {
  hostname: "localhost",
  port: 3000,
  path: "/health",
  method: "GET",
  timeout: 5000,
};

console.log("1️⃣ Testing Health Endpoint...");
console.log(`Request: GET http://localhost:3000/health`);

const req = http.request(options, (res) => {
  console.log(`✅ Status: ${res.statusCode}`);
  console.log(`Headers:`, res.headers);

  let data = "";
  res.on("data", (chunk) => {
    data += chunk;
  });

  res.on("end", () => {
    console.log("✅ Response:", data);

    // Test Street View endpoint now
    testStreetViewEndpoint();
  });
});

req.on("error", (err) => {
  console.log("❌ Health check failed:", err.message);
  console.log("Error details:", err);
});

req.on("timeout", () => {
  console.log("❌ Request timeout");
  req.destroy();
});

req.end();

function testStreetViewEndpoint() {
  console.log("\n2️⃣ Testing Street View Check Endpoint...");

  const svOptions = {
    hostname: "localhost",
    port: 3000,
    path: "/api/game/streetview/check/1",
    method: "GET",
    timeout: 5000,
  };

  console.log(`Request: GET http://localhost:3000/api/game/streetview/check/1`);

  const svReq = http.request(svOptions, (res) => {
    console.log(`✅ Status: ${res.statusCode}`);

    let data = "";
    res.on("data", (chunk) => {
      data += chunk;
    });

    res.on("end", () => {
      console.log("✅ Response:", data);
    });
  });

  svReq.on("error", (err) => {
    console.log("❌ Street View check failed:", err.message);
    console.log("Error details:", err);
  });

  svReq.on("timeout", () => {
    console.log("❌ Request timeout");
    svReq.destroy();
  });

  svReq.end();
}
