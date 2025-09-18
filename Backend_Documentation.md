# 🌍 GeoGuess - Server Infrastruktur Dokumentation

**Version:** 1.0.0  
**Stand:** September 2025  
**Architektur:** Backend-For-Frontend (BFF) Pattern mit Real-time Support

---
## Backend-Log aus einen Spiel: 

```
 npm start

> geoguessr-clone-backend@1.0.0 start
> node src/server.js

[dotenv@17.2.0] injecting env (17) from .env (tip: ⚙️  enable debug logging with { debug: true })
[dotenv@17.2.0] injecting env (0) from .env (tip: ⚙️  write to custom object with { processEnv: myObject })
🚀 Cache service initialized
🌍 Street View service initialized with interactive support
⚔️ Battle Royale Manager initialized
🔌 Socket service initialized
🔌 Socket service reference set in Battle Royale Manager
🚀 Socket.IO server initialized
🚀 Server running on port 3000
📡 API URL: http://localhost:3000
📡 API URL (IPv4): http://127.0.0.1:3000
🏥 Health check: http://localhost:3000/health
🔌 Socket.IO server ready for real-time connections
⚔️ Battle Royale service: http://localhost:3000/api/battle-royale
🔍 Connecting to database...
Host: ep-steep-star-a2ymqe47-pooler.eu-central-1.aws.neon.tech
SSL enabled: Yes
✅ Connected to PostgreSQL database
📊 PostgreSQL version: 16.9
127.0.0.1 - - [18/Sep/2025:20:02:58 +0000] "GET /health HTTP/1.1" 200 600 "-" "-"
✅ Server health check passed (IPv4) - Status: 200
🎮 Game Route: /newRound called
🎲 GameService: Getting random game location
🔍 Executed query: {
  text: '\n' +
    '      SELECT l.id, l.name, l.country, l.coordinates, l.has_pano, l.pano_id\n' +
    '      FROM locations l \n' +
    '...',
  duration: '191ms',
  rows: 1
}
✅ GameService: Selected location: Sydney Harbour Bridge, Australia
🆕 GameService: Creating round for location 111, user null
✅ GameService: Created round 29
✅ New round created: 29 for location: Sydney Harbour Bridge
127.0.0.1 - - [18/Sep/2025:20:03:56 +0000] "GET /api/game/newRound HTTP/1.1" 200 120 "-" "okhttp/4.12.0"
🔍 Game Route: Street View check for location 29
127.0.0.1 - - [18/Sep/2025:20:03:56 +0000] "GET /api/game/streetview/check/29 HTTP/1.1" 404 71 "-" "okhttp/4.12.0"
🎮 Game Route: /newRound called
🎲 GameService: Getting random game location
✅ GameService: Selected location: Golden Gate Bridge, United States
🆕 GameService: Creating round for location 110, user null
✅ GameService: Created round 30
✅ New round created: 30 for location: Golden Gate Bridge
127.0.0.1 - - [18/Sep/2025:20:04:26 +0000] "GET /api/game/newRound HTTP/1.1" 200 114 "-" "okhttp/4.12.0"
🔍 Game Route: Street View check for location 30
127.0.0.1 - - [18/Sep/2025:20:04:26 +0000] "GET /api/game/streetview/check/30 HTTP/1.1" 404 71 "-" "okhttp/4.12.0"
```

## 📋 Inhaltsverzeichnis

