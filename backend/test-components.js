// Simple component test - no async operations
console.log("🔍 Testing component imports...");

try {
  console.log("1. Testing Express...");
  const express = require("express");
  console.log("✅ Express OK");

  console.log("2. Testing gameService...");
  const gameService = require("./src/services/gameService");
  console.log("✅ gameService OK");

  console.log("3. Testing games routes...");
  const gamesRouter = require("./src/routes/games");
  console.log("✅ Games router OK");

  console.log("4. Testing database module (no connection)...");
  const database = require("./src/database/connection");
  console.log("✅ Database module OK");

  console.log("🎉 All imports successful!");

  // Now test creating a simple server without database
  console.log("5. Creating minimal server...");
  const app = express();
  app.use(express.json());

  app.get("/test", (req, res) => {
    res.json({ message: "Test endpoint works" });
  });

  const server = app.listen(3001, () => {
    console.log("✅ Minimal server running on port 3001");
    console.log("Visit http://localhost:3001/test to test");

    // Auto-shutdown after 5 seconds
    setTimeout(() => {
      console.log("🔄 Shutting down test server...");
      server.close(() => {
        console.log("✅ Server shutdown complete");
        process.exit(0);
      });
    }, 5000);
  });
} catch (error) {
  console.error("❌ Component test failed:", error.message);
  console.error("Stack:", error.stack);
}
