const database = require("../connection");

const sampleLocations = [
  {
    name: "Eiffel Tower Area",
    country: "France",
    city: "Paris",
    latitude: 48.8584,
    longitude: 2.2945,
    difficulty: 2,
    category: "landmark",
    image_urls: [
      "https://example.com/paris1.jpg",
      "https://example.com/paris2.jpg",
    ],
    hints: { architecture: "Haussmanian", period: "19th century" },
  },
  {
    name: "Times Square",
    country: "United States",
    city: "New York",
    latitude: 40.758,
    longitude: -73.9855,
    difficulty: 1,
    category: "urban",
    image_urls: ["https://example.com/nyc1.jpg"],
    hints: { signs: "English", traffic: "yellow taxis" },
  },
  {
    name: "Rural Japanese Village",
    country: "Japan",
    city: "Shirakawa-go",
    latitude: 36.2578,
    longitude: 136.9061,
    difficulty: 4,
    category: "rural",
    image_urls: ["https://example.com/japan1.jpg"],
    hints: { architecture: "traditional", mountains: "visible" },
  },
  {
    name: "Sahara Desert Road",
    country: "Morocco",
    city: "Merzouga",
    latitude: 31.0801,
    longitude: -4.0133,
    difficulty: 5,
    category: "desert",
    image_urls: ["https://example.com/sahara1.jpg"],
    hints: { landscape: "desert", vegetation: "sparse" },
  },
  {
    name: "London Bridge Area",
    country: "United Kingdom",
    city: "London",
    latitude: 51.5074,
    longitude: -0.0883,
    difficulty: 2,
    category: "urban",
    image_urls: ["https://example.com/london1.jpg"],
    hints: { driving: "left side", language: "English" },
  },
  {
    name: "Sydney Opera House",
    country: "Australia",
    city: "Sydney",
    latitude: -33.8568,
    longitude: 151.2153,
    difficulty: 2,
    category: "landmark",
    image_urls: ["https://example.com/sydney1.jpg"],
    hints: { architecture: "modern", water: "harbor" },
  },
  {
    name: "Grand Canyon",
    country: "United States",
    city: "Grand Canyon Village",
    latitude: 36.1069,
    longitude: -112.1129,
    difficulty: 4,
    category: "nature",
    image_urls: ["https://example.com/grandcanyon1.jpg"],
    hints: { landscape: "canyon", color: "red rocks" },
  },
  {
    name: "Copacabana Beach",
    country: "Brazil",
    city: "Rio de Janeiro",
    latitude: -22.9711,
    longitude: -43.1822,
    difficulty: 1,
    category: "coastal",
    image_urls: ["https://example.com/copacabana1.jpg"],
    hints: { beach: "famous", city: "Rio" },
  },
  {
    name: "Mount Fuji",
    country: "Japan",
    city: "Fujinomiya",
    latitude: 35.3606,
    longitude: 138.7274,
    difficulty: 3,
    category: "mountain",
    image_urls: ["https://example.com/fuji1.jpg"],
    hints: { mountain: "volcano", snow: "peak" },
  },
  {
    name: "Santorini Cliffs",
    country: "Greece",
    city: "Santorini",
    latitude: 36.3932,
    longitude: 25.4615,
    difficulty: 2,
    category: "coastal",
    image_urls: ["https://example.com/santorini1.jpg"],
    hints: { buildings: "white", sea: "blue" },
  },
  {
    name: "Banff National Park",
    country: "Canada",
    city: "Banff",
    latitude: 51.4968,
    longitude: -115.9281,
    difficulty: 3,
    category: "nature",
    image_urls: ["https://example.com/banff1.jpg"],
    hints: { lakes: "turquoise", mountains: "rocky" },
  },
  {
    name: "Table Mountain",
    country: "South Africa",
    city: "Cape Town",
    latitude: -33.9628,
    longitude: 18.4098,
    difficulty: 3,
    category: "mountain",
    image_urls: ["https://example.com/tablemountain1.jpg"],
    hints: { mountain: "flat top", city: "Cape Town" },
  },
  {
    name: "Petra",
    country: "Jordan",
    city: "Petra",
    latitude: 30.3285,
    longitude: 35.4444,
    difficulty: 5,
    category: "landmark",
    image_urls: ["https://example.com/petra1.jpg"],
    hints: { ruins: "ancient", color: "rose" },
  },
  {
    name: "Uluru",
    country: "Australia",
    city: "Uluru",
    latitude: -25.3444,
    longitude: 131.0369,
    difficulty: 4,
    category: "nature",
    image_urls: ["https://example.com/uluru1.jpg"],
    hints: { rock: "large", color: "red" },
  },
  {
    name: "Machu Picchu",
    country: "Peru",
    city: "Cusco",
    latitude: -13.1631,
    longitude: -72.545,
    difficulty: 5,
    category: "landmark",
    image_urls: ["https://example.com/machu1.jpg"],
    hints: { ruins: "Inca", mountain: "Andes" },
  },
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
