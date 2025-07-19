## 📋 Development Phases

### Phase 1: Foundation Setup (Week 1-2)

#### Backend Development
1. **Project Initialization**
   - Set up Node.js project structure
   - Configure Express.js server
   - Implement basic routing
   - Set up environment configuration

2. **Database Setup**
   - Create Neon PostgreSQL database
   - Design and implement schema
   - Set up PostGIS extension
   - Create initial migrations

3. **Authentication System**
   - Implement JWT-based authentication
   - Create user registration endpoint
   - Build login/logout functionality
   - Add password hashing with bcrypt

4. **Location Management**
   - Design location data structure
   - Create location seeding system
   - Implement random location retrieval
   - Add difficulty categorization

#### Android Development
1. **Project Setup**
   - Create Android Studio project
   - Configure Gradle dependencies
   - Set up MVVM architecture
   - Implement dependency injection

2. **Core UI Components**
   - Main menu activity
   - Authentication screens
   - Basic game activity layout
   - Material Design theming

3. **Local Database**
   - Set up Room database
   - Create entity models
   - Implement DAOs
   - Add offline caching logic

4. **Networking Layer**
   - Configure Retrofit API service
   - Implement authentication interceptor
   - Add error handling
   - Create repository pattern

**Deliverables Phase 1:**
- Working authentication system
- Basic single-player game mode
- Offline location caching
- Simple scoring system

### Phase 2: Core Game Features (Week 3-4)

#### Game Logic Implementation
1. **Single Player Mode**
   - Location presentation system
   - Interactive map for guessing
   - Distance calculation (Haversine formula)
   - Score calculation algorithm
   - Round progression logic

2. **Scoring System**
   ```kotlin
   object ScoreCalculator {
       fun calculate(distanceKm: Double): Int {
           return when {
               distanceKm <= 1 -> 5000
               distanceKm <= 10 -> (5000 - (distanceKm - 1) * 400).toInt()
               distanceKm <= 100 -> (1400 - (distanceKm - 10) * 10).toInt()
               else -> maxOf(0, (500 - (distanceKm - 100) * 2).toInt())
           }
       }
   }
   ```

3. **Game State Management**
   - Round tracking
   - Timer functionality
   - Result calculation
   - Local score persistence

#### UI/UX Development
1. **Street View Component**
   - 360-degree image viewer
   - Touch navigation controls
   - Zoom functionality
   - Loading states

2. **Map Interface**
   - Interactive world map
   - Guess placement marker
   - Zoom/pan controls
   - Location reveal animation

3. **Score Display**
   - Round results screen
   - Distance visualization
   - Score breakdown
   - Performance statistics

**Deliverables Phase 2:**
- Complete single-player experience
- Intuitive UI for all game phases
- Accurate scoring system
- Local statistics tracking

### Phase 3: Multiplayer & Real-time Features (Week 5-6)

#### Socket.IO Integration
1. **Server-side Implementation**
   ```javascript
   // Battle Royale Session Management
   class BattleRoyaleManager {
       constructor() {
           this.sessions = new Map();
       }
       
       createSession(creatorId) {
           const code = this.generateSessionCode();
           const session = {
               code,
               creatorId,
               players: [],
               status: 'waiting',
               currentRound: 0,
               locations: []
           };
           this.sessions.set(code, session);
           return session;
       }
       
       processGuess(sessionCode, userId, guess) {
           const session = this.sessions.get(sessionCode);
           // Eliminate lowest scorers
           // Advance to next round
           // Check win conditions
       }
   }
   ```

2. **Real-time Events**
   - Player join/leave events
   - Round start synchronization
   - Guess submission handling
   - Elimination notifications
   - Final results broadcasting

#### Android Real-time Client
1. **Socket Connection**
   ```kotlin
   class SocketClient {
       private lateinit var socket: Socket
       
       fun connect() {
           socket = IO.socket("https://your-app.vercel.app")
           socket.connect()
           setupEventListeners()
       }
       
       private fun setupEventListeners() {
           socket.on("round-started") { args ->
               val location = args[0] as JSONObject
               // Update UI with new location
           }
           
           socket.on("player-eliminated") { args ->
               val playerId = args[0] as String
               // Handle player elimination
           }
       }
   }
   ```

