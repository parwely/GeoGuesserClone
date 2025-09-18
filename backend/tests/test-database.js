// Test database connection with timeout
const database = require("./src/database/connection");

async function testDatabase() {
  console.log("🔍 Testing database connection...");

  // Set a timeout for the database connection
  const timeoutPromise = new Promise((_, reject) => {
    setTimeout(() => reject(new Error("Database connection timeout")), 10000);
  });

  try {
    const connectPromise = database.connect();
    await Promise.race([connectPromise, timeoutPromise]);

    console.log("✅ Database connected successfully");

    // Test a simple query
    const result = await database.query("SELECT 1 as test");
    console.log("✅ Database query successful:", result.rows[0]);

    // Test if locations table exists
    const tableCheck = await database.query(`
      SELECT table_name 
      FROM information_schema.tables 
      WHERE table_schema = 'public' AND table_name = 'locations'
    `);

    if (tableCheck.rows.length > 0) {
      console.log("✅ Locations table exists");

      // Count locations
      const countResult = await database.query(
        "SELECT COUNT(*) as count FROM locations"
      );
      console.log(`✅ Found ${countResult.rows[0].count} locations`);
    } else {
      console.log("❌ Locations table not found");
    }

    // Test rounds table
    const roundsCheck = await database.query(`
      SELECT table_name 
      FROM information_schema.tables 
      WHERE table_schema = 'public' AND table_name = 'rounds'
    `);

    if (roundsCheck.rows.length > 0) {
      console.log("✅ Rounds table exists");
    } else {
      console.log("❌ Rounds table not found");
    }
  } catch (error) {
    console.error("❌ Database test failed:", error.message);
  } finally {
    console.log("🔄 Closing database connection...");
    process.exit(0);
  }
}

testDatabase();
