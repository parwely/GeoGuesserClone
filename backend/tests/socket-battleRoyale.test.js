require("dotenv").config();
const { io } = require("socket.io-client");

// Test Socket.IO connection and Battle Royale functionality
async function testSocketIO() {
  try {
    console.log("🧪 Testing Socket.IO Integration...\n");

    // You'll need a valid JWT token for testing
    // For now, we'll show the connection attempt
    const socket = io("http://localhost:3000", {
      auth: {
        token: "your-jwt-token-here", // Replace with actual token
      },
      timeout: 5000,
    });

    socket.on("connect", () => {
      console.log("✅ Connected to Socket.IO server");
      console.log("Socket ID:", socket.id);

      // Test creating a Battle Royale session
      socket.emit(
        "create-session",
        {
          settings: {
            difficulty: 3,
            category: "urban",
            maxRounds: 5,
            eliminationRate: 0.2,
          },
        },
        (response) => {
          if (response.success) {
            console.log(
              "✅ Battle Royale session created:",
              response.session.code
            );

            // Test joining the session
            socket.emit(
              "join-session",
              {
                sessionCode: response.session.code,
              },
              (joinResponse) => {
                if (joinResponse.success) {
                  console.log("✅ Successfully joined session");
                  console.log(
                    "Players in session:",
                    joinResponse.session.players.length
                  );
                } else {
                  console.log("❌ Failed to join session:", joinResponse.error);
                }
              }
            );
          } else {
            console.log("❌ Failed to create session:", response.error);
          }
        }
      );
    });

    socket.on("connected", (data) => {
      console.log("📡 Server confirmation:", data.message);
      console.log("User:", data.username);
    });

    socket.on("player-joined", (data) => {
      console.log("👤 Player joined:", data.player.username);
      console.log("Total players:", data.totalPlayers);
    });

    socket.on("session-started", (data) => {
      console.log("🚀 Session started:", data.message);
    });

    socket.on("round-started", (data) => {
      console.log("🎯 Round started:", data.round);
      console.log("Location:", data.location.name, data.location.country);
      console.log("Duration:", data.duration / 1000, "seconds");
    });

    socket.on("round-ended", (data) => {
      console.log("🏁 Round ended:", data.roundNumber);
      console.log("Eliminated:", data.eliminated.length, "players");
      console.log("Remaining:", data.remaining, "players");
    });

    socket.on("player-eliminated", (data) => {
      console.log("💀 You were eliminated!");
      console.log("Final score:", data.finalScore);
      console.log("Final rank:", data.finalRank);
    });

    socket.on("session-ended", (data) => {
      console.log("🏆 Session ended!");
      if (data.winner) {
        console.log(
          "Winner:",
          data.winner.username,
          "with",
          data.winner.score,
          "points"
        );
      }
    });

    socket.on("connect_error", (error) => {
      console.error("❌ Connection failed:", error.message);
    });

    socket.on("disconnect", (reason) => {
      console.log("🔌 Disconnected:", reason);
    });

    // Keep the connection alive for testing
    setTimeout(() => {
      socket.disconnect();
      console.log("\n✅ Socket.IO test completed");
    }, 10000);
  } catch (error) {
    console.error("❌ Socket.IO test failed:", error.message);
  }
}

// HTTP API test for Battle Royale
async function testBattleRoyaleAPI() {
  const axios = require("axios");
  const BASE_URL = "http://localhost:3000/api";

  try {
    console.log("🧪 Testing Battle Royale HTTP API...\n");

    // Test 1: Get Battle Royale stats
    console.log("1️⃣ Testing Battle Royale Stats...");
    const statsResponse = await axios.get(`${BASE_URL}/battle-royale/stats`);

    console.log("✅ Stats retrieved successfully!");
    console.log(
      "Active sessions:",
      statsResponse.data.data.battleRoyale.activeSessions
    );
    console.log(
      "Total players:",
      statsResponse.data.data.battleRoyale.totalPlayers
    );
    console.log("");

    // Test 2: Health check
    console.log("2️⃣ Testing Battle Royale Health Check...");
    const healthResponse = await axios.get(`${BASE_URL}/battle-royale/health`);

    console.log("✅ Health check passed!");
    console.log("Battle Royale service is healthy");
    console.log("");
  } catch (error) {
    console.error("❌ Battle Royale API test failed:", error.message);
    if (error.response) {
      console.error("Response:", error.response.data);
    }
  }
}

if (require.main === module) {
  console.log("🎮 Testing Socket.IO and Battle Royale Integration\n");
  console.log("Note: Make sure the server is running on port 3000\n");

  testBattleRoyaleAPI().then(() => {
    // Uncomment the line below and add a valid JWT token to test Socket.IO
    // testSocketIO();
    console.log(
      "💡 To test Socket.IO, add a valid JWT token and uncomment testSocketIO()"
    );
  });
}

module.exports = { testSocketIO, testBattleRoyaleAPI };
