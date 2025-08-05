const locationService = require("./locationService");
const cacheService = require("./cacheService");

class BattleRoyaleManager {
  constructor() {
    this.sessions = new Map();
    this.playerSessions = new Map(); // Track which session each player is in
    this.roundTimers = new Map(); // Track timers for each session
    this.socketService = null; // Will be set by socketService to avoid circular dependency

    // Configuration
    this.config = {
      maxPlayers: 50,
      minPlayers: 2,
      roundDuration: 30000, // 30 seconds per round
      eliminationPercentage: 0.2, // Eliminate bottom 20% each round
      maxRounds: 10,
      waitingRoomTimeout: 120000, // 2 minutes waiting room
    };

    console.log("⚔️ Battle Royale Manager initialized");
  }

  // Set socket service reference (called by socketService)
  setSocketService(socketService) {
    this.socketService = socketService;
    console.log("🔌 Socket service reference set in Battle Royale Manager");
  }

  // Generate unique session code
  generateSessionCode() {
    return Math.random().toString(36).substring(2, 8).toUpperCase();
  }

  // Create new battle royale session
  async createSession(creatorId, settings = {}) {
    try {
      const code = this.generateSessionCode();

      // Get random locations for the session
      const locations = await locationService.getRandomLocations({
        count: this.config.maxRounds,
        difficulty: settings.difficulty || null,
        category: settings.category || null,
      });

      if (locations.length < this.config.maxRounds) {
        throw new Error("Not enough locations available for session");
      }

      const session = {
        code,
        creatorId,
        players: [],
        status: "waiting", // waiting, active, finished
        currentRound: 0,
        maxRounds: Math.min(
          settings.maxRounds || this.config.maxRounds,
          locations.length
        ),
        locations: locations,
        settings: {
          difficulty: settings.difficulty || "mixed",
          category: settings.category || "mixed",
          roundDuration: settings.roundDuration || this.config.roundDuration,
          eliminationRate:
            settings.eliminationRate || this.config.eliminationPercentage,
        },
        rounds: [], // Store round results
        startedAt: null,
        finishedAt: null,
        winner: null,
        createdAt: new Date(),
      };

      this.sessions.set(code, session);

      // Set waiting room timeout
      setTimeout(() => {
        if (
          this.sessions.has(code) &&
          this.sessions.get(code).status === "waiting"
        ) {
          this.cancelSession(code, "timeout");
        }
      }, this.config.waitingRoomTimeout);

      console.log(`🎮 Battle Royale session created: ${code}`);
      return session;
    } catch (error) {
      console.error(
        "❌ Failed to create Battle Royale session:",
        error.message
      );
      throw error;
    }
  }

  // Join session
  joinSession(sessionCode, userId, username, socketId) {
    const session = this.sessions.get(sessionCode);

    if (!session) {
      throw new Error("Session not found");
    }

    if (session.status !== "waiting") {
      throw new Error("Session is not accepting new players");
    }

    if (session.players.length >= this.config.maxPlayers) {
      throw new Error("Session is full");
    }

    // Check if player is already in another session
    if (this.playerSessions.has(userId)) {
      const currentSessionCode = this.playerSessions.get(userId);
      this.leaveSession(currentSessionCode, userId);
    }

    // Check if player is already in this session
    const existingPlayer = session.players.find((p) => p.userId === userId);
    if (existingPlayer) {
      // Update socket ID for reconnection
      existingPlayer.socketId = socketId;
      existingPlayer.connected = true;
      return session;
    }

    const player = {
      userId,
      username,
      socketId,
      score: 0,
      isAlive: true,
      connected: true,
      joinedAt: new Date(),
      roundGuesses: {}, // Store guesses for each round
    };

    session.players.push(player);
    this.playerSessions.set(userId, sessionCode);

    console.log(`👤 Player ${username} joined session ${sessionCode}`);
    return session;
  }

  // Leave session
  leaveSession(sessionCode, userId) {
    const session = this.sessions.get(sessionCode);
    if (!session) return false;

    const playerIndex = session.players.findIndex((p) => p.userId === userId);
    if (playerIndex === -1) return false;

    const player = session.players[playerIndex];

    if (session.status === "waiting") {
      // Remove player completely if in waiting room
      session.players.splice(playerIndex, 1);
      this.playerSessions.delete(userId);

      // If creator leaves, cancel session
      if (userId === session.creatorId && session.players.length === 0) {
        this.cancelSession(sessionCode, "creator_left");
      }
    } else {
      // Mark as disconnected during active game
      player.connected = false;
    }

    console.log(`👤 Player ${player.username} left session ${sessionCode}`);
    return true;
  }

  // Start session
  startSession(sessionCode, creatorId) {
    const session = this.sessions.get(sessionCode);

    if (!session) {
      throw new Error("Session not found");
    }

    if (session.creatorId !== creatorId) {
      throw new Error("Only session creator can start the game");
    }

    if (session.players.length < this.config.minPlayers) {
      throw new Error(
        `Need at least ${this.config.minPlayers} players to start`
      );
    }

    if (session.status !== "waiting") {
      throw new Error("Session already started or finished");
    }

    session.status = "active";
    session.startedAt = new Date();
    session.currentRound = 1;

    // Initialize first round
    this.startRound(sessionCode);

    console.log(
      `🚀 Battle Royale session ${sessionCode} started with ${session.players.length} players`
    );
    return session;
  }