1. [Architektur-Übersicht](#architektur)
2. [Technologie-Stack](#technologien)
3. [Datenbank-Design](#datenbank)
4. [API-Endpunkte](#api)
5. [Services und Business Logic](#services)
6. [Real-time Kommunikation](#realtime)
7. [Sicherheit und Middleware](#sicherheit)
8. [Deployment und Konfiguration](#deployment)
9. [Testing-Strategien](#testing)
10. [Monitoring und Diagnostik](#monitoring)

---

## 🏗️ Architektur-Übersicht {#architektur}

### Backend-For-Frontend (BFF) Pattern

Das System implementiert das **Backend-For-Frontend Pattern**, bei dem der Server die gesamte Spiellogik verwaltet, während der Client (Android App) primär für die Darstellung zuständig ist.

#### Kernprinzipien:

**Server-Side Game Logic:**

- Rundenverwaltung und Spielstatus
- Scoring-Algorithmen und Validierung
- Location-Management mit PostGIS
- Street View API-Integration
- Battle Royale Session Management

**Client-Side Rendering:**

- Google Maps SDK für Street View
- UI/UX und Benutzereingaben
- Real-time Updates via Socket.IO
- Offline-Caching für Performance

### Schichtenmodell

```
┌─────────────────────────────────────┐
│           Android Client            │
├─────────────────────────────────────┤
│          HTTP/Socket.IO             │
├─────────────────────────────────────┤
│        Express.js Server            │
├─────────────────────────────────────┤
│     Business Logic Services         │
├─────────────────────────────────────┤
│      PostgreSQL + PostGIS           │
└─────────────────────────────────────┘
```

---

## 🛠️ Technologie-Stack {#technologien}

### Core Framework

```json
{
  "runtime": "Node.js",
  "framework": "Express.js 4.18.2",
  "database": "PostgreSQL 16.9",
  "spatial": "PostGIS Extension",
  "realtime": "Socket.IO 4.8.1"
}
```

### Dependencies im Detail

#### **Production Dependencies:**

```javascript
{
  // Server Framework
  "express": "^4.18.2",
  "cors": "^2.8.5",
  "helmet": "^8.1.0",

  // Database & ORM
  "pg": "^8.16.3",
  "postgis": "^1.0.5",

  // Authentication & Security
  "bcryptjs": "^3.0.2",
  "jsonwebtoken": "^9.0.2",
  "express-rate-limit": "^8.0.1",

  // Performance & Optimization
  "compression": "^1.8.1",
  "node-cache": "^5.1.2",

  // Real-time Communication
  "socket.io": "^4.8.1",

  // External APIs
  "axios": "^1.10.0",

  // Configuration
  "dotenv": "^17.2.0",

  // Logging
  "morgan": "^1.10.1"
}
```

#### **Development Dependencies:**

```javascript
{
  "jest": "^30.0.4",
  "supertest": "^7.1.3",
  "nodemon": "^3.1.10",
  "socket.io-client": "^4.8.1"
}
```

### Database Migrations System

```javascript
{
  "db-migrate": "^0.11.14",
  "db-migrate-pg": "^1.5.2"
}
```

---

## 🗄️ Datenbank-Design {#datenbank}

### PostgreSQL + PostGIS Schema

#### **Kern-Tabellen:**

**1. users (Benutzerverwaltung)**

```sql
CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  avatar_url VARCHAR(255),
  total_score BIGINT DEFAULT 0,
  games_played INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT NOW(),
  last_active TIMESTAMP DEFAULT NOW()
);
```

**2. locations (Geografische Daten mit PostGIS)**

```sql
CREATE TABLE locations (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100),
  country VARCHAR(50) NOT NULL,
  city VARCHAR(50),
  coordinates GEOMETRY(POINT, 4326) NOT NULL,
  difficulty INTEGER DEFAULT 3 CHECK (difficulty >= 1 AND difficulty <= 5),
  category VARCHAR(30),
  image_urls TEXT[] NOT NULL,
  hints JSONB DEFAULT '{}',
  view_count INTEGER DEFAULT 0,

  -- Street View Metadata (BFF Enhancement)
  has_pano BOOLEAN DEFAULT NULL,
  pano_id VARCHAR(255) DEFAULT NULL,

  created_at TIMESTAMP DEFAULT NOW()
);
```

**3. rounds (BFF Spielrunden)**

```sql
CREATE TABLE rounds (
  id SERIAL PRIMARY KEY,
  location_id INTEGER NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
  user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
  status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'completed', 'expired')),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  completed_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
  expires_at TIMESTAMP WITH TIME ZONE DEFAULT (NOW() + INTERVAL '1 hour')
);
```

**4. guesses (Spielerversuche)**

```sql
CREATE TABLE guesses (
  id SERIAL PRIMARY KEY,
  round_id INTEGER NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
  user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
  guess_lat DECIMAL(10, 8) NOT NULL,
  guess_lng DECIMAL(11, 8) NOT NULL,
  actual_lat DECIMAL(10, 8) NOT NULL,
  actual_lng DECIMAL(11, 8) NOT NULL,
  distance_meters INTEGER NOT NULL,
  score INTEGER NOT NULL CHECK (score >= 0 AND score <= 5000),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

**5. game_sessions (Session Management)**

```sql
CREATE TABLE game_sessions (
  id SERIAL PRIMARY KEY,
  session_type VARCHAR(20) NOT NULL CHECK (session_type IN ('single', 'battle', 'challenge')),
  session_code VARCHAR(6) UNIQUE,
  created_by INTEGER REFERENCES users(id),
  status VARCHAR(20) DEFAULT 'waiting' CHECK (status IN ('waiting', 'active', 'finished')),
  settings JSONB DEFAULT '{}',
  location_ids INTEGER[],
  max_players INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT NOW(),
  started_at TIMESTAMP,
  finished_at TIMESTAMP
);
```

#### **Performance-Indizes:**

**Geografische Abfragen (PostGIS):**

```sql
CREATE INDEX idx_locations_coordinates
ON locations USING GIST(coordinates);
```

**BFF Performance Indizes:**

```sql
CREATE INDEX idx_rounds_location_id ON rounds(location_id);
CREATE INDEX idx_rounds_user_id ON rounds(user_id);
CREATE INDEX idx_rounds_status ON rounds(status);
CREATE INDEX idx_guesses_round_id ON guesses(round_id);
```

**Street View Optimierung:**

```sql
CREATE INDEX idx_locations_has_pano
ON locations(has_pano) WHERE has_pano IS NOT FALSE;

CREATE INDEX idx_locations_pano_id
ON locations(pano_id) WHERE pano_id IS NOT NULL;
```

### Migration System

**Automatisierte Schema-Verwaltung:**

```javascript
// Migration Runner
class MigrationRunner {
  async runMigrations() {
    const appliedMigrations = await this.getAppliedMigrations();
    const migrationFiles = await this.getMigrationFiles();

    for (const file of migrationFiles) {
      const migrationName = path.basename(file, ".js");

      if (!appliedMigrations.includes(migrationName)) {
        const migration = require(path.join(this.migrationsPath, file));
        await migration.up(database);

        await database.query(
          "INSERT INTO schema_migrations (migration_name) VALUES ($1)",
          [migrationName]
        );
      }
    }
  }
}
```

**Verfügbare Migrationen:**

- `001-initial-schema.js` - Basis-Tabellen und PostGIS
- `002-create-indexes.js` - Performance-Indizes
- `003-bff-game-tables.js` - BFF Pattern Support

---

## 📡 API-Endpunkte {#api}

### RESTful API Design

#### **Game Endpoints (BFF Pattern)**

**GET /api/games/newRound** | **GET /api/game/newRound**

```javascript
// Neue Spielrunde starten
Response: {
  "id": 123,
  "lat": 48.8566,
  "lng": 2.3522,
  "name": "Paris",
  "country": "France",
  "pano_id": "CAoSLEFGMVFpcE02..."
}
```

**POST /api/games/guess** | **POST /api/game/guess**

```javascript
// Vermutung einreichen
Request: {
  "roundId": 123,
  "guessLat": 48.8566,
  "guessLng": 2.3522
}

Response: {
  "distance": 1234,
  "score": 4500,
  "actualLocation": {
    "lat": 48.8566,
    "lng": 2.3522,
    "name": "Paris"
  }
}
```

**GET /api/games/round/:id** | **GET /api/game/round/:id**

```javascript
// Runden-Details abrufen
Response: {
  "round": {
    "id": 123,
    "status": "completed",
    "location": {...},
    "guesses": [...]
  }
}
```

**GET /api/game/streetview/check/:locationId**

```javascript
// Street View Verfügbarkeit prüfen (Android Kompatibilität)
Response: {
  "valid": true,
  "location_id": 89,
  "response_time_ms": 150,
  "cached": false,
  "pano_id": "CAoSLEFGMVFpcE06..."
}
```

#### **Location Endpoints**

**GET /api/locations**

```javascript
// Locations mit Filterung
Query: {
  "country": "France",
  "difficulty": 3,
  "category": "urban",
  "limit": 10
}
```

**GET /api/locations/random**

```javascript
// Zufällige Location
Response: {
  "id": 123,
  "coordinates": {
    "latitude": 48.8566,
    "longitude": 2.3522
  },
  "country": "France",
  "difficulty": 3
}
```

**GET /api/locations/:id/streetview/diagnose**

```javascript
// Street View Diagnostik
Response: {
  "location": {...},
  "streetview": {
    "available": true,
    "pano_id": "...",
    "metadata": {...}
  },
  "recommendations": [...]
}
```

#### **Battle Royale Endpoints**

**POST /api/battle-royale/create**

```javascript
// Battle Royale Session erstellen
Request: {
  "settings": {
    "maxPlayers": 10,
    "roundDuration": 120,
    "difficulty": "medium"
  }
}

Response: {
  "code": "ABC123",
  "creatorId": "user123",
  "status": "waiting"
}
```

**GET /api/battle-royale/stats**

```javascript
// Battle Royale Statistiken
Response: {
  "activeSessions": 5,
  "totalPlayers": 42,
  "connectedUsers": 38
}
```

#### **Authentication Endpoints**

**POST /api/auth/register**
**POST /api/auth/login**
**POST /api/auth/refresh**
**GET /api/auth/profile**

#### **System Endpoints**

**GET /health**

```javascript
// System Health Check
Response: {
  "status": "OK",
  "timestamp": "2025-09-18T19:00:00Z",
  "database": "connected",
  "version": "1.0.0"
}
```

### Android App Kompatibilität

**Duale Route-Unterstützung:**

```javascript
// Server.js Route-Aliases
app.use("/api/games", require("./routes/games"));
app.use("/api/game", require("./routes/games")); // Android Kompatibilität
```

**Warum nötig?**

- Android App erwartet singular "game" Pfade
- Backend Design nutzt plural "games" Pfade
- Alias-System ermöglicht beide Varianten

---

## 🔧 Services und Business Logic {#services}

### Service-Architektur

#### **gameService.js - Kern-Spiellogik**

**Hauptfunktionen:**

```javascript
// Zufällige Location mit Street View Coverage
async function getRandomGameLocation() {
  const result = await database.query(`
    SELECT l.id, l.name, l.country, l.coordinates, l.has_pano, l.pano_id
    FROM locations l 
    WHERE l.has_pano IS NOT FALSE 
      AND ST_IsValid(l.coordinates)
    ORDER BY RANDOM() 
    LIMIT 1
  `);
}

// Neue Spielrunde erstellen
async function createRound(locationId, userId = null) {
  const result = await database.query(
    `
    INSERT INTO rounds (location_id, user_id, status, expires_at) 
    VALUES ($1, $2, 'active', NOW() + INTERVAL '1 hour') 
    RETURNING id, created_at
  `,
    [locationId, userId]
  );
}

// Vermutung verarbeiten und bewerten
async function processGuess(guessData) {
  const distance = calculateDistance(
    guessData.guessLat,
    guessData.guessLng,
    actualLat,
    actualLng
  );

  const score = calculateScore(distance);
  // ... Database operations
}
```

**Scoring-Algorithmus:**

```javascript
function calculateScore(distanceMeters) {
  const MAX_SCORE = 5000;
  const PERFECT_DISTANCE = 100; // 100m = fast perfekt
  const MAX_DISTANCE = 20000000; // 20,000km = 0 Punkte

  if (distanceMeters <= PERFECT_DISTANCE) {
    return MAX_SCORE;
  }

  // Exponential decay für realistische Punkteverteilung
  const factor = Math.pow(distanceMeters / MAX_DISTANCE, 0.5);
  return Math.max(0, Math.round(MAX_SCORE * (1 - factor)));
}
```

**Street View Validierung:**

```javascript
async function validateStreetViewLocation(lat, lng) {
  const serverApiKey = process.env.SERVER_GOOGLE_KEY;
  const metadataUrl = `https://maps.googleapis.com/maps/api/streetview/metadata?location=${lat},${lng}&key=${serverApiKey}`;

  const response = await fetch(metadataUrl);
  const data = await response.json();

  if (data.status === "OK") {
    return {
      available: true,
      pano_id: data.pano_id,
    };
  }
  return { available: false };
}
```

#### **battleRoyaleService.js - Multiplayer Management**

**Session Management:**

```javascript
class BattleRoyaleManager {
  constructor() {
    this.sessions = new Map();
    this.config = {
      maxPlayers: 10,
      roundDuration: 120000, // 2 Minuten
      maxRounds: 5,
    };
  }

  async createSession(creatorId, settings = {}) {
    const code = this.generateSessionCode();

    const locations = await locationService.getRandomLocations({
      count: this.config.maxRounds,
      difficulty: settings.difficulty || null,
    });

    const session = {
      code,
      creatorId,
      players: [],
      status: "waiting",
      currentRound: 0,
      locations: locations,
      settings: { ...this.config, ...settings },
    };

    this.sessions.set(code, session);
    return session;
  }
}
```

#### **socketService.js - Real-time Kommunikation**

**Socket.IO Integration:**

```javascript
class SocketService {
  initialize(server) {
    this.io = new Server(server, {
      cors: {
        origin: process.env.CORS_ORIGIN || "*",
        methods: ["GET", "POST"],
      },
    });

    this.io.on("connection", (socket) => {
      socket.on("battle:join", async (data, callback) => {
        try {
          const result = await battleRoyaleManager.joinSession(
            data.sessionCode,
            data.userId,
            data.username,
            socket.id
          );

          // Broadcast an alle Session-Teilnehmer
          this.sendToSession(data.sessionCode, "battle:player_joined", {
            player: result.player,
            totalPlayers: result.session.players.length,
          });

          callback({ success: true, session: result.session });
        } catch (error) {
          callback({ success: false, error: error.message });
        }
      });
    });
  }

  // Broadcast an Session-Teilnehmer
  sendToSession(sessionCode, event, data) {
    this.io.to(sessionCode).emit(event, data);
  }
}
```

#### **locationService.js - Geografische Daten**

**PostGIS Abfragen:**

```javascript
class LocationService {
  async getRandomLocations(options = {}) {
    let query = `
      SELECT id, name, country, city,
             ST_X(coordinates) as longitude,
             ST_Y(coordinates) as latitude,
             difficulty, category, image_urls
      FROM locations 
      WHERE ST_IsValid(coordinates)
    `;

    const params = [];
    let paramIndex = 1;

    if (options.country) {
      query += ` AND country = $${paramIndex++}`;
      params.push(options.country);
    }

    if (options.difficulty) {
      query += ` AND difficulty = $${paramIndex++}`;
      params.push(options.difficulty);
    }

    query += ` ORDER BY RANDOM() LIMIT $${paramIndex}`;
    params.push(options.count || 10);

    const result = await database.query(query, params);
    return result.rows;
  }

  async findNearbyLocations(lat, lng, radius = 1000) {
    const result = await database.query(
      `
      SELECT id, name, country,
             ST_Distance(
               coordinates, 
               ST_GeomFromText('POINT($2 $1)', 4326)
             ) as distance
      FROM locations 
      WHERE ST_DWithin(
        coordinates,
        ST_GeomFromText('POINT($2 $1)', 4326),
        $3
      )
      ORDER BY distance
    `,
      [lat, lng, radius]
    );

    return result.rows;
  }
}
```

#### **cacheService.js - Performance Optimierung**

**Memory Caching:**

```javascript
class CacheService {
  constructor() {
    this.cache = new NodeCache({
      stdTTL: 600, // 10 Minuten default
      checkperiod: 120, // Cleanup alle 2 Minuten
      maxKeys: 1000,
    });
  }

  async getOrSet(key, fetcher, ttl = 600) {
    let value = this.cache.get(key);

    if (value === undefined) {
      value = await fetcher();
      this.cache.set(key, value, ttl);
    }

    return value;
  }

  // Street View Metadaten cachen
  async cacheStreetViewData(locationId, metadata) {
    const key = `streetview:${locationId}`;
    this.cache.set(key, metadata, 3600); // 1 Stunde
  }
}
```

#### **streetViewService.js - Google API Integration**

**Multi-Platform URL Generation:**

```javascript
class StreetViewService {
  generateStaticUrl(lat, lng, options = {}) {
    const params = new URLSearchParams({
      location: `${lat},${lng}`,
      size: options.size || "640x640",
      fov: options.fov || "90",
      heading: options.heading || "0",
      pitch: options.pitch || "0",
      key: process.env.GOOGLE_MAPS_API_KEY,
    });

    return `https://maps.googleapis.com/maps/api/streetview?${params}`;
  }

  generateEmbedUrl(lat, lng, options = {}) {
    const params = new URLSearchParams({
      q: `${lat},${lng}`,
      fov: options.fov || "90",
      heading: options.heading || "0",
      pitch: options.pitch || "0",
    });

    return `https://www.google.com/maps/embed/v1/streetview?key=${process.env.GOOGLE_MAPS_API_KEY}&${params}`;
  }

  async validateCoverage(locations) {
    const results = [];

    for (const location of locations) {
      try {
        const metadata = await this.getMetadata(
          location.latitude,
          location.longitude
        );

        results.push({
          location,
          validation: {
            hasCoverage: metadata.status === "OK",
            panoId: metadata.pano_id,
            date: metadata.date,
          },
        });
      } catch (error) {
        results.push({
          location,
          validation: { hasCoverage: false, error: error.message },
        });
      }
    }

    return results;
  }
}
```

---

## 🔌 Real-time Kommunikation {#realtime}

### Socket.IO Integration

#### **Connection Management**

**Server-Side Setup:**

```javascript
// server.js
const socketService = require("./services/socketService");

if (require.main === module) {
  socketService.initialize(server);

  server.listen(PORT, "0.0.0.0", (err) => {
    console.log(`🚀 Server running on port ${PORT}`);
    console.log(`🔌 Socket.IO server ready for real-time connections`);
  });
}
```

**Socket Event Handling:**

```javascript
// socketService.js Event-Mapping
this.io.on("connection", (socket) => {
  console.log(`🔌 Client connected: ${socket.id}`);

  // Battle Royale Events
  socket.on("battle:create", this.handleCreateSession.bind(this));
  socket.on("battle:join", this.handleJoinSession.bind(this));
  socket.on("battle:leave", this.handleLeaveSession.bind(this));
  socket.on("battle:guess", this.handlePlayerGuess.bind(this));

  // Game State Events
  socket.on("game:start_round", this.handleStartRound.bind(this));
  socket.on("game:submit_guess", this.handleSubmitGuess.bind(this));

  socket.on("disconnect", () => {
    console.log(`🔌 Client disconnected: ${socket.id}`);
    this.handleDisconnect(socket.id);
  });
});
```

#### **Battle Royale Real-time Flow**

**1. Session Creation:**

```javascript
// Client → Server
socket.emit(
  "battle:create",
  {
    settings: {
      maxPlayers: 10,
      roundDuration: 120,
      difficulty: "medium",
    },
  },
  (response) => {
    if (response.success) {
      console.log("Session created:", response.sessionCode);
    }
  }
);
```

**2. Player Join Broadcast:**

```javascript
// Server → All Session Players
this.sendToSession(sessionCode, "battle:player_joined", {
  player: {
    id: userId,
    username: username,
    score: 0,
  },
  totalPlayers: session.players.length,
});
```

**3. Round Start Synchronization:**

```javascript
// Server → All Session Players
this.sendToSession(sessionCode, "battle:round_started", {
  round: session.currentRound,
  location: {
    lat: location.latitude,
    lng: location.longitude,
    name: location.name,
  },
  timeLimit: session.settings.roundDuration,
});
```

**4. Live Leaderboard Updates:**

```javascript
// Nach jedem Guess Update
this.sendToSession(sessionCode, "battle:leaderboard_update", {
  leaderboard: session.players
    .sort((a, b) => b.totalScore - a.totalScore)
    .map((player, index) => ({
      rank: index + 1,
      username: player.username,
      score: player.totalScore,
      lastGuess: player.lastGuess,
    })),
});
```

#### **Error Handling und Reconnection**

**Client-Side Reconnection:**

```javascript
// Android App Socket Configuration
socket.on("connect_error", (error) => {
  console.log("Connection failed:", error.message);

  // Exponential backoff retry
  setTimeout(() => {
    socket.connect();
  }, Math.min(1000 * Math.pow(2, retryCount), 30000));
});

socket.on("reconnect", (attemptNumber) => {
  console.log("Reconnected after", attemptNumber, "attempts");

  // Re-join Battle Royale session if active
  if (currentSessionCode) {
    socket.emit("battle:rejoin", {
      sessionCode: currentSessionCode,
      userId: currentUserId,
    });
  }
});
```

**Server-Side Session Recovery:**

```javascript
handleRejoinSession(socket, data, callback) {
  try {
    const session = this.battleRoyaleManager.getSession(data.sessionCode);

    if (session) {
      // Update Socket ID for reconnected player
      const player = session.players.find(p => p.id === data.userId);
      if (player) {
        player.socketId = socket.id;
        socket.join(data.sessionCode);

        callback({
          success: true,
          session: this.sanitizeSession(session),
          currentRound: session.currentRound
        });
      }
    }
  } catch (error) {
    callback({ success: false, error: error.message });
  }
}
```

---

## 🛡️ Sicherheit und Middleware {#sicherheit}

### Security Stack

#### **1. Express Security Middleware**

```javascript
// Helmet.js - Security Headers
app.use(
  helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        styleSrc: ["'self'", "'unsafe-inline'"],
        scriptSrc: ["'self'", "maps.googleapis.com"],
        imgSrc: ["'self'", "*.googleapis.com", "data:"],
        connectSrc: ["'self'", "*.googleapis.com"],
      },
    },
    hsts: {
      maxAge: 31536000,
      includeSubDomains: true,
      preload: true,
    },
  })
);

// CORS Configuration
app.use(
  cors({
    origin: process.env.CORS_ORIGIN || "*",
    credentials: true,
    methods: ["GET", "POST", "PUT", "DELETE"],
    allowedHeaders: ["Content-Type", "Authorization"],
  })
);
```

#### **2. Rate Limiting**

```javascript
// Express Rate Limit
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 Minuten
  max: 100, // 100 Requests pro IP pro Window
  message: {
    error: "Too many requests",
    retryAfter: "15 minutes",
  },
  standardHeaders: true,
  legacyHeaders: false,
});

app.use("/api/", limiter);

// Spezielle Limits für Battle Royale
const battleRoyaleLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 Minute
  max: 20, // 20 Actions pro Minute
  keyGenerator: (req) => req.user?.id || req.ip,
});

app.use("/api/battle-royale/", battleRoyaleLimiter);
```

#### **3. Authentication Middleware**

```javascript
// JWT Token Authentication
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers["authorization"];
  const token = authHeader && authHeader.split(" ")[1];

  if (!token) {
    return res.status(401).json({
      error: "Access token required",
      message: "No authorization header provided",
    });
  }

  jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({
        error: "Invalid token",
        message: "Token verification failed",
      });
    }

    req.user = user;
    next();
  });
};

