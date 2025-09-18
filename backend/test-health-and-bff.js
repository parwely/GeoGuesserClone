// Health check test
const http = require("http");

const req = http.get("http://localhost:3000/health", (res) => {
  console.log(`Status: ${res.statusCode}`);
  let data = "";
  res.on("data", (chunk) => (data += chunk));
  res.on("end", () => {
    console.log("✅ Health endpoint response:", data.substring(0, 200));

    // Now test our BFF endpoint
    console.log("\n🎮 Testing BFF endpoint...");
    const gameReq = http.get(
      "http://localhost:3000/api/games/newRound",
      (gameRes) => {
        console.log(`BFF Status: ${gameRes.statusCode}`);
        let gameData = "";
        gameRes.on("data", (chunk) => (gameData += chunk));
        gameRes.on("end", () => {
          console.log("BFF Response:", gameData);
        });
      }
    );

    gameReq.on("error", (error) => {
      console.error("❌ BFF request error:", error.message);
    });
  });
});

req.on("error", (error) => {
  console.error("❌ Health check error:", error.message);
});

setTimeout(() => {
  console.log("⏰ Test timeout - exiting");
  process.exit(0);
}, 5000);
