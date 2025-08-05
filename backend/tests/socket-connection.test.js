const io = require("socket.io-client");

// Test Socket.IO Connection
console.log("🧪 Testing Socket.IO Connection...\n");

// Replace with a valid JWT token from your auth endpoint
const JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."; // Add your token here

const socket = io("http://localhost:3000", {
  auth: {
    token: JWT_TOKEN,
  },
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
        maxRounds: 3,
        eliminationRate: 0.2,
      },
    },
    (response) => {
      if (response.success) {
        console.log("✅ Session created:", response.session.code);

        // Test joining the session
        socket.emit(
          "join-session",
          {
            sessionCode: response.session.code,
          },
          (joinResponse) => {
            if (joinResponse.success) {
              console.log("✅ Joined session successfully");
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
  console.log("📡 Server message:", data.message);
  console.log("👤 Username:", data.username);
});

socket.on("player-joined", (data) => {
  console.log("🎮 Player joined:", data.player.username);
  console.log("👥 Total players:", data.totalPlayers);
});

socket.on("session-started", (data) => {
  console.log("🚀 Session started:", data.message);
});

socket.on("round-started", (data) => {
  console.log("⏰ Round", data.round, "started");
  console.log("📍 Location:", data.location.name, data.location.country);
  console.log("⏱️  Duration:", data.duration / 1000, "seconds");
});

socket.on("connect_error", (error) => {
  console.log("❌ Connection error:", error.message);
  if (error.message === "Authentication error") {
    console.log("💡 Please add a valid JWT token to this test file");
  }
  process.exit(1);
});

socket.on("disconnect", (reason) => {
  console.log("🔌 Disconnected:", reason);
  process.exit(0);
});

// Cleanup after 10 seconds
setTimeout(() => {
  console.log("\n🔄 Test completed, disconnecting...");
  socket.disconnect();
}, 10000);