// Optional Authentication (für Guest-Zugriff)
const optionalAuth = (req, res, next) => {
  const authHeader = req.headers["authorization"];
  const token = authHeader && authHeader.split(" ")[1];

  if (token) {
    jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
      if (!err) {
        req.user = user;
      }
    });
  }

  next();
};
```

#### **4. Input Validation**

```javascript
// Game Guess Validation
const validateGuess = (req, res, next) => {
  const { roundId, guessLat, guessLng } = req.body;

  // Validate Round ID
  if (!roundId || !Number.isInteger(roundId) || roundId <= 0) {
    return res.status(400).json({
      error: "Invalid round ID",
      message: "Round ID must be a positive integer",
    });
  }

  // Validate Coordinates
  if (!isValidLatitude(guessLat) || !isValidLongitude(guessLng)) {
    return res.status(400).json({
      error: "Invalid coordinates",
      message:
        "Latitude must be between -90 and 90, longitude between -180 and 180",
    });
  }

  next();
};

function isValidLatitude(lat) {
  return typeof lat === "number" && lat >= -90 && lat <= 90;
}

function isValidLongitude(lng) {
  return typeof lng === "number" && lng >= -180 && lng <= 180;
}
```

#### **5. Database Security**

**SQL Injection Prevention:**

```javascript
// Parameterized Queries
const result = await database.query(
  `
  SELECT * FROM locations 
  WHERE country = $1 AND difficulty = $2
`,
  [country, difficulty]
);

