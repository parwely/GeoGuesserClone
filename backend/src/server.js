const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const morgan = require("morgan");
const rateLimit = require("express-rate-limit");
const compression = require("compression");
require("dotenv").config();

const app = express();
const PORT = process.env.PORT || 3000;

// Compression middleware - should be early in the stack
app.use(
  compression({
    filter: (req, res) => {
      if (req.headers["x-no-compression"]) {
        return false;
      }
      return compression.filter(req, res);
    },
    threshold: 1024, // Only compress responses > 1KB
    level: 6, // Compression level (1-9, 6 is good balance)
  })
);

// Security middleware
app.use(helmet());
app.use(
  cors({
    origin: process.env.CORS_ORIGIN || "*",
    credentials: true,
  })
);

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
});
app.use("/api/", limiter);

// Logging middleware
app.use(morgan("combined"));

// Body parsing middleware
app.use(express.json({ limit: "10mb" }));
app.use(express.urlencoded({ extended: true }));

// Basic routes
app.get("/", (req, res) => {
  res.json({
    message: "GeoGuessr Clone API",
    version: "1.0.0",
    status: "active",
    endpoints: {
      auth: "/api/auth", // Fixed: was '/routes/auth'
      locations: "/api/locations", // Fixed: was '/routes/locations'
      games: "/api/games", // Fixed: was '/routes/games'
    },
  });
});

// Health check endpoint
// Enhanced health check endpoint
app.get("/health", async (req, res) => {
  const startTime = Date.now();
  const database = require("./database/connection");
  const cacheService = require("./services/cacheService");

  try {
    // Test database connection
    await database.query("SELECT 1");

    const health = {
      status: "OK",
      timestamp: new Date().toISOString(),
      uptime: process.uptime(),
      responseTime: Date.now() - startTime,
      database: "connected",
      cache: cacheService.getStats(),
      memory: {
        used: Math.round(process.memoryUsage().heapUsed / 1024 / 1024),
        total: Math.round(process.memoryUsage().heapTotal / 1024 / 1024),
      },
      database_pool: database.getPoolStats(),
    };

    res.json(health);
  } catch (error) {
    res.status(503).json({
      status: "ERROR",
      timestamp: new Date().toISOString(),
      database: "disconnected",
      error: error.message,
    });
  }
});

// Cache management endpoint (development only)
if (process.env.NODE_ENV === "development") {
  app.get("/cache/stats", (req, res) => {
    const cacheService = require("./services/cacheService");
    res.json(cacheService.getStats());
  });

  app.delete("/cache/clear", (req, res) => {
    const cacheService = require("./services/cacheService");
    cacheService.clear();
    res.json({ message: "Cache cleared successfully" });
  });
}

// API routes - make sure these exist
try {
  app.use("/api/auth", require("./routes/auth"));
  app.use("/api/locations", require("./routes/locations"));
  app.use("/api/games", require("./routes/games"));
} catch (error) {
  console.error("Error loading routes:", error.message);
  process.exit(1);
}

// Error handling middleware
app.use((err, req, res, next) => {
  console.error("Error:", err.stack);
  res.status(500).json({
    error: "Something went wrong!",
    message:
      process.env.NODE_ENV === "development"
        ? err.message
        : "Internal server error",
  });
});

// 404 handler
app.use("*", (req, res) => {
  res.status(404).json({
    error: "Route not found",
    path: req.originalUrl,
    method: req.method,
  });
});

if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`🚀 Server running on port ${PORT}`);
    console.log(`📡 API URL: http://localhost:${PORT}`);
    console.log(`🏥 Health check: http://localhost:${PORT}/health`);
  });
}

module.exports = app;