  // Start new round
  startRound(sessionCode) {
    const session = this.sessions.get(sessionCode);
    if (!session || session.status !== "active") return;

    const currentLocation = session.locations[session.currentRound - 1];
    const alivePlayers = session.players.filter((p) => p.isAlive);

    const roundData = {
      roundNumber: session.currentRound,
      location: {
        id: currentLocation.id,
        name: currentLocation.name,
        country: currentLocation.country,
        difficulty: currentLocation.difficulty,
        category: currentLocation.category,
        // Don't send actual coordinates to prevent cheating
      },
      startTime: new Date(),
      duration: session.settings.roundDuration,
      playersAlive: alivePlayers.length,
      guesses: new Map(),
    };

    session.rounds.push(roundData);

    // Set round timer
    const timerId = setTimeout(() => {
      this.endRound(sessionCode);
    }, session.settings.roundDuration);

    this.roundTimers.set(sessionCode, timerId);

    console.log(
      `🎯 Round ${session.currentRound} started for session ${sessionCode}`
    );

    // Trigger socket event for round start
    if (this.socketService) {
      this.socketService.broadcastToSession(sessionCode, "round-started", {
        roundNumber: session.currentRound,
        timeLimit: session.settings.roundDuration,
        location: {
          id: currentLocation.id,
          name: currentLocation.name,
          country: currentLocation.country,
          difficulty: currentLocation.difficulty,
        },
        playersAlive: alivePlayers.length,
      });
    }

    return roundData;
  }

  // Process player guess
  processGuess(sessionCode, userId, guess) {
    const session = this.sessions.get(sessionCode);
    if (!session || session.status !== "active") {
      throw new Error("Invalid session state");
    }

    const player = session.players.find((p) => p.userId === userId);
    if (!player || !player.isAlive) {
      throw new Error("Player not found or eliminated");
    }

    const currentRound = session.rounds[session.currentRound - 1];
    if (!currentRound) {
      throw new Error("No active round");
    }

    // Check if player already guessed
    if (currentRound.guesses.has(userId)) {
      throw new Error("Already submitted guess for this round");
    }

    const actualLocation = session.locations[session.currentRound - 1];

    // Calculate distance and score
    const distance = this.calculateDistance(
      guess.latitude,
      guess.longitude,
      actualLocation.coordinates.latitude,
      actualLocation.coordinates.longitude
    );

    const roundScore = this.calculateScore(distance);

    const guessData = {
      userId,
      latitude: guess.latitude,
      longitude: guess.longitude,
      distance,
      score: roundScore,
      submittedAt: new Date(),
    };

    currentRound.guesses.set(userId, guessData);
    player.score += roundScore;
    player.roundGuesses[session.currentRound] = guessData;

    console.log(
      `🎯 Guess processed for ${player.username}: ${distance}km, ${roundScore} points`
    );

    // Check if all alive players have guessed
    const alivePlayers = session.players.filter((p) => p.isAlive);
    if (currentRound.guesses.size >= alivePlayers.length) {
      // End round early if everyone has guessed
      clearTimeout(this.roundTimers.get(sessionCode));
      this.endRound(sessionCode);
    }

    return guessData;
  }

  // End current round
  endRound(sessionCode) {
    const session = this.sessions.get(sessionCode);
    if (!session || session.status !== "active") return;

    const currentRound = session.rounds[session.currentRound - 1];
    if (!currentRound) return;

    currentRound.endTime = new Date();

    // Clear round timer
    if (this.roundTimers.has(sessionCode)) {
      clearTimeout(this.roundTimers.get(sessionCode));
      this.roundTimers.delete(sessionCode);
    }

    // Calculate eliminations
    const alivePlayers = session.players.filter((p) => p.isAlive);
    const playersWithGuesses = alivePlayers.filter((p) =>
      currentRound.guesses.has(p.userId)
    );

    // Players who didn't guess get eliminated
    alivePlayers.forEach((player) => {
      if (!currentRound.guesses.has(player.userId)) {
        player.isAlive = false;
        console.log(`❌ ${player.username} eliminated for not guessing`);
      }
    });

    // Eliminate bottom performers
    if (playersWithGuesses.length > 2) {
      const eliminations = Math.max(
        1,
        Math.floor(playersWithGuesses.length * session.settings.eliminationRate)
      );

      // Sort by round score (ascending for elimination)
      const sortedPlayers = playersWithGuesses.sort((a, b) => {
        const aGuess = currentRound.guesses.get(a.userId);
        const bGuess = currentRound.guesses.get(b.userId);
        return aGuess.score - bGuess.score;
      });

      for (let i = 0; i < eliminations; i++) {
        sortedPlayers[i].isAlive = false;
        console.log(`💀 ${sortedPlayers[i].username} eliminated`);
      }
    }

    const stillAlive = session.players.filter((p) => p.isAlive);

    console.log(
      `🏁 Round ${session.currentRound} ended. ${stillAlive.length} players remaining`
    );

    const roundResult = {
      roundNumber: session.currentRound,
      eliminated: session.players
        .filter((p) => !p.isAlive)
        .map((p) => ({
          userId: p.userId,
          username: p.username,
          finalScore: p.score,
        })),
      remaining: stillAlive.length,
      leaderboard: this.getLeaderboard(sessionCode),
    };

    // Trigger socket event for round end
    if (this.socketService) {
      this.socketService.broadcastToSession(sessionCode, "round-ended", {
        roundNumber: session.currentRound,
        eliminatedPlayers: roundResult.eliminated.map((p) => p.username),
        remainingCount: stillAlive.length,
        leaderboard: this.getLeaderboard(sessionCode),
      });
    }

    // Check win conditions
    if (stillAlive.length <= 1 || session.currentRound >= session.maxRounds) {
      this.endSession(sessionCode);
    } else {
      // Move to next round
      session.currentRound++;
      setTimeout(() => {
        this.startRound(sessionCode);
      }, 3000); // 3 second break between rounds
    }

    return roundResult;
  }