// NIEMALS String Concatenation:
// ❌ FALSCH: `SELECT * FROM locations WHERE country = '${country}'`
```

**Connection Security:**

```javascript
// database/connection.js
const pool = new Pool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT,
  database: process.env.DB_NAME,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  ssl:
    process.env.DB_SSL === "true"
      ? {
          rejectUnauthorized: false,
        }
      : false,
  max: 20, // Connection Pool Limit
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});
```

#### **6. Environment Security**

**.env Konfiguration:**

```bash
# Database
DB_HOST=ep-steep-star-a2ymqe47-pooler.eu-central-1.aws.neon.tech
DB_NAME=neondb
DB_USER=neondb_owner
DB_PASSWORD=<secure_password>
DB_SSL=true

# JWT
JWT_SECRET=<256-bit-secure-key>
JWT_EXPIRES_IN=7d

# Google APIs
GOOGLE_MAPS_API_KEY=<restricted_api_key>
SERVER_GOOGLE_KEY=<server_side_api_key>

# CORS
CORS_ORIGIN=https://yourdomain.com

# Rate Limiting
RATE_LIMIT_WINDOW_MS=900000
RATE_LIMIT_MAX=100
```

**Environment Validation:**

```javascript
// Startup Environment Check
const requiredEnvVars = [
  "DB_HOST",
  "DB_NAME",
  "DB_USER",
  "DB_PASSWORD",
  "JWT_SECRET",
  "GOOGLE_MAPS_API_KEY",
];

