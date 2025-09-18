// Direkter Test des Street View Check Endpoints
const http = require("http");

async function quickTest() {
  console.log("🔍 Quick Street View Check Test");

  const options = {
    hostname: "localhost",
    port: 3000,
    path: "/api/game/streetview/check/1",
    method: "GET",
    headers: { "Content-Type": "application/json" },
  };

  const req = http.request(options, (res) => {
    console.log(`Status: ${res.statusCode}`);
    let data = "";
    res.on("data", (chunk) => (data += chunk));
    res.on("end", () => {
      console.log("Response:", data);

      // Also test the original path that Android was calling
      const options2 = {
        hostname: "localhost",
        port: 3000,
        path: "/api/game/streetview/check/24",
        method: "GET",
        headers: { "Content-Type": "application/json" },
      };

      const req2 = http.request(options2, (res2) => {
        console.log(`\nAndroid path status: ${res2.statusCode}`);
        let data2 = "";
        res2.on("data", (chunk) => (data2 += chunk));
        res2.on("end", () => {
          console.log("Android path response:", data2);
          process.exit(0);
        });
      });

      req2.on("error", (error) => {
        console.error("Android path error:", error.message);
        process.exit(1);
      });

      req2.end();
    });
  });

  req.on("error", (error) => {
    console.error("Request error:", error.message);
    process.exit(1);
  });

  req.end();
}

// Wait a bit, then test
setTimeout(quickTest, 3000);
