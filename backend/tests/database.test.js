require('dotenv').config();
const database = require('../src/database/connection'); // Fixed: Added ../ to go up one directory

async function testDatabase() {
  try {
    await database.connect();
    
    console.log('🔍 Testing database functionality...');
    
    // Test basic query
    const locationCount = await database.query('SELECT COUNT(*) as count FROM locations');
    console.log(`📍 Total locations: ${locationCount.rows[0].count}`);
    
    // Test PostGIS functionality
    const spatialQuery = await database.query(`
      SELECT 
        name, 
        country, 
        ST_X(coordinates) as longitude, 
        ST_Y(coordinates) as latitude,
        difficulty
      FROM locations 
      ORDER BY difficulty DESC
      LIMIT 3
    `);
    
    console.log('🌍 Sample locations with coordinates:');
    spatialQuery.rows.forEach(row => {
      console.log(`  • ${row.name}, ${row.country} (${row.latitude.toFixed(4)}, ${row.longitude.toFixed(4)}) - Difficulty: ${row.difficulty}`);
    });
    
    // Test distance calculation (Haversine formula via PostGIS)
    const distanceQuery = await database.query(`
      SELECT 
        name,
        country,
        ST_Distance(
          coordinates,
          ST_SetSRID(ST_MakePoint(2.2945, 48.8584), 4326)
        ) / 1000 as distance_km
      FROM locations
      WHERE name != 'Eiffel Tower Area'
      ORDER BY distance_km
      LIMIT 1
    `);
    
    console.log('🎯 Closest location to Eiffel Tower:');
    console.log(`  • ${distanceQuery.rows[0].name}, ${distanceQuery.rows[0].country} - ${Math.round(distanceQuery.rows[0].distance_km)} km away`);
    
    console.log('✅ Database test completed successfully!');
    
  } catch (error) {
    console.error('❌ Database test failed:', error.message);
  } finally {
    await database.disconnect();
  }
}

testDatabase();