for (const envVar of requiredEnvVars) {
  if (!process.env[envVar]) {
    console.error(`❌ Missing required environment variable: ${envVar}`);
    process.exit(1);
  }
}
```

---

## 📦 Deployment und Konfiguration {#deployment}

### Production Deployment

#### **Vercel Configuration**

**vercel.json:**

```json
{
  "version": 2,
  "builds": [
    {
      "src": "src/server.js",
      "use": "@vercel/node"
    }
  ],
  "routes": [
    {
      "src": "/(.*)",
      "dest": "src/server.js"
    }
  ],
  "env": {
    "NODE_ENV": "production"
  },
  "functions": {
    "src/server.js": {
      "maxDuration": 30
    }
  }
}
```

#### **Docker Configuration**

**Dockerfile:**

```dockerfile
FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./
RUN npm ci --only=production

# Copy source code
COPY src/ ./src/
COPY config/ ./config/

# Set environment
ENV NODE_ENV=production
ENV PORT=3000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:3000/health || exit 1

EXPOSE 3000

USER node

CMD ["npm", "start"]
```

**docker-compose.yml:**

```yaml
version: "3.8"

services:
  backend:
    build: .
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - DB_HOST=postgres
      - REDIS_URL=redis://redis:6379
    depends_on:
      - postgres
      - redis
    restart: unless-stopped

  postgres:
    image: postgis/postgis:16-3.4
    environment:
      POSTGRES_DB: geoguessr
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
```

#### **CI/CD Pipeline (GitHub Actions)**

**.github/workflows/deploy.yml:**

```yaml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: 18
          cache: "npm"

      - name: Install dependencies
        run: npm ci

      - name: Run tests
        run: npm test
        env:
          NODE_ENV: test
          DB_HOST: localhost
          DB_NAME: test_db

  deploy:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Deploy to Vercel
        uses: amondnet/vercel-action@v25
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.ORG_ID }}
          vercel-project-id: ${{ secrets.PROJECT_ID }}
          vercel-args: "--prod"
