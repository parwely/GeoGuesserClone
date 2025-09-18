// Test route loading individually
console.log("🔍 Testing route imports...");

try {
  console.log("1. Testing auth routes...");
  require("./src/routes/auth");
  console.log("✅ Auth routes OK");

  console.log("2. Testing location routes...");
  require("./src/routes/locations");
  console.log("✅ Location routes OK");

  console.log("3. Testing game routes...");
  require("./src/routes/games");
  console.log("✅ Game routes OK");

  console.log("4. Testing battle royale routes...");
  require("./src/routes/battleRoyale");
  console.log("✅ Battle royale routes OK");

  console.log("🎉 All routes loaded successfully!");
} catch (error) {
  console.error("❌ Route loading failed:", error.message);
  console.error("Stack:", error.stack);
}