2. **Battle Royale UI**
   - Lobby system with session codes
   - Real-time player list
   - Elimination animations
   - Live leaderboard during game

**Deliverables Phase 3:**
- Functional battle royale mode
- Real-time multiplayer synchronization
- Session management system
- Lobby and matchmaking UI

### Phase 4: Social Features (Week 7-8)

#### Challenge System
1. **Challenge Creation**
   ```kotlin
   data class Challenge(
       val id: String,
       val creatorId: String,
       val title: String,
       val locationIds: List<Int>,
       val shareCode: String,
       val settings: ChallengeSettings
   )
   
   class ChallengeManager {
       suspend fun createChallenge(locations: List<Location>): Challenge {
           return apiService.createChallenge(locations)
       }
       
       suspend fun acceptChallenge(shareCode: String): ChallengeResult {
           return apiService.joinChallenge(shareCode)
       }
   }
   ```

2. **Friends System**
   - Friend request functionality
   - Friend list management
   - Challenge sharing between friends
   - Social statistics comparison

#### Global Features
1. **Leaderboards**
   - Global ranking system
   - Timeframe filtering (daily, weekly, all-time)
   - Game mode separation
   - Friend rankings

2. **Daily Challenges**
   - Automated daily location selection
   - Global participation tracking
   - Special scoring for daily events
   - Achievement system

**Deliverables Phase 4:**
- Complete challenge system
- Friends and social features
- Global leaderboards
- Daily challenge automation

### Phase 5: Polish & Optimization (Week 9-10)

#### Performance Optimization
1. **Image Management**
   - WebP format conversion
   - Lazy loading implementation
   - Cache size management
   - Offline image storage

2. **Database Optimization**
   ```sql
   -- Performance Indexes
   CREATE INDEX idx_locations_coordinates ON locations USING GIST(coordinates);
   CREATE INDEX idx_game_results_score ON game_results(total_score DESC);
   CREATE INDEX idx_users_last_active ON users(last_active DESC);
   CREATE INDEX idx_challenges_share_code ON challenges(share_code);
   ```

3. **Network Optimization**
   - Request caching
   - Connection pooling
   - Rate limiting
   - Error retry logic

#### User Experience
1. **Tutorial System**
   - Interactive onboarding
   - Feature explanations
   - Tips and tricks
   - Help documentation

2. **Accessibility**
   - Screen reader support
   - High contrast mode
   - Large text options
   - Touch target sizing

**Deliverables Phase 5:**
- Optimized performance
- Complete tutorial system
- Accessibility compliance
- Production-ready stability

## 🗄️ Database Design

### Core Tables
```sql
-- Users and Authentication
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

-- Locations with Geographic Data
CREATE EXTENSION IF NOT EXISTS postgis;

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
    created_at TIMESTAMP DEFAULT NOW()
);

-- Game Sessions
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

-- Game Results and Scoring
CREATE TABLE game_results (
    id SERIAL PRIMARY KEY,
    session_id INTEGER REFERENCES game_sessions(id),
    user_id INTEGER REFERENCES users(id),
    total_score INTEGER NOT NULL,
    total_distance DECIMAL(10,2) NOT NULL,
    accuracy DECIMAL(5,2),
    rounds_completed INTEGER NOT NULL,
    time_taken INTEGER, -- seconds
    rounds_data JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Battle Royale Participants
CREATE TABLE battle_participants (
    id SERIAL PRIMARY KEY,
    session_id INTEGER REFERENCES game_sessions(id),
    user_id INTEGER REFERENCES users(id),
    current_round INTEGER DEFAULT 1,
    is_eliminated BOOLEAN DEFAULT FALSE,
    elimination_round INTEGER,
    final_rank INTEGER,
    total_score INTEGER DEFAULT 0,
    joined_at TIMESTAMP DEFAULT NOW()
);

-- Social Features
CREATE TABLE friendships (
    id SERIAL PRIMARY KEY,
    requester_id INTEGER REFERENCES users(id),
    addressee_id INTEGER REFERENCES users(id),
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'blocked')),
    created_at TIMESTAMP DEFAULT NOW(),
    accepted_at TIMESTAMP,
    UNIQUE(requester_id, addressee_id),
    CHECK (requester_id != addressee_id)
);

-- Challenges
CREATE TABLE challenges (
    id SERIAL PRIMARY KEY,
    creator_id INTEGER REFERENCES users(id),
    title VARCHAR(100) NOT NULL,
    description TEXT,
    share_code VARCHAR(6) UNIQUE NOT NULL,
    location_ids INTEGER[] NOT NULL,
    settings JSONB DEFAULT '{}',
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'expired', 'disabled')),
    attempts INTEGER DEFAULT 0,
    best_score INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP DEFAULT (NOW() + INTERVAL '30 days')
);

-- Daily Challenges
CREATE TABLE daily_challenges (
    id SERIAL PRIMARY KEY,
    date DATE UNIQUE NOT NULL,
    location_ids INTEGER[] NOT NULL,
    theme VARCHAR(50),
    difficulty INTEGER DEFAULT 3,
    participants INTEGER DEFAULT 0,
    average_score DECIMAL(8,2),
    created_at TIMESTAMP DEFAULT NOW()
);

-- User Statistics
CREATE TABLE user_statistics (
    user_id INTEGER PRIMARY KEY REFERENCES users(id),
    total_distance DECIMAL(12,2) DEFAULT 0,
    perfect_scores INTEGER DEFAULT 0,
    countries_visited INTEGER DEFAULT 0,
    longest_streak INTEGER DEFAULT 0,
    current_streak INTEGER DEFAULT 0,
    favorite_region VARCHAR(50),
    avg_score DECIMAL(8,2) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT NOW()
);
```