```

#### **Environment-spezifische Konfiguration**

**config/production.js:**

```javascript
module.exports = {
  server: {
    port: process.env.PORT || 3000,
    host: "0.0.0.0",
  },
  database: {
    ssl: {
      rejectUnauthorized: false,
    },
    pool: {
      max: 20,
      min: 5,
      acquire: 30000,
      idle: 10000,
    },
  },
  cache: {
    ttl: 3600, // 1 hour
    maxKeys: 1000,
  },
  logging: {
    level: "info",
    format: "combined",
  },
  security: {
    rateLimit: {
      windowMs: 15 * 60 * 1000,
      max: 100,
    },
  },
};
```

**config/development.js:**

```javascript
module.exports = {
  server: {
    port: 3000,
    host: "localhost",
  },
  database: {
    ssl: false,
    pool: {
      max: 5,
      min: 1,
    },
  },
  cache: {
    ttl: 300, // 5 minutes
    maxKeys: 100,
  },
  logging: {
    level: "debug",
    format: "dev",
  },
  security: {
    rateLimit: {
      windowMs: 15 * 60 * 1000,
      max: 1000, // Relaxed for development
    },
  },
};
```

---

## 🧪 Testing-Strategien {#testing}

### Test Framework Setup

#### **Jest Configuration**

**package.json test setup:**

```json
{
  "jest": {
    "testEnvironment": "node",
    "setupFilesAfterEnv": ["<rootDir>/tests/setup.js"],
    "testTimeout": 30000,
    "collectCoverageFrom": [
      "src/**/*.js",
      "!src/database/migrations/**",
      "!src/database/seeders/**"
    ],
    "coverageThreshold": {
      "global": {
        "branches": 70,
        "functions": 70,
        "lines": 70,
        "statements": 70
      }
    }
  }
}
```

**tests/setup.js:**

```javascript
const database = require("../src/database/connection");

// Global test setup
beforeAll(async () => {
  await database.connect();
});

afterAll(async () => {
  await database.disconnect();
});

// Mock external services in test environment
if (process.env.NODE_ENV === "test") {
  // Mock Google Maps API
  jest.mock("../src/services/streetViewService", () => ({
    validateCoverage: jest.fn().mockResolvedValue([
      {
        location: { id: 1 },
        validation: { hasCoverage: true, panoId: "test_pano" },
      },
    ]),
  }));
}
```

#### **Unit Tests - Services**

**tests/services/gameService.test.js:**

```javascript
const gameService = require("../../src/services/gameService");
const database = require("../../src/database/connection");

describe("GameService", () => {
  describe("getRandomGameLocation", () => {
    test("should return a valid location with coordinates", async () => {
      const location = await gameService.getRandomGameLocation();

      expect(location).toBeDefined();
      expect(location.id).toBeGreaterThan(0);
      expect(location.coordinates).toMatchObject({
        latitude: expect.any(Number),
        longitude: expect.any(Number),
      });
      expect(location.coordinates.latitude).toBeGreaterThanOrEqual(-90);
      expect(location.coordinates.latitude).toBeLessThanOrEqual(90);
      expect(location.coordinates.longitude).toBeGreaterThanOrEqual(-180);
      expect(location.coordinates.longitude).toBeLessThanOrEqual(180);
    });
  });

  describe("calculateScore", () => {
    test("should return maximum score for perfect guess", () => {
      const score = gameService.calculateScore(0);
      expect(score).toBe(5000);
    });

    test("should return 0 for very far guess", () => {
      const score = gameService.calculateScore(20000000);
      expect(score).toBe(0);
    });

    test("should return decreasing scores for increasing distances", () => {
      const score1km = gameService.calculateScore(1000);
      const score10km = gameService.calculateScore(10000);
      const score100km = gameService.calculateScore(100000);

      expect(score1km).toBeGreaterThan(score10km);
      expect(score10km).toBeGreaterThan(score100km);
    });
  });

  describe("createRound", () => {
    test("should create a new round successfully", async () => {
      const location = await gameService.getRandomGameLocation();
      const round = await gameService.createRound(location.id);

      expect(round).toMatchObject({
        id: expect.any(Number),
        created_at: expect.any(Date),
      });
    });
  });
});
```

#### **Integration Tests - API Endpoints**

**tests/api/games.test.js:**

```javascript
const request = require("supertest");
const { app } = require("../../src/server");
const database = require("../../src/database/connection");

describe("Games API", () => {
  beforeAll(async () => {
    await database.connect();
  });

  afterAll(async () => {
    await database.disconnect();
  });

  describe("GET /api/games/newRound", () => {
    test("should return a new round", async () => {
      const response = await request(app)
        .get("/api/games/newRound")
        .expect(200);

      expect(response.body).toMatchObject({
        id: expect.any(Number),
        lat: expect.any(Number),
        lng: expect.any(Number),
        name: expect.any(String),
        country: expect.any(String),
      });
    });
  });

  describe("POST /api/games/guess", () => {
    test("should process a guess and return score", async () => {
      // First, get a new round
      const roundResponse = await request(app)
        .get("/api/games/newRound")
        .expect(200);

      const roundId = roundResponse.body.id;

      // Submit a guess
      const guessResponse = await request(app)
        .post("/api/games/guess")
        .send({
          roundId: roundId,
          guessLat: 48.8566,
          guessLng: 2.3522,
        })
        .expect(200);

      expect(guessResponse.body).toMatchObject({
        distance: expect.any(Number),
        score: expect.any(Number),
        actualLocation: expect.objectContaining({
          lat: expect.any(Number),
          lng: expect.any(Number),
        }),
      });

      expect(guessResponse.body.score).toBeGreaterThanOrEqual(0);
      expect(guessResponse.body.score).toBeLessThanOrEqual(5000);
    });

    test("should reject invalid coordinates", async () => {
      const response = await request(app)
        .post("/api/games/guess")
        .send({
          roundId: 1,
          guessLat: 91, // Invalid latitude
          guessLng: 2.3522,
        })
        .expect(400);

      expect(response.body).toMatchObject({
        error: expect.any(String),
      });
    });
  });
});
```

#### **Socket.IO Tests**

**tests/socket/battleRoyale.test.js:**

```javascript
const Client = require("socket.io-client");
const { server } = require("../../src/server");

