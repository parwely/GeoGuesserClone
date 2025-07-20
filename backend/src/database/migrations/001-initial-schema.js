const database = require('../connection');

async function up() {
  const client = await database.getClient();
  
  try {
    await client.query('BEGIN');
    
    console.log('🚀 Creating initial database schema...');
    
    // Enable PostGIS extension
    await client.query('CREATE EXTENSION IF NOT EXISTS postgis;');
    console.log('✅ PostGIS extension enabled');
    
    // Users table
    await client.query(`
      CREATE TABLE IF NOT EXISTS users (
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
    `);
    console.log('✅ Users table created');
    
    // Locations table with PostGIS
    await client.query(`
      CREATE TABLE IF NOT EXISTS locations (
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
    `);
    console.log('✅ Locations table created');
    
    // Game sessions table
    await client.query(`
      CREATE TABLE IF NOT EXISTS game_sessions (
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
    `);
    console.log('✅ Game sessions table created');
    
    // Game results table
    await client.query(`
      CREATE TABLE IF NOT EXISTS game_results (
        id SERIAL PRIMARY KEY,
        session_id INTEGER REFERENCES game_sessions(id),
        user_id INTEGER REFERENCES users(id),
        total_score INTEGER NOT NULL,
        total_distance DECIMAL(10,2) NOT NULL,
        accuracy DECIMAL(5,2),
        rounds_completed INTEGER NOT NULL,
        time_taken INTEGER,
        rounds_data JSONB NOT NULL,
        created_at TIMESTAMP DEFAULT NOW()
      );
    `);
    console.log('✅ Game results table created');
    
    await client.query('COMMIT');
    console.log('🎉 Initial schema migration completed successfully!');
    
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('❌ Migration failed:', error.message);
    throw error;
  } finally {
    client.release();
  }
}

async function down() {
  const client = await database.getClient();
  
  try {
    await client.query('BEGIN');
    
    console.log('🔄 Rolling back initial schema...');
    
    await client.query('DROP TABLE IF EXISTS game_results CASCADE;');
    await client.query('DROP TABLE IF EXISTS game_sessions CASCADE;');
    await client.query('DROP TABLE IF EXISTS locations CASCADE;');
    await client.query('DROP TABLE IF EXISTS users CASCADE;');
    await client.query('DROP EXTENSION IF EXISTS postgis CASCADE;');
    
    await client.query('COMMIT');
    console.log('✅ Schema rollback completed');
    
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}

module.exports = { up, down };