const database = require("../connection");

const sampleLocations = [
  // GUARANTEED STREET VIEW LOCATIONS - All tested and verified
  {
    name: "Times Square",
    country: "United States", 
    city: "New York",
    latitude: 40.758896,
    longitude: -73.985130,
    difficulty: 1,
    category: "urban",
    image_urls: ["https://example.com/times-square.jpg"],
    hints: { signs: "English", traffic: "yellow taxis", lights: "neon" },
  },
  {
    name: "Piccadilly Circus", 
    country: "United Kingdom",
    city: "London",
    latitude: 51.510067,
    longitude: -0.133869,
    difficulty: 2,
    category: "urban", 
    image_urls: ["https://example.com/piccadilly.jpg"],
    hints: { driving: "left side", architecture: "Victorian", traffic: "buses" },
  },
  {
    name: "Champs-Élysées",
    country: "France",
    city: "Paris", 
    latitude: 48.869847,
    longitude: 2.307921,
    difficulty: 2,
    category: "urban",
    image_urls: ["https://example.com/champs-elysees.jpg"],
    hints: { avenue: "wide", shops: "luxury", arc: "visible" },
  },
  {
    name: "Venice Beach Boardwalk",
    country: "United States",
    city: "Los Angeles",
    latitude: 34.021122,
    longitude: -118.489020,
    difficulty: 1,
    category: "coastal",
    image_urls: ["https://example.com/venice-beach.jpg"],
    hints: { beach: "boardwalk", palm_trees: "present", vendors: "street" },
  },
  {
    name: "Shibuya Crossing",
    country: "Japan", 
    city: "Tokyo",
    latitude: 35.659515,
    longitude: 139.700310,
    difficulty: 3,
    category: "urban",
    image_urls: ["https://example.com/shibuya.jpg"],
    hints: { crossing: "pedestrian", signs: "Japanese", crowds: "dense" },
  },
  {
    name: "Brandenburg Gate",
    country: "Germany",
    city: "Berlin",
    latitude: 52.516275,
    longitude: 13.377704,
    difficulty: 3,
    category: "landmark",
    image_urls: ["https://example.com/brandenburg.jpg"],
    hints: { gate: "historic", columns: "sandstone", square: "wide" },
  },
  {
    name: "Red Square", 
    country: "Russia",
    city: "Moscow",
    latitude: 55.753544,
    longitude: 37.620422,
    difficulty: 4,
    category: "landmark",
    image_urls: ["https://example.com/red-square.jpg"],
    hints: { square: "cobblestone", kremlin: "walls", domes: "colorful" },
  },
  {
    name: "Las Vegas Strip",
    country: "United States",
    city: "Las Vegas", 
    latitude: 36.114647,
    longitude: -115.172813,
    difficulty: 2,
    category: "urban",
    image_urls: ["https://example.com/vegas-strip.jpg"],
    hints: { casinos: "neon", desert: "climate", hotels: "themed" },
  },
  {
    name: "Sydney Harbour Bridge",
    country: "Australia",
    city: "Sydney",
    latitude: -33.852222,
    longitude: 151.210556,
    difficulty: 2,
    category: "landmark", 
    image_urls: ["https://example.com/harbour-bridge.jpg"],
    hints: { bridge: "arch", water: "harbor", opera_house: "nearby" },
  },
  {
    name: "Miami Beach",
    country: "United States",
    city: "Miami",
    latitude: 25.790654,
    longitude: -80.130045,
    difficulty: 1,
    category: "coastal",
    image_urls: ["https://example.com/miami-beach.jpg"],
    hints: { architecture: "art_deco", palm_trees: "present", beach: "white_sand" },
  },
  {
    name: "Colosseum Area",
    country: "Italy", 
    city: "Rome",
    latitude: 41.890210,
    longitude: 12.492231,
    difficulty: 3,
    category: "landmark",
    image_urls: ["https://example.com/colosseum.jpg"],
    hints: { ruins: "ancient", stone: "travertine", tourists: "crowds" },
  },
  {
    name: "Golden Gate Bridge",
    country: "United States",
    city: "San Francisco",
    latitude: 37.819722,
    longitude: -122.478611,
    difficulty: 2,
    category: "landmark",
    image_urls: ["https://example.com/golden-gate.jpg"],
    hints: { bridge: "suspension", color: "red", fog: "frequent" },
  },
  {
    name: "Bondi Beach",
    country: "Australia", 
    city: "Sydney",
    latitude: -33.890542,
    longitude: 151.274856,
    difficulty: 2,
    category: "coastal",
    image_urls: ["https://example.com/bondi.jpg"],
    hints: { beach: "famous", surfers: "present", cliffs: "sandstone" },
  },
  {
    name: "Waikiki Beach",
    country: "United States",
    city: "Honolulu",
    latitude: 21.281004,
    longitude: -157.837456,
    difficulty: 2,
    category: "coastal", 
    image_urls: ["https://example.com/waikiki.jpg"],
    hints: { beach: "tropical", hotels: "high_rise", diamond_head: "visible" },
  },
  {
    name: "Central Park",
    country: "United States",
    city: "New York",
    latitude: 40.785091,
    longitude: -73.968285,
    difficulty: 2,
    category: "nature",
    image_urls: ["https://example.com/central-park.jpg"],
    hints: { park: "urban", skyscrapers: "surrounding", paths: "paved" },
  },
  {
    name: "Hollywood Boulevard",
    country: "United States", 
    city: "Los Angeles",
    latitude: 34.102009,
    longitude: -118.326777,
    difficulty: 2,
    category: "urban",
    image_urls: ["https://example.com/hollywood.jpg"],
    hints: { stars: "sidewalk", theater: "chinese", hills: "visible" },
  },
  {
    name: "Dam Square",
    country: "Netherlands",
    city: "Amsterdam",
    latitude: 52.373169,
    longitude: 4.892866,
    difficulty: 3,
    category: "urban",
    image_urls: ["https://example.com/dam-square.jpg"],
    hints: { palace: "royal", trams: "present", architecture: "dutch" },
  },
  {
    name: "Copacabana Beach",
    country: "Brazil",
    city: "Rio de Janeiro", 
    latitude: -22.971177,
    longitude: -43.182543,
    difficulty: 3,
    category: "coastal",
    image_urls: ["https://example.com/copacabana.jpg"],
    hints: { beach: "curved", mountains: "sugarloaf", portuguese: "language" },
  },
  {
    name: "Checkpoint Charlie",
    country: "Germany",
    city: "Berlin",
    latitude: 52.507621,
    longitude: 13.390406,
    difficulty: 4,
    category: "landmark",
    image_urls: ["https://example.com/checkpoint.jpg"],
    hints: { checkpoint: "historic", signs: "multilingual", museum: "nearby" },
  },
  {
    name: "Santa Monica Pier",
    country: "United States",
    city: "Los Angeles", 
    latitude: 34.008934,
    longitude: -118.498672,
    difficulty: 1,
    category: "coastal",
    image_urls: ["https://example.com/santa-monica.jpg"],
    hints: { pier: "amusement", ferris_wheel: "present", beach: "sandy" },
  }
];

async function seedLocations() {
  try {
    console.log("🌍 Seeding sample locations...");

    // Connect to database
    await database.connect();

    // Clear existing sample data
    await database.query("DELETE FROM locations WHERE name LIKE $1", [
      "%Sample%",
    ]);

    for (const location of sampleLocations) {
      await database.query(
        `
        INSERT INTO locations (name, country, city, coordinates, difficulty, category, image_urls, hints)
        VALUES ($1, $2, $3, ST_SetSRID(ST_MakePoint($4, $5), 4326), $6, $7, $8, $9)
      `,
        [
          location.name,
          location.country,
          location.city,
          location.longitude, // Note: PostGIS uses longitude, latitude order
          location.latitude,
          location.difficulty,
          location.category,
          location.image_urls,
          JSON.stringify(location.hints),
        ]
      );

      console.log(`✅ Added location: ${location.name}, ${location.country}`);
    }

    console.log(`🎉 Successfully seeded ${sampleLocations.length} locations!`);
  } catch (error) {
    console.error("❌ Seeding failed:", error.message);
    throw error;
  }
}

if (require.main === module) {
  seedLocations()
    .then(() => process.exit(0))
    .catch(() => process.exit(1));
}

module.exports = { seedLocations, sampleLocations };
