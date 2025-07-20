const database = require('../connection');

const sampleLocations = [
  {
    name: 'Eiffel Tower Area',
    country: 'France',
    city: 'Paris',
    latitude: 48.8584,
    longitude: 2.2945,
    difficulty: 2,
    category: 'landmark',
    image_urls: ['https://example.com/paris1.jpg', 'https://example.com/paris2.jpg'],
    hints: { architecture: 'Haussmanian', period: '19th century' }
  },
  {
    name: 'Times Square',
    country: 'United States',
    city: 'New York',
    latitude: 40.7580,
    longitude: -73.9855,
    difficulty: 1,
    category: 'urban',
    image_urls: ['https://example.com/nyc1.jpg'],
    hints: { signs: 'English', traffic: 'yellow taxis' }
  },
  {
    name: 'Rural Japanese Village',
    country: 'Japan',
    city: 'Shirakawa-go',
    latitude: 36.2578,
    longitude: 136.9061,
    difficulty: 4,
    category: 'rural',
    image_urls: ['https://example.com/japan1.jpg'],
    hints: { architecture: 'traditional', mountains: 'visible' }
  },
  {
    name: 'Sahara Desert Road',
    country: 'Morocco',
    city: 'Merzouga',
    latitude: 31.0801,
    longitude: -4.0133,
    difficulty: 5,
    category: 'desert',
    image_urls: ['https://example.com/sahara1.jpg'],
    hints: { landscape: 'desert', vegetation: 'sparse' }
  },
  {
    name: 'London Bridge Area',
    country: 'United Kingdom',
    city: 'London',
    latitude: 51.5074,
    longitude: -0.0883,
    difficulty: 2,
    category: 'urban',
    image_urls: ['https://example.com/london1.jpg'],
    hints: { driving: 'left side', language: 'English' }
  }
];

async function seedLocations() {
  try {
    console.log('🌍 Seeding sample locations...');
    
    await database.connect();
    
    // Clear existing sample data
    await database.query('DELETE FROM locations WHERE name LIKE $1', ['%Sample%']);
    
    for (const location of sampleLocations) {
      await database.query(`
        INSERT INTO locations (name, country, city, coordinates, difficulty, category, image_urls, hints)
        VALUES ($1, $2, $3, ST_SetSRID(ST_MakePoint($4, $5), 4326), $6, $7, $8, $9)
      `, [
        location.name,
        location.country,
        location.city,
        location.longitude, // Note: PostGIS uses longitude, latitude order
        location.latitude,
        location.difficulty,
        location.category,
        location.image_urls,
        JSON.stringify(location.hints)
      ]);
      
      console.log(`✅ Added location: ${location.name}, ${location.country}`);
    }
    
    console.log(`🎉 Successfully seeded ${sampleLocations.length} locations!`);
    
  } catch (error) {
    console.error('❌ Seeding failed:', error.message);
    throw error;
  }
}

if (require.main === module) {
  seedLocations()
    .then(() => process.exit(0))
    .catch(() => process.exit(1));
}

module.exports = { seedLocations, sampleLocations };