describe("Socket.IO Battle Royale", () => {
  let clientSocket;
  let serverSocket;

  beforeAll((done) => {
    server.listen(() => {
      const port = server.address().port;
      clientSocket = new Client(`http://localhost:${port}`);

      server.on("connection", (socket) => {
        serverSocket = socket;
      });

      clientSocket.on("connect", done);
    });
  });

  afterAll(() => {
    server.close();
    clientSocket.close();
  });

  test("should create battle royale session", (done) => {
    clientSocket.emit(
      "battle:create",
      {
        settings: {
          maxPlayers: 5,
          roundDuration: 120,
        },
      },
      (response) => {
        expect(response.success).toBe(true);
        expect(response.sessionCode).toMatch(/^[A-Z0-9]{6}$/);
        done();
      }
    );
  });

  test("should join battle royale session", (done) => {
    // First create a session
    clientSocket.emit(
      "battle:create",
      {
        settings: { maxPlayers: 5 },
      },
      (createResponse) => {
        const sessionCode = createResponse.sessionCode;

        // Then join it
        clientSocket.emit(
          "battle:join",
          {
            sessionCode: sessionCode,
            userId: "test_user",
            username: "Test Player",
          },
          (joinResponse) => {
            expect(joinResponse.success).toBe(true);
            expect(joinResponse.session.players).toHaveLength(1);
            done();
          }
        );
      }
    );
  });
});
```

#### **Performance Tests**

**tests/performance/load.test.js:**

```javascript
const axios = require("axios");

describe("Performance Tests", () => {
  const BASE_URL = "http://localhost:3000";

  test("should handle concurrent newRound requests", async () => {
    const concurrentRequests = 10;
    const startTime = Date.now();

    const promises = Array(concurrentRequests)
      .fill()
      .map(() => axios.get(`${BASE_URL}/api/games/newRound`));

    const responses = await Promise.all(promises);
    const endTime = Date.now();

    // All requests should succeed
    responses.forEach((response) => {
      expect(response.status).toBe(200);
      expect(response.data.id).toBeDefined();
    });

    // Should complete within reasonable time
    const totalTime = endTime - startTime;
    expect(totalTime).toBeLessThan(5000); // 5 seconds

    console.log(
      `${concurrentRequests} concurrent requests completed in ${totalTime}ms`
    );
  });

  test("should maintain database connections under load", async () => {
    const requests = Array(50)
      .fill()
      .map((_, index) =>
        axios
          .get(`${BASE_URL}/api/locations/random`)
          .then((response) => ({ index, status: response.status }))
          .catch((error) => ({ index, error: error.message }))
      );

    const results = await Promise.all(requests);
    const errors = results.filter((result) => result.error);

    expect(errors).toHaveLength(0);
    console.log(`50 database requests completed with 0 errors`);
  });
});
```

### Test Coverage und Continuous Integration

**Test Scripts:**

```json
{
  "scripts": {
    "test": "jest --detectOpenHandles --forceExit",
    "test:watch": "jest --watch --detectOpenHandles",
    "test:coverage": "jest --coverage --detectOpenHandles --forceExit",
    "test:integration": "jest tests/api/ --detectOpenHandles --forceExit",
    "test:unit": "jest tests/services/ --detectOpenHandles --forceExit"
  }
}
```

---

## 🔍 Monitoring und Diagnostik {#monitoring}

### Health Monitoring

#### **Health Check Endpoint**

**System Health Dashboard:**

```javascript
// GET /health
app.get("/health", async (req, res) => {
  try {
    // Database connectivity test
    const dbResult = await database.query("SELECT NOW() as timestamp");
    const dbLatency =
      Date.now() - new Date(dbResult.rows[0].timestamp).getTime();

    // Memory usage
    const memUsage = process.memoryUsage();

    // Uptime
    const uptime = process.uptime();

    res.status(200).json({
      status: "OK",
      timestamp: new Date().toISOString(),
      version: process.env.npm_package_version || "1.0.0",
      environment: process.env.NODE_ENV || "development",

      database: {
        status: "connected",
        latency: `${dbLatency}ms`,
        timestamp: dbResult.rows[0].timestamp,
      },

      server: {
        uptime: `${Math.floor(uptime / 3600)}h ${Math.floor(
          (uptime % 3600) / 60
        )}m`,
        memory: {
          used: `${Math.round(memUsage.heapUsed / 1024 / 1024)}MB`,
          total: `${Math.round(memUsage.heapTotal / 1024 / 1024)}MB`,
          external: `${Math.round(memUsage.external / 1024 / 1024)}MB`,
        },
        pid: process.pid,
      },

      services: {
        cache: cacheService.getStats(),
        battleRoyale: battleRoyaleManager.getStats(),
        sockets: socketService.getStats(),
      },
    });
  } catch (error) {
    res.status(503).json({
      status: "ERROR",
      timestamp: new Date().toISOString(),
      database: "disconnected",
      error: error.message,
    });
  }
});
```

#### **Structured Logging**

**Morgan Custom Format:**

```javascript
// Enhanced request logging
const morganFormat =
  ':remote-addr - :remote-user [:date[clf]] ":method :url HTTP/:http-version" :status :res[content-length] ":referrer" ":user-agent" :response-time ms';

app.use(
  morgan(morganFormat, {
    stream: {
      write: (message) => {
        // Parse and structure log data
        const logData = {
          timestamp: new Date().toISOString(),
          level: "info",
          type: "http_request",
          message: message.trim(),
          environment: process.env.NODE_ENV,
        };

        // Send to logging service (e.g., Winston, CloudWatch)
        console.log(JSON.stringify(logData));
      },
    },
  })
);
```

**Application Logging:**

```javascript
// Custom logger utility
class Logger {
  static info(message, metadata = {}) {
    console.log(
      JSON.stringify({
        timestamp: new Date().toISOString(),
        level: "info",
        message,
        ...metadata,
      })
    );
  }

