const express = require("express");
const cors = require("cors");
require("dotenv").config();

const app = express();
const PORT = process.env.PORT || 3000;

// Basic middleware
app.use(cors({ origin: "*" }));
app.use(express.json());

// Basic health check
app.get("/health", (req, res) => {
  res.json({ status: "OK", message: "Server is running" });
});

// Simple battle royale endpoint test
app.get("/api/battle-royale", (req, res) => {
  console.log("🎮 Simple Battle Royale endpoint called");
  res.json({
    success: true,
    message: "Battle Royale service is active (simple version)",
    timestamp: new Date(),
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

app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 Simple test server running on port ${PORT}`);
  console.log(`📡 Test: http://127.0.0.1:${PORT}/api/battle-royale`);
  
  // Test self-request after a brief delay
  setTimeout(() => {
    const http = require('http');
    console.log('🔍 Testing self-request...');
    http.get(`http://127.0.0.1:${PORT}/api/battle-royale`, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        console.log('✅ Self-test passed! Status:', res.statusCode);
        console.log('📦 Response:', data.substring(0, 100) + '...');
      });
    }).on('error', (err) => {
      console.error('❌ Self-test failed:', err.message);
    });
  }, 1000);
});