### Indexes for Performance
```sql
-- Geographic queries
CREATE INDEX idx_locations_coordinates ON locations USING GIST(coordinates);
CREATE INDEX idx_locations_country ON locations(country);
CREATE INDEX idx_locations_difficulty ON locations(difficulty);

-- Leaderboard queries
CREATE INDEX idx_game_results_score ON game_results(total_score DESC);
CREATE INDEX idx_game_results_user_created ON game_results(user_id, created_at DESC);

-- Social features
CREATE INDEX idx_friendships_requester ON friendships(requester_id);
CREATE INDEX idx_friendships_addressee ON friendships(addressee_id);

-- Challenges
CREATE INDEX idx_challenges_share_code ON challenges(share_code);
CREATE INDEX idx_challenges_creator ON challenges(creator_id);

-- Session management
CREATE INDEX idx_game_sessions_code ON game_sessions(session_code);
CREATE INDEX idx_game_sessions_type_status ON game_sessions(session_type, status);
```

## 🚀 API Design

### Authentication Endpoints
```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
GET  /api/auth/me
POST /api/auth/logout
```

### Game Management
```
GET  /api/locations/random/:count
POST /api/games/single
POST /api/games/battle/create
POST /api/games/battle/join/:code
GET  /api/games/:id
PUT  /api/games/:id/result
```

### Social Features
```
GET  /api/leaderboards
GET  /api/leaderboards/friends
GET  /api/friends
POST /api/friends/request
PUT  /api/friends/accept/:id
DELETE /api/friends/:id
```

### Challenge System
```
POST /api/challenges/create
GET  /api/challenges/:code
POST /api/challenges/:id/attempt
GET  /api/challenges/my
```

### Statistics
```
GET  /api/stats/user/:id
GET  /api/stats/global
GET  /api/daily-challenge
GET  /api/daily-challenge/leaderboard
```

## 📱 Android Application Structure

```
app/src/main/java/com/geoguessr/
├── ui/
│   ├── activities/
│   │   ├── MainActivity.kt
│   │   ├── AuthActivity.kt
│   │   ├── GameActivity.kt
│   │   ├── BattleRoyaleActivity.kt
│   │   └── StatsActivity.kt
│   ├── fragments/
│   │   ├── StreetViewFragment.kt
│   │   ├── MapGuessFragment.kt
│   │   ├── ScoreFragment.kt
│   │   └── LeaderboardFragment.kt
│   └── adapters/
│       ├── LeaderboardAdapter.kt
│       └── FriendsAdapter.kt
├── data/
│   ├── network/
│   │   ├── ApiService.kt
│   │   ├── SocketClient.kt
│   │   └── AuthInterceptor.kt
│   ├── database/
│   │   ├── entities/
│   │   ├── dao/
│   │   └── AppDatabase.kt
│   ├── repositories/
│   │   ├── GameRepository.kt
│   │   ├── UserRepository.kt
│   │   └── LocationRepository.kt
│   └── models/
│       ├── Game.kt
│       ├── User.kt
│       └── Location.kt
├── viewmodels/
│   ├── GameViewModel.kt
│   ├── AuthViewModel.kt
│   └── StatsViewModel.kt
├── utils/
│   ├── DistanceCalculator.kt
│   ├── ScoreCalculator.kt
│   └── Constants.kt
└── di/
    ├── NetworkModule.kt
    ├── DatabaseModule.kt
    └── RepositoryModule.kt
```

