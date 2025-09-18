// Simple BFF endpoint test
const http = require("http");

async function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function testNewRound() {
  console.log("🎮 Testing /api/games/newRound endpoint...");

  const options = {
    hostname: "localhost",
    port: 3000,
    path: "/api/games/newRound",
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  };

  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => {
        console.log(`Status: ${res.statusCode}`);
        try {
          const jsonData = JSON.parse(data);
          console.log("Response:", JSON.stringify(jsonData, null, 2));
          resolve({ status: res.statusCode, data: jsonData });
        } catch (e) {
          console.log("Raw response:", data);
          resolve({ status: res.statusCode, data: data });
        }
      });
    });

    req.on("error", (error) => {
      console.error("Request error:", error.message);
      reject(error);
    });

    req.setTimeout(10000, () => {
      console.error("Request timeout");
      req.destroy();
      reject(new Error("Timeout"));
    });

    req.end();
  });
}

async function main() {
  try {
    console.log("⏳ Waiting 2 seconds for server...");
    await sleep(2000);

    const result = await testNewRound();

    if (result.status === 200) {
      console.log("✅ BFF /newRound endpoint is working!");
    } else {
      console.log("❌ BFF endpoint returned non-200 status");
    }
  } catch (error) {
    console.error("❌ Test failed:", error.message);
  }
}

main();