  // End session
  endSession(sessionCode) {
    const session = this.sessions.get(sessionCode);
    if (!session) return;

    session.status = "finished";
    session.finishedAt = new Date();

    const alivePlayers = session.players.filter((p) => p.isAlive);
    if (alivePlayers.length === 1) {
      session.winner = alivePlayers[0];
    } else if (alivePlayers.length > 1) {
      // Determine winner by highest score
      session.winner = alivePlayers.reduce((winner, player) =>
        player.score > winner.score ? player : winner
      );
    }

    // Clean up player sessions
    session.players.forEach((player) => {
      this.playerSessions.delete(player.userId);
    });

    // Clear any remaining timers
    if (this.roundTimers.has(sessionCode)) {
      clearTimeout(this.roundTimers.get(sessionCode));
      this.roundTimers.delete(sessionCode);
    }

    console.log(
      `🏆 Battle Royale session ${sessionCode} finished. Winner: ${
        session.winner?.username || "None"
      }`
    );

    // Trigger socket event for session end
    if (this.socketService) {
      this.socketService.broadcastToSession(sessionCode, "session-ended", {
        winner: session.winner
          ? {
              userId: session.winner.userId,
              username: session.winner.username,
              score: session.winner.score,
            }
          : null,
        finalRanking: this.getLeaderboard(sessionCode),
        totalRounds: session.currentRound - 1,
      });
    }

    // Schedule session cleanup after 10 minutes
    setTimeout(() => {
      this.sessions.delete(sessionCode);
      console.log(`🧹 Session ${sessionCode} cleaned up`);
    }, 600000);

    return session;
  }

  // Cancel session
  cancelSession(sessionCode, reason = "unknown") {
    const session = this.sessions.get(sessionCode);
    if (!session) return false;

    // Clean up players
    session.players.forEach((player) => {
      this.playerSessions.delete(player.userId);
    });

    // Clear timers
    if (this.roundTimers.has(sessionCode)) {
      clearTimeout(this.roundTimers.get(sessionCode));
      this.roundTimers.delete(sessionCode);
    }

    this.sessions.delete(sessionCode);
    console.log(`❌ Session ${sessionCode} cancelled: ${reason}`);
    return true;
  }

  // Get session info
  getSession(sessionCode) {
    return this.sessions.get(sessionCode);
  }

  // Get leaderboard
  getLeaderboard(sessionCode) {
    const session = this.sessions.get(sessionCode);
    if (!session) return [];

    return session.players
      .sort((a, b) => b.score - a.score)
      .map((player, index) => ({
        rank: index + 1,
        userId: player.userId,
        username: player.username,
        score: player.score,
        isAlive: player.isAlive,
        connected: player.connected,
      }));
  }

  // Utility methods
  calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371; // Earth's radius in kilometers
    const dLat = this.toRad(lat2 - lat1);
    const dLon = this.toRad(lon2 - lon1);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.toRad(lat1)) *
        Math.cos(this.toRad(lat2)) *
        Math.sin(dLon / 2) *
        Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  toRad(degrees) {
    return degrees * (Math.PI / 180);
  }

  calculateScore(distanceKm) {
    // Score calculation: max 5000 points, decreasing with distance
    if (distanceKm === 0) return 5000;
    if (distanceKm >= 20000) return 0;

    // Exponential decay formula
    return Math.round(5000 * Math.exp(-distanceKm / 2000));
  }

  // Get active sessions count
  getStats() {
    const activeSessions = Array.from(this.sessions.values()).filter(
      (s) => s.status === "active"
    ).length;
    const waitingSessions = Array.from(this.sessions.values()).filter(
      (s) => s.status === "waiting"
    ).length;
    const totalPlayers = Array.from(this.sessions.values()).reduce(
      (sum, s) => sum + s.players.length,
      0
    );

    return {
      totalSessions: this.sessions.size,
      activeSessions,
      waitingSessions,
      totalPlayers,
      connectedPlayers: this.playerSessions.size,
    };
  }
}

module.exports = new BattleRoyaleManager();