  static error(message, error = null, metadata = {}) {
    console.error(
      JSON.stringify({
        timestamp: new Date().toISOString(),
        level: "error",
        message,
        error: error
          ? {
              name: error.name,
              message: error.message,
              stack: error.stack,
            }
          : null,
        ...metadata,
      })
    );
  }

  static warn(message, metadata = {}) {
    console.warn(
      JSON.stringify({
        timestamp: new Date().toISOString(),
        level: "warn",
        message,
        ...metadata,
      })
    );
  }
}

// Usage in services
Logger.info("New round created", {
  roundId: round.id,
  locationId: location.id,
  userId: userId,
});
```

#### **Performance Metrics**

**Response Time Tracking:**

```javascript
// Middleware for performance tracking
const performanceTracker = (req, res, next) => {
  const startTime = Date.now();

  res.on("finish", () => {
    const responseTime = Date.now() - startTime;

    // Log slow requests
    if (responseTime > 1000) {
      Logger.warn("Slow request detected", {
        method: req.method,
        url: req.url,
        responseTime: `${responseTime}ms`,
        statusCode: res.statusCode,
      });
    }

    // Metrics collection
    metrics.recordResponseTime(req.route?.path || req.url, responseTime);
    metrics.incrementRequestCount(req.method, res.statusCode);
  });

  next();
};

app.use(performanceTracker);
```

**Database Query Monitoring:**

```javascript
// Enhanced database connection with query logging
class Database {
  async query(text, params = []) {
    const startTime = Date.now();

    try {
      const result = await this.pool.query(text, params);
      const duration = Date.now() - startTime;

      // Log slow queries
      if (duration > 500) {
        Logger.warn("Slow database query", {
          query: text.substring(0, 100) + (text.length > 100 ? "..." : ""),
          duration: `${duration}ms`,
          rowCount: result.rowCount,
        });
      }

      return result;
    } catch (error) {
      const duration = Date.now() - startTime;

      Logger.error("Database query failed", error, {
        query: text.substring(0, 100),
        duration: `${duration}ms`,
        params: params.length,
      });

      throw error;
    }
  }
}
```

#### **Error Tracking**

**Global Error Handler:**

```javascript
// Unhandled error catcher
process.on("uncaughtException", (error) => {
  Logger.error("Uncaught Exception", error, {
    type: "uncaughtException",
    critical: true,
  });

  // Graceful shutdown
  process.exit(1);
});

process.on("unhandledRejection", (reason, promise) => {
  Logger.error("Unhandled Rejection", reason, {
    type: "unhandledRejection",
    promise: promise.toString(),
  });
});

// Express error handler
app.use((err, req, res, next) => {
  Logger.error("Express error handler", err, {
    method: req.method,
    url: req.url,
    userAgent: req.get("User-Agent"),
    ip: req.ip,
  });

  if (res.headersSent) {
    return next(err);
  }

  const statusCode = err.statusCode || 500;
  const isDevelopment = process.env.NODE_ENV === "development";

  res.status(statusCode).json({
    error: isDevelopment ? err.message : "Internal server error",
    timestamp: new Date().toISOString(),
    requestId: req.id || "unknown",
  });
});
```

#### **Real-time Service Monitoring**

**Battle Royale Service Health:**

```javascript
// Service-specific health checks
router.get("/health", async (req, res) => {
  try {
    const stats = battleRoyaleManager.getStats();
    const socketStats = socketService.getStats();

    // Check for service anomalies
    const issues = [];

    if (stats.activeSessions > 100) {
      issues.push("High session count detected");
    }

    if (socketStats.connectedUsers > 1000) {
      issues.push("High socket connection count");
    }

    const healthStatus = issues.length === 0 ? "healthy" : "warning";

    res.json({
      success: true,
      status: healthStatus,
      message: "Battle Royale service status",
      data: {
        battleRoyale: stats,
        sockets: socketStats,
        issues: issues,
        timestamp: new Date(),
      },
    });
  } catch (error) {
    res.status(503).json({
      success: false,
      status: "unhealthy",
      error: "Battle Royale service is unhealthy",
      message: error.message,
    });
  }
});
```

**Cache Service Monitoring:**

```javascript
// Cache performance metrics
class CacheService {
  getStats() {
    const keys = this.cache.keys();
    const stats = this.cache.getStats();

    return {
      totalKeys: keys.length,
      hits: stats.hits,
      misses: stats.misses,
      hitRate: stats.hits / (stats.hits + stats.misses) || 0,
      memory: {
        used: `${Math.round(JSON.stringify(this.cache.data).length / 1024)}KB`,
        keys: keys.length,
      },
      performance: {
        avgResponseTime: this.calculateAvgResponseTime(),
        slowKeys: this.getSlowKeys(),
      },
    };
  }

  // Track cache performance
  async getOrSet(key, fetcher, ttl = 600) {
    const startTime = Date.now();
    const value = await super.getOrSet(key, fetcher, ttl);
    const responseTime = Date.now() - startTime;

    this.recordMetric(key, responseTime, value !== undefined);
    return value;
  }
}
```

---

## 🎯 Zusammenfassung und Best Practices

### Architektur-Prinzipien

1. **Backend-For-Frontend Pattern**: Server verwaltet Spiellogik, Client handles UI
2. **Microservice-orientierte Services**: Modulare Geschäftslogik
3. **Real-time First**: Socket.IO für sofortige Updates
4. **Database-First**: PostGIS für geografische Abfragen
5. **Security by Design**: Umfassende Sicherheitsmaßnahmen

### Performance Optimierungen

1. **Caching-Strategien**: Memory Cache für häufige Abfragen
2. **Database Indexing**: Optimierte PostGIS-Indizes
3. **Connection Pooling**: Effiziente Datenbankverbindungen
4. **Response Compression**: Gzip-Komprimierung
5. **Query Optimization**: Parametrisierte Queries

### Skalierbarkeit

1. **Horizontal Scaling**: Stateless Server Design
2. **Load Balancing**: Session-agnostic Architecture
3. **Database Scaling**: Read Replicas Support
4. **Caching Layers**: Redis Integration Ready
5. **Monitoring**: Comprehensive Health Checks

### Wartbarkeit

1. **Structured Logging**: JSON-basierte Logs
2. **Error Handling**: Comprehensive Error Tracking
3. **Testing Strategy**: Unit, Integration, Performance Tests
4. **Documentation**: API Documentation & Code Comments
5. **Migration System**: Database Schema Versioning

---

**Ende der Dokumentation**
