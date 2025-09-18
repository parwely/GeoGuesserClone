// Minimal server test with our actual routes
const express = require("express");
const cors = require("cors");
const database = require("./src/database/connection");

async function startMinimalServer() {
  console.log("🚀 Starting minimal server with BFF routes...");

  try {
    const app = express();

    // Basic middleware
    app.use(express.json());
    app.use(cors());

    // Health check
    app.get("/health", (req, res) => {
      res.json({ status: "OK", message: "Minimal server running" });
    });

    // Connect database first
    console.log("🔍 Connecting to database...");
    await database.connect();
    console.log("✅ Database connected");

    // Load our routes
    console.log("📂 Loading game routes...");
    const gamesRouter = require("./src/routes/games");
    app.use("/api/games", gamesRouter);
    console.log("✅ Game routes loaded");

    // Start server
    const server = app.listen(3001, () => {
      console.log("✅ Minimal server running on port 3001");
      console.log("🧪 Testing endpoints in 2 seconds...");

      setTimeout(async () => {
        await testMinimalServer();
        server.close(() => {
          console.log("✅ Minimal server shutdown");
          process.exit(0);
        });
      }, 2000);
    });
  } catch (error) {
    console.error("❌ Minimal server failed:", error.message);
    console.error("Stack:", error.stack);
    process.exit(1);
  }
}

async function testMinimalServer() {
  console.log("🧪 Testing minimal server endpoints...");

  try {
    // Test health
    const healthResult = await makeHttpRequest("GET", "/health", 3001);
    console.log(
      `Health: ${healthResult.status} - ${healthResult.data?.message || "OK"}`
    );

    // Test new round
    const newRoundResult = await makeHttpRequest(
      "GET",
      "/api/games/newRound",
      3001
    );
    console.log(`NewRound: ${newRoundResult.status}`);

    if (newRoundResult.status === 200) {
      console.log("✅ BFF endpoints working on minimal server!");
    } else {
      console.log("❌ BFF endpoint failed:", newRoundResult.data);
    }
  } catch (error) {
    console.error("❌ Test failed:", error.message);
  }
}

function makeHttpRequest(method, path, port = 3000) {
  const http = require("http"); // Move this here
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "localhost",
      port: port,
      path: path,
      method: method,
      headers: { "Content-Type": "application/json" },
    };

    const req = http.request(options, (res) => {
      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => {
        try {
          resolve({
            status: res.statusCode,
            data: data ? JSON.parse(data) : null,
          });
        } catch (e) {
          resolve({
            status: res.statusCode,
            data: data,
          });
        }
      });
    });

    req.on("error", reject);
    req.setTimeout(5000, () => {
      req.destroy();
      reject(new Error("Request timeout"));
    });

    req.end();
  });
}

startMinimalServer();
