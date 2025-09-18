// Debug server startup
const express = require("express");
const database = require("./src/database/connection");

async function test() {
  try {
    console.log("1. Testing database connection...");
    await database.connect();
    console.log("✅ Database connected");

    console.log("2. Testing gameService import...");
    const gameService = require("./src/services/gameService");
    console.log("✅ gameService imported");

    console.log("3. Testing games routes import...");
    const gamesRouter = require("./src/routes/games");
    console.log("✅ Games router imported");

    console.log("4. Creating Express app...");
    const app = express();
    app.use(express.json());

    console.log("5. Adding routes...");
    app.use("/api/games", gamesRouter);

    console.log("6. Starting server...");
    const server = app.listen(3001, () => {
      console.log("✅ Debug server running on port 3001");
      console.log("Testing /api/games/newRound endpoint...");
    });
  } catch (error) {
    console.error("❌ Debug test failed:", error.message);
    console.error("Stack:", error.stack);
  }
}

test();