## 🔧 Deployment Configuration

### Backend (Vercel)
```json
{
  "version": 2,
  "builds": [
    {
      "src": "server.js",
      "use": "@vercel/node"
    }
  ],
  "routes": [
    {
      "src": "/socket.io/(.*)",
      "dest": "server.js"
    },
    {
      "src": "/api/(.*)",
      "dest": "server.js"
    }
  ],
  "env": {
    "DATABASE_URL": "@database_url",
    "JWT_SECRET": "@jwt_secret"
  }
}
```

### Environment Variables
```bash
# Backend (.env)
DATABASE_URL=postgresql://user:pass@host/db
JWT_SECRET=your-secret-key
CORS_ORIGIN=*
NODE_ENV=production

# Android (local.properties)
api.base.url=https://your-app.vercel.app
socket.url=wss://your-app.vercel.app
```

## 📊 Testing Strategy

### Backend Testing
```javascript
// Unit Tests
describe('Score Calculator', () => {
  test('perfect guess returns maximum score', () => {
    expect(calculateScore(0)).toBe(5000);
  });
  
  test('distance affects score correctly', () => {
    expect(calculateScore(100)).toBeLessThan(calculateScore(50));
  });
});

// Integration Tests
describe('Battle Royale API', () => {
  test('creates battle session', async () => {
    const response = await request(app)
      .post('/api/games/battle/create')
      .send({ creatorId: 1 });
    
    expect(response.status).toBe(201);
    expect(response.body.sessionCode).toHaveLength(6);
  });
});
```

### Android Testing
```kotlin
@Test
fun `game repository returns cached locations when offline`() {
    // Given
    val mockLocations = listOf(mockLocation1, mockLocation2)
    whenever(localDao.getRandomCached(5)).thenReturn(mockLocations)
    whenever(apiService.getRandomLocations(5)).thenThrow(IOException())
    
    // When
    val result = runBlocking { repository.getRandomLocations(5) }
    
    // Then
    assertEquals(mockLocations.size, result.size)
}
```

## 💡 Optimization Guidelines

### Performance Best Practices
1. **Image Optimization**
   - Use WebP format for all images
   - Implement progressive loading
   - Cache images locally for offline use
   - Compress images to <500KB each

2. **Database Optimization**
   - Use connection pooling
   - Implement query result caching
   - Add appropriate indexes
   - Use prepared statements

3. **Network Optimization**
   - Implement request deduplication
   - Use compression for API responses
   - Add retry logic with exponential backoff
   - Cache API responses when appropriate

### Cost Management
```javascript
// Rate limiting to prevent abuse
const rateLimit = require('express-rate-limit');

const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: 'Too many requests from this IP'
});

const gameLimiter = rateLimit({
  windowMs: 5 * 60 * 1000, // 5 minutes
  max: 5, // limit game creation
  message: 'Game creation rate limit exceeded'
});
```

## 🎯 Success Metrics

### Technical Metrics
- App startup time: <3 seconds
- API response time: <500ms (95th percentile)
- Offline capability: 100 cached locations minimum
- Battery efficiency: <5% drain per 30-minute session

### User Experience Metrics
- Tutorial completion rate: >80%
- Daily active users retention: >60% after 1 week
- Average session duration: 15+ minutes
- Crash rate: <1% of sessions

### Business Metrics
- Monthly infrastructure cost: $0
- User acquisition cost: $0 (organic growth)
- User engagement: 3+ games per session average
- Feature adoption: Challenge sharing >40% of users

---

**Development Timeline: 10 weeks | Target: 5 concurrent users | Infrastructure: Free tier only**
