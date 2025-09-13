const express = require("express");
const router = express.Router();
const { authenticateToken } = require("../middleware/authMiddleware");
const battleRoyaleManager = require("../services/battleRoyaleService");
const socketService = require("../services/socketService");

// Battle Royale service information - Root endpoint
router.get("/", async (req, res) => {
  try {
    console.log("🎮 Battle Royale service info requested");

    const stats = battleRoyaleManager.getStats();
    const socketStats = socketService.getStats();

    res.json({
      success: true,
      message: "Battle Royale service is active",
      version: "1.0.0",
      data: {
        service: "Battle Royale",
        status: "active",
        features: [
          "Real-time multiplayer battles",
          "Session-based gameplay",
          "Elimination rounds",
          "Live leaderboards",
        ],
        endpoints: {
          stats: "/api/battle-royale/stats",
          create: "/api/battle-royale/create",
          session: "/api/battle-royale/session/:code",
          leaderboard: "/api/battle-royale/session/:code/leaderboard",
          start: "/api/battle-royale/session/:code/start",
          health: "/api/battle-royale/health",
        },
        currentStats: {
          activeSessions: stats.activeSessions,
          totalPlayers: stats.totalPlayers,
          connectedUsers: socketStats.connectedUsers,
        },
        timestamp: new Date(),
      },
    });
  } catch (error) {
    console.error("❌ Battle Royale service info failed:", error.message);
    res.status(500).json({
      error: "Failed to get service information",
      message: "Internal server error",
    });
  }
});

// Get battle royale statistics - Public endpoint
router.get("/stats", async (req, res) => {
  try {
    console.log("📊 Battle Royale stats requested");

    const stats = battleRoyaleManager.getStats();
    const socketStats = socketService.getStats();

    res.json({
      success: true,
      data: {
        battleRoyale: stats,
        connections: {
          connectedUsers: socketStats.connectedUsers,
        },
        timestamp: new Date(),
      },
    });
  } catch (error) {
    console.error("❌ Battle Royale stats failed:", error.message);
    res.status(500).json({
      error: "Failed to get statistics",
      message: "Internal server error",
    });
  }
});

// Create battle royale session - Authenticated endpoint
router.post("/create", authenticateToken, async (req, res) => {
  try {
    const { settings } = req.body;
    const userId = req.user.id;

    console.log(`🎮 ${req.user.username} creating Battle Royale session`);

    const session = await battleRoyaleManager.createSession(userId, settings);

    res.json({
      success: true,
      data: {
        session: {
          code: session.code,
          status: session.status,
          settings: session.settings,
          maxRounds: session.maxRounds,
          createdAt: session.createdAt,
        },
      },
    });
  } catch (error) {
    console.error("❌ Create Battle Royale session failed:", error.message);
    res.status(500).json({
      error: "Failed to create session",
      message: error.message,
    });
  }
});

// Get session info - Public endpoint
router.get("/session/:code", async (req, res) => {
  try {
    const { code } = req.params;

    console.log(`🔍 Session info requested: ${code}`);

    const session = battleRoyaleManager.getSession(code);

    if (!session) {
      return res.status(404).json({
        error: "Session not found",
        message: `No session found with code ${code}`,
      });
    }

    res.json({
      success: true,
      data: {
        session: {
          code: session.code,
          status: session.status,
          currentRound: session.currentRound,
          maxRounds: session.maxRounds,
          settings: session.settings,
          players: session.players.map((p) => ({
            username: p.username,
            score: p.score,
            isAlive: p.isAlive,
            connected: p.connected,
          })),
          createdAt: session.createdAt,
          startedAt: session.startedAt,
          winner: session.winner
            ? {
                username: session.winner.username,
                score: session.winner.score,
              }
            : null,
        },
      },
    });
  } catch (error) {
    console.error("❌ Get session info failed:", error.message);
    res.status(500).json({
      error: "Failed to get session info",
      message: "Internal server error",
    });
  }
});

// Get session leaderboard - Public endpoint
router.get("/session/:code/leaderboard", async (req, res) => {
  try {
    const { code } = req.params;

    console.log(`🏆 Leaderboard requested for session: ${code}`);

    const leaderboard = battleRoyaleManager.getLeaderboard(code);

    if (!leaderboard) {
      return res.status(404).json({
        error: "Session not found",
        message: `No session found with code ${code}`,
      });
    }

    res.json({
      success: true,
      data: {
        leaderboard,
        sessionCode: code,
        timestamp: new Date(),
      },
    });
  } catch (error) {
    console.error("❌ Get leaderboard failed:", error.message);
    res.status(500).json({
      error: "Failed to get leaderboard",
      message: "Internal server error",
    });
  }
});

// Start session - Authenticated endpoint
router.post("/session/:code/start", authenticateToken, async (req, res) => {
  try {
    const { code } = req.params;
    const userId = req.user.id;

    console.log(`🚀 ${req.user.username} starting session: ${code}`);

    const session = battleRoyaleManager.startSession(code, userId);

    // Trigger socket events for all players
    socketService.sendToSession(code, "session-started", {
      session: {
        code: session.code,
        status: session.status,
        currentRound: session.currentRound,
      },
      message: "Battle Royale has begun!",
    });

    res.json({
      success: true,
      data: {
        session: {
          code: session.code,
          status: session.status,
          currentRound: session.currentRound,
          startedAt: session.startedAt,
        },
      },
    });
  } catch (error) {
    console.error("❌ Start session failed:", error.message);
    res.status(400).json({
      error: "Failed to start session",
      message: error.message,
    });
  }
});

// Health check for battle royale service
router.get("/health", async (req, res) => {
  try {
    const stats = battleRoyaleManager.getStats();
    const socketStats = socketService.getStats();

    res.json({
      success: true,
      message: "Battle Royale service is healthy",
      data: {
        battleRoyale: stats,
        sockets: socketStats,
        timestamp: new Date(),
      },
    });
  } catch (error) {
    res.status(503).json({
      success: false,
      error: "Battle Royale service is unhealthy",
      message: error.message,
    });
  }
});

module.exports = router;
