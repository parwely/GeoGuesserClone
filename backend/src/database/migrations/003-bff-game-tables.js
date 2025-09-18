const database = require("../connection");

/**
 * Migration 003: Add BFF pattern support with rounds and guesses tables
 * Add Street View metadata fields to locations table
 */

async function up() {
  try {
    console.log("🔄 Running migration 003: BFF game tables...");

    // Add Street View metadata fields to locations table
    await database.query(`
      ALTER TABLE locations 
      ADD COLUMN IF NOT EXISTS has_pano BOOLEAN DEFAULT NULL,
      ADD COLUMN IF NOT EXISTS pano_id VARCHAR(255) DEFAULT NULL
    `);

    // Add indexes for faster lookups on Street View fields
    await database.query(`
      CREATE INDEX IF NOT EXISTS idx_locations_has_pano 
      ON locations(has_pano) WHERE has_pano IS NOT FALSE
    `);

    await database.query(`
      CREATE INDEX IF NOT EXISTS idx_locations_pano_id 
      ON locations(pano_id) WHERE pano_id IS NOT NULL
    `);

    // Create rounds table for BFF pattern
    await database.query(`
      CREATE TABLE IF NOT EXISTS rounds (
        id SERIAL PRIMARY KEY,
        location_id INTEGER NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
        user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
        status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'completed', 'expired')),
        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
        completed_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
        expires_at TIMESTAMP WITH TIME ZONE DEFAULT (NOW() + INTERVAL '1 hour')
      )
    `);

    // Create guesses table for BFF pattern
    await database.query(`
      CREATE TABLE IF NOT EXISTS guesses (
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
      )
    `);

    // Create indexes for performance
    await database.query(`
      CREATE INDEX IF NOT EXISTS idx_rounds_location_id ON rounds(location_id)
    `);

    await database.query(`
      CREATE INDEX IF NOT EXISTS idx_rounds_user_id ON rounds(user_id)
    `);

    await database.query(`
      CREATE INDEX IF NOT EXISTS idx_rounds_status ON rounds(status)
    `);

    await database.query(`
      CREATE INDEX IF NOT EXISTS idx_rounds_created_at ON rounds(created_at)
    `);

    await database.query(`
      CREATE INDEX IF NOT EXISTS idx_guesses_round_id ON guesses(round_id)
    `);

    await database.query(`
      CREATE INDEX IF NOT EXISTS idx_guesses_user_id ON guesses(user_id)
    `);

    await database.query(`
      CREATE INDEX IF NOT EXISTS idx_guesses_created_at ON guesses(created_at)
    `);

    // Add constraint: each round can have at most one guess
    await database.query(`
      CREATE UNIQUE INDEX IF NOT EXISTS idx_guesses_unique_round ON guesses(round_id)
    `);

    // Clean up expired rounds function
    await database.query(`
      CREATE OR REPLACE FUNCTION cleanup_expired_rounds()
      RETURNS INTEGER AS $$
      DECLARE
        deleted_count INTEGER;
      BEGIN
        DELETE FROM rounds 
        WHERE status = 'active' 
          AND expires_at < NOW() 
          AND created_at < NOW() - INTERVAL '24 hours';
          
        GET DIAGNOSTICS deleted_count = ROW_COUNT;
        
        -- Update status instead of deleting if you want to keep history
        UPDATE rounds 
        SET status = 'expired' 
        WHERE status = 'active' 
          AND expires_at < NOW();
          
        RETURN deleted_count;
      END;
      $$ LANGUAGE plpgsql
    `);

    console.log("✅ Migration 003 completed: BFF game tables created");
  } catch (error) {
    console.error("❌ Migration 003 failed:", error.message);
    throw error;
  }
}

async function down() {
  try {
    console.log("🔄 Reversing migration 003: BFF game tables...");

    // Drop function
    await database.query("DROP FUNCTION IF EXISTS cleanup_expired_rounds()");

    // Drop tables (will cascade and remove indexes)
    await database.query("DROP TABLE IF EXISTS guesses CASCADE");
    await database.query("DROP TABLE IF EXISTS rounds CASCADE");

    // Remove added columns from locations
    await database.query(`
      ALTER TABLE locations 
      DROP COLUMN IF EXISTS has_pano,
      DROP COLUMN IF EXISTS pano_id
    `);

    console.log("✅ Migration 003 reversed: BFF game tables removed");
  } catch (error) {
    console.error("❌ Migration 003 reversal failed:", error.message);
    throw error;
  }
}

module.exports = { up, down };
