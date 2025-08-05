const { Server } = require("socket.io");
const authService = require("./authService");
const battleRoyaleManager = require("./battleRoyaleService");

class SocketService {
  constructor() {
    this.io = null;
    this.connectedUsers = new Map(); // socketId -> userId mapping
    this.userSockets = new Map(); // userId -> socketId mapping

    console.log("🔌 Socket service initialized");
  }

  initialize(server) {
    this.io = new Server(server, {
      cors: {
        origin: process.env.CORS_ORIGIN || "*",
        methods: ["GET", "POST"],
        credentials: true,
      },
      pingTimeout: 60000,
      pingInterval: 25000,
    });

    // Set circular reference for battleRoyaleManager
    battleRoyaleManager.setSocketService(this);

    this.setupMiddleware();
    this.setupEventHandlers();

    console.log("🚀 Socket.IO server initialized");
    return this.io;
  }

  setupMiddleware() {
    // Authentication middleware
    this.io.use(async (socket, next) => {
      try {
        const token = socket.handshake.auth.token;

        if (!token) {
          throw new Error("No authentication token provided");
        }

        const decoded = authService.verifyToken(token);
        const user = await authService.getUserById(decoded.userId);

        if (!user) {
          throw new Error("User not found");
        }

        socket.userId = user.id;
        socket.username = user.username;

        console.log(`🔐 User authenticated: ${user.username} (${socket.id})`);
        next();
      } catch (error) {
        console.error("❌ Socket authentication failed:", error.message);
        next(new Error("Authentication failed"));
      }
    });
  }

  setupEventHandlers() {
    this.io.on("connection", (socket) => {
      console.log(`👤 User connected: ${socket.username} (${socket.id})`);

      // Track connected user
      this.connectedUsers.set(socket.id, socket.userId);
      this.userSockets.set(socket.userId, socket.id);

      // Send connection confirmation
      socket.emit("connected", {
        message: "Connected to Battle Royale server",
        userId: socket.userId,
        username: socket.username,
      });

      // Battle Royale Events
      this.setupBattleRoyaleEvents(socket);

      // Disconnection handler
      socket.on("disconnect", (reason) => {
        console.log(`👤 User disconnected: ${socket.username} (${reason})`);

        this.connectedUsers.delete(socket.id);
        this.userSockets.delete(socket.userId);

        // Handle session cleanup
        this.handleUserDisconnect(socket.userId);
      });

      // Error handler
      socket.on("error", (error) => {
        console.error(`❌ Socket error for ${socket.username}:`, error);
      });
    });
  }

  setupBattleRoyaleEvents(socket) {
    // Create session
    socket.on("create-session", async (data, callback) => {
      try {
        console.log(
          `🎮 ${socket.username} creating session with settings:`,
          data
        );

        const session = await battleRoyaleManager.createSession(
          socket.userId,
          data.settings || {}
        );

        // Join creator to the session room
        socket.join(`session-${session.code}`);

        callback({
          success: true,
          session: this.formatSessionForClient(session),
        });

        console.log(`✅ Session ${session.code} created by ${socket.username}`);
      } catch (error) {
        console.error("❌ Create session failed:", error.message);
        callback({
          success: false,
          error: error.message,
        });
      }
    });

    // Join session
    socket.on("join-session", (data, callback) => {
      try {
        const { sessionCode } = data;
        console.log(`🔗 ${socket.username} joining session ${sessionCode}`);

        const session = battleRoyaleManager.joinSession(
          sessionCode,
          socket.userId,
          socket.username,
          socket.id
        );

        // Join socket room
        socket.join(`session-${sessionCode}`);

        // Notify all players in session
        socket.to(`session-${sessionCode}`).emit("player-joined", {
          player: {
            userId: socket.userId,
            username: socket.username,
            score: 0,
            isAlive: true,
          },
          totalPlayers: session.players.length,
        });

        callback({
          success: true,
          session: this.formatSessionForClient(session),
        });

        console.log(`✅ ${socket.username} joined session ${sessionCode}`);
      } catch (error) {
        console.error("❌ Join session failed:", error.message);
        callback({
          success: false,
          error: error.message,
        });
      }
    });

    // Leave session
    socket.on("leave-session", (data, callback) => {
      try {
        const { sessionCode } = data;
        console.log(`🚪 ${socket.username} leaving session ${sessionCode}`);

        const success = battleRoyaleManager.leaveSession(
          sessionCode,
          socket.userId
        );

        if (success) {
          socket.leave(`session-${sessionCode}`);

          // Notify remaining players
          socket.to(`session-${sessionCode}`).emit("player-left", {
            userId: socket.userId,
            username: socket.username,
          });
        }

        callback({
          success,
          message: success
            ? "Left session successfully"
            : "Failed to leave session",
        });
      } catch (error) {
        console.error("❌ Leave session failed:", error.message);
        callback({
          success: false,
          error: error.message,
        });
      }
    });

    // Start session
    socket.on("start-session", (data, callback) => {
      try {
        const { sessionCode } = data;
        console.log(`🚀 ${socket.username} starting session ${sessionCode}`);

        const session = battleRoyaleManager.startSession(
          sessionCode,
          socket.userId
        );

        // Notify all players that game has started
        this.io.to(`session-${sessionCode}`).emit("session-started", {
          session: this.formatSessionForClient(session),
          message: "Battle Royale has begun!",
        });

        // Start first round
        this.startRoundForSession(sessionCode);

        callback({
          success: true,
          session: this.formatSessionForClient(session),
        });
      } catch (error) {
        console.error("❌ Start session failed:", error.message);
        callback({
          success: false,
          error: error.message,
        });
      }
    });

    // Submit guess
    socket.on("submit-guess", (data, callback) => {
      try {
        const { sessionCode, guess } = data;
        console.log(
          `🎯 ${socket.username} submitting guess for session ${sessionCode}`
        );

        const guessResult = battleRoyaleManager.processGuess(
          sessionCode,
          socket.userId,
          guess
        );

        // Notify player of their result
        callback({
          success: true,
          guess: guessResult,
        });

        // Notify all players that a guess was submitted (without revealing the guess)
        socket.to(`session-${sessionCode}`).emit("guess-submitted", {
          username: socket.username,
          hasGuessed: true,
        });

        console.log(
          `✅ Guess processed for ${socket.username}: ${guessResult.distance}km`
        );
      } catch (error) {
        console.error("❌ Submit guess failed:", error.message);
        callback({
          success: false,
          error: error.message,
        });
      }
    });

    // Get session info
    socket.on("get-session", (data, callback) => {
      try {
        const { sessionCode } = data;
        const session = battleRoyaleManager.getSession(sessionCode);

        if (!session) {
          callback({
            success: false,
            error: "Session not found",
          });
          return;
        }

        callback({
          success: true,
          session: this.formatSessionForClient(session),
        });
      } catch (error) {
        callback({
          success: false,
          error: error.message,
        });
      }
    });

    // Get leaderboard
    socket.on("get-leaderboard", (data, callback) => {
      try {
        const { sessionCode } = data;
        const leaderboard = battleRoyaleManager.getLeaderboard(sessionCode);

        callback({
          success: true,
          leaderboard,
        });
      } catch (error) {
        callback({
          success: false,
          error: error.message,
        });
      }
    });
  }

