async function up(database) {
  const client = await database.getClient();
  
  try {
    await client.query('BEGIN');
    
    console.log('🚀 Creating performance indexes...');
    
    // Geographic queries (PostGIS spatial index)
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_locations_coordinates 
      ON locations USING GIST(coordinates);
    `);
    console.log('✅ Geographic spatial index created');
    
    // Location search indexes
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_locations_country ON locations(country);
      CREATE INDEX IF NOT EXISTS idx_locations_difficulty ON locations(difficulty);
      CREATE INDEX IF NOT EXISTS idx_locations_category ON locations(category);
    `);
    console.log('✅ Location search indexes created');
    
    // User performance indexes
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
      CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
      CREATE INDEX IF NOT EXISTS idx_users_last_active ON users(last_active DESC);
    `);
    console.log('✅ User indexes created');
    
    // Game session indexes
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_game_sessions_code ON game_sessions(session_code);
      CREATE INDEX IF NOT EXISTS idx_game_sessions_type_status ON game_sessions(session_type, status);
      CREATE INDEX IF NOT EXISTS idx_game_sessions_created_by ON game_sessions(created_by);
    `);
    console.log('✅ Game session indexes created');
    
    // Leaderboard indexes
    await client.query(`
      CREATE INDEX IF NOT EXISTS idx_game_results_score ON game_results(total_score DESC);
      CREATE INDEX IF NOT EXISTS idx_game_results_user_created ON game_results(user_id, created_at DESC);
      CREATE INDEX IF NOT EXISTS idx_game_results_session ON game_results(session_id);
    `);
    console.log('✅ Leaderboard indexes created');
    
    await client.query('COMMIT');
    console.log('🎉 Performance indexes created successfully!');
    
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('❌ Index creation failed:', error.message);
    throw error;
  } finally {
    client.release();
  }
}

async function down(database) {
  const client = await database.getClient();
  
  try {
    await client.query('BEGIN');
    
    console.log('🔄 Dropping performance indexes...');
    
    const indexes = [
      'idx_locations_coordinates',
      'idx_locations_country',
      'idx_locations_difficulty', 
      'idx_locations_category',
      'idx_users_username',
      'idx_users_email',
      'idx_users_last_active',
      'idx_game_sessions_code',
      'idx_game_sessions_type_status',
      'idx_game_sessions_created_by',
      'idx_game_results_score',
      'idx_game_results_user_created',
      'idx_game_results_session'
    ];
    
    for (const index of indexes) {
      await client.query(`DROP INDEX IF EXISTS ${index};`);
    }
    
    await client.query('COMMIT');
    console.log('✅ Indexes rollback completed');
    
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}

module.exports = { up, down };