  // Start round for all players in session
  startRoundForSession(sessionCode) {
    const roundData = battleRoyaleManager.startRound(sessionCode);

    if (roundData) {
      this.io.to(`session-${sessionCode}`).emit("round-started", {
        round: roundData.roundNumber,
        location: roundData.location,
        duration: roundData.duration,
        playersAlive: roundData.playersAlive,
        startTime: roundData.startTime,
      });

      console.log(
        `🎯 Round ${roundData.roundNumber} started for session ${sessionCode}`
      );
    }
  }

  // End round for all players in session
  endRoundForSession(sessionCode) {
    const session = battleRoyaleManager.getSession(sessionCode);
    if (!session) return;

    const roundResult = battleRoyaleManager.endRound(sessionCode);

    if (roundResult) {
      this.io.to(`session-${sessionCode}`).emit("round-ended", {
        roundNumber: roundResult.roundNumber,
        eliminated: roundResult.eliminated,
        remaining: roundResult.remaining,
        leaderboard: roundResult.leaderboard,
      });

      // Notify eliminated players
      roundResult.eliminated.forEach((player) => {
        const socketId = this.userSockets.get(player.userId);
        if (socketId) {
          this.io.to(socketId).emit("player-eliminated", {
            message: "You have been eliminated!",
            finalScore: player.finalScore,
            finalRank: roundResult.leaderboard.find(
              (p) => p.userId === player.userId
            )?.rank,
          });
        }
      });

      console.log(
        `🏁 Round ${roundResult.roundNumber} ended for session ${sessionCode}`
      );
    }
  }

  // End session for all players
  endSessionForPlayers(sessionCode) {
    const session = battleRoyaleManager.endSession(sessionCode);

    if (session) {
      this.io.to(`session-${sessionCode}`).emit("session-ended", {
        winner: session.winner
          ? {
              userId: session.winner.userId,
              username: session.winner.username,
              score: session.winner.score,
            }
          : null,
        leaderboard: battleRoyaleManager.getLeaderboard(sessionCode),
        message: session.winner
          ? `🏆 ${session.winner.username} wins the Battle Royale!`
          : "Battle Royale ended with no winner",
      });

      // Clean up socket rooms
      this.io
        .in(`session-${sessionCode}`)
        .socketsLeave(`session-${sessionCode}`);

      console.log(`🏆 Session ${sessionCode} ended and broadcast to players`);
    }
  }

  // Handle user disconnect
  handleUserDisconnect(userId) {
    // Find and leave any active sessions
    const sessionCode = battleRoyaleManager.playerSessions?.get(userId);
    if (sessionCode) {
      battleRoyaleManager.leaveSession(sessionCode, userId);
    }
  }

  // Format session data for client (remove sensitive information)
  formatSessionForClient(session) {
    return {
      code: session.code,
      status: session.status,
      currentRound: session.currentRound,
      maxRounds: session.maxRounds,
      settings: session.settings,
      players: session.players.map((p) => ({
        userId: p.userId,
        username: p.username,
        score: p.score,
        isAlive: p.isAlive,
        connected: p.connected,
      })),
      createdAt: session.createdAt,
      startedAt: session.startedAt,
      winner: session.winner
        ? {
            userId: session.winner.userId,
            username: session.winner.username,
            score: session.winner.score,
          }
        : null,
    };
  }

  // Send message to specific user
  sendToUser(userId, event, data) {
    const socketId = this.userSockets.get(userId);
    if (socketId) {
      this.io.to(socketId).emit(event, data);
      return true;
    }
    return false;
  }

  // Send message to session
  sendToSession(sessionCode, event, data) {
    this.io.to(`session-${sessionCode}`).emit(event, data);
  }

  // Broadcast to session (alias for sendToSession)
  broadcastToSession(sessionCode, event, data) {
    this.sendToSession(sessionCode, event, data);
  }

  // Get service stats
  getStats() {
    return {
      connectedUsers: this.connectedUsers.size,
      activeSessions: battleRoyaleManager.getStats(),
    };
  }
}

module.exports = new SocketService();
