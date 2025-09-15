const database = require("../connection");

const sampleLocations = [
  // URBAN - all difficulties
  {
    name: "Times Square",
    country: "United States", 
    city: "New York",
    latitude: 40.758896,
    longitude: -73.985130,
    difficulty: 1,
    category: "urban",
    image_urls: ["https://example.com/times-square.jpg"],
    hints: { signs: "English", traffic: "yellow taxis", lights: "neon" }
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
    hints: { driving: "left side", architecture: "Victorian", traffic: "buses" }
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
    hints: { crossing: "pedestrian", signs: "Japanese", crowds: "dense" }
  },
  {
    name: "Dam Square",
    country: "Netherlands",
    city: "Amsterdam",
    latitude: 52.373169,
    longitude: 4.892866,
    difficulty: 4,
    category: "urban",
    image_urls: ["https://example.com/dam-square.jpg"],
    hints: { palace: "royal", trams: "present", architecture: "dutch" }
  },
  {
    name: "Prague Old Town",
    country: "Czech Republic",
    city: "Prague",
    latitude: 50.0755,
    longitude: 14.4378,
    difficulty: 5,
    category: "urban",
    image_urls: ["https://example.com/prague.jpg"],
    hints: { architecture: "gothic", cobblestone: "streets", castle: "visible" }
  },

  // COASTAL - all difficulties
  {
    name: "Miami Beach",
    country: "United States",
    city: "Miami",
    latitude: 25.790654,
    longitude: -80.130045,
    difficulty: 1,
    category: "coastal",
    image_urls: ["https://example.com/miami-beach.jpg"],
    hints: { architecture: "art_deco", palm_trees: "present", beach: "white_sand" }
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
    hints: { beach: "famous", surfers: "present", cliffs: "sandstone" }
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
    hints: { beach: "curved", mountains: "sugarloaf", portuguese: "language" }
  },
  {
    name: "Amalfi Coast",
    country: "Italy",
    city: "Amalfi",
    latitude: 40.6340,
    longitude: 14.6027,
    difficulty: 4,
    category: "coastal",
    image_urls: ["https://example.com/amalfi.jpg"],
    hints: { cliffs: "dramatic", lemon_trees: "present", mediterranean: "sea" }
  },
  {
    name: "Big Sur Coast",
    country: "United States",
    city: "California",
    latitude: 36.2704,
    longitude: -121.8076,
    difficulty: 5,
    category: "coastal",
    image_urls: ["https://example.com/big-sur.jpg"],
    hints: { redwoods: "forest", pacific: "ocean", fog: "frequent" }
  },

  // DESERT - all difficulties
  {
    name: "Death Valley",
    country: "United States",
    city: "Death Valley",
    latitude: 36.5323,
    longitude: -116.9325,
    difficulty: 1,
    category: "desert",
    image_urls: ["https://example.com/death-valley.jpg"],
    hints: { landscape: "desert", heat: "extreme", below_sea_level: "true" }
  },
  {
    name: "Wadi Rum",
    country: "Jordan",
    city: "Aqaba",
    latitude: 29.5765,
    longitude: 35.4206,
    difficulty: 2,
    category: "desert",
    image_urls: ["https://example.com/wadi-rum.jpg"],
    hints: { rock_formations: "red", desert: "protected", nomads: "bedouin" }
  },
  {
    name: "Sahara Desert Road",
    country: "Morocco",
    city: "Merzouga",
    latitude: 31.0801,
    longitude: -4.0133,
    difficulty: 3,
    category: "desert",
    image_urls: ["https://example.com/sahara.jpg"],
    hints: { landscape: "desert", dunes: "sand", vegetation: "sparse" }
  },
  {
    name: "Atacama Desert",
    country: "Chile",
    city: "San Pedro de Atacama",
    latitude: -22.9098,
    longitude: -68.2003,
    difficulty: 4,
    category: "desert",
    image_urls: ["https://example.com/atacama.jpg"],
    hints: { altitude: "high", landscape: "arid", mountains: "volcanic" }
  },
  {
    name: "Gobi Desert",
    country: "Mongolia",
    city: "Dalanzadgad",
    latitude: 43.5758,
    longitude: 104.4253,
    difficulty: 5,
    category: "desert",
    image_urls: ["https://example.com/gobi.jpg"],
    hints: { climate: "extreme", landscape: "barren", nomads: "present" }
  },

  // RURAL - all difficulties
  {
    name: "Irish Countryside",
    country: "Ireland",
    city: "Cork",
    latitude: 51.8985,
    longitude: -8.4756,
    difficulty: 1,
    category: "rural",
    image_urls: ["https://example.com/ireland.jpg"],
    hints: { fields: "green", stone_walls: "present", sheep: "grazing" }
  },
  {
    name: "French Countryside",
    country: "France",
    city: "Provence",
    latitude: 43.9352,
    longitude: 4.8357,
    difficulty: 2,
    category: "rural",
    image_urls: ["https://example.com/provence.jpg"],
    hints: { lavender: "fields", vineyards: "present", stone_houses: "old" }
  },
  {
    name: "Tuscany Countryside",
    country: "Italy",
    city: "Val d'Orcia",
    latitude: 43.0642,
    longitude: 11.6041,
    difficulty: 3,
    category: "rural",
    image_urls: ["https://example.com/tuscany.jpg"],
    hints: { hills: "rolling", vineyards: "present", cypresses: "tall" }
  },
  {
    name: "Scottish Highlands",
    country: "United Kingdom",
    city: "Inverness",
    latitude: 57.4778,
    longitude: -4.2247,
    difficulty: 4,
    category: "rural", 
    image_urls: ["https://example.com/highlands.jpg"],
    hints: { landscape: "mountainous", sheep: "present", castles: "ancient" }
  },
  {
    name: "Japanese Village",
    country: "Japan",
    city: "Shirakawa-go",
    latitude: 36.2578,
    longitude: 136.9061,
    difficulty: 5,
    category: "rural",
    image_urls: ["https://example.com/shirakawa.jpg"],
    hints: { architecture: "traditional", mountains: "visible", rice: "fields" }
  },

  // ARCTIC - all difficulties
  {
    name: "Iceland Ring Road",
    country: "Iceland",
    city: "Reykjavik",
    latitude: 64.1466,
    longitude: -21.9426,
    difficulty: 1,
    category: "arctic",
    image_urls: ["https://example.com/iceland.jpg"],
    hints: { geysers: "present", volcanic: "landscape", aurora: "visible" }
  },
  {
    name: "Northern Canada",
    country: "Canada",
    city: "Yellowknife",
    latitude: 62.4540,
    longitude: -114.3718,
    difficulty: 2,
    category: "arctic",
    image_urls: ["https://example.com/yellowknife.jpg"],
    hints: { lakes: "frozen", aurora: "borealis", indigenous: "culture" }
  },
  {
    name: "Siberian Road",
    country: "Russia",
    city: "Norilsk",
    latitude: 69.3558,
    longitude: 88.1893,
    difficulty: 3,
    category: "arctic",
    image_urls: ["https://example.com/siberia.jpg"],
    hints: { tundra: "landscape", permafrost: "ground", mining: "city" }
  },
  {
    name: "Lapland Road",
    country: "Finland",
    city: "Rovaniemi",
    latitude: 66.5039,
    longitude: 25.7294,
    difficulty: 4,
    category: "arctic",
    image_urls: ["https://example.com/lapland.jpg"],
    hints: { trees: "evergreen", snow: "frequent", reindeer: "present" }
  },
  {
    name: "Svalbard Settlement",
    country: "Norway",
    city: "Longyearbyen",
    latitude: 78.2232,
    longitude: 15.6267,
    difficulty: 5,
    category: "arctic",
    image_urls: ["https://example.com/svalbard.jpg"],
    hints: { arctic: "circle", polar_bears: "warning_signs", permafrost: "visible" }
  },

  // NATURE - all difficulties  
  {
    name: "Central Park",
    country: "United States",
    city: "New York",
    latitude: 40.785091,
    longitude: -73.968285,
    difficulty: 1,
    category: "forest",
    image_urls: ["https://example.com/central-park.jpg"],
    hints: { park: "urban", skyscrapers: "surrounding", paths: "paved" }
  },
  {
    name: "Yosemite Valley",
    country: "United States",
    city: "California", 
    latitude: 37.7459,
    longitude: -119.5936,
    difficulty: 2,
    category: "mountain",
    image_urls: ["https://example.com/yosemite.jpg"],
    hints: { granite: "cliffs", waterfalls: "tall", sequoia: "trees" }
  },
  {
    name: "Yellowstone National Park",
    country: "United States",
    city: "Yellowstone",
    latitude: 44.4280,
    longitude: -110.5885,
    difficulty: 3,
    category: "forest",
    image_urls: ["https://example.com/yellowstone.jpg"],
    hints: { geysers: "present", wildlife: "bison", landscape: "volcanic" }
  },
  {
    name: "Banff National Park",
    country: "Canada",
    city: "Banff",
    latitude: 51.4968,
    longitude: -115.9281,
    difficulty: 4,
    category: "mountain",
    image_urls: ["https://example.com/banff.jpg"],
    hints: { lakes: "turquoise", mountains: "rocky", wildlife: "bears" }
  },
  {
    name: "Amazon Rainforest Road",
    country: "Brazil",
    city: "Manaus",
    latitude: -3.4653,
    longitude: -62.2159,
    difficulty: 5,
    category: "forest",
    image_urls: ["https://example.com/amazon.jpg"],
    hints: { vegetation: "dense", humidity: "high", wildlife: "diverse" }
  },

  // LANDMARK - all difficulties
  {
    name: "Golden Gate Bridge",
    country: "United States",
    city: "San Francisco",
    latitude: 37.819722,
    longitude: -122.478611,
    difficulty: 1,
    category: "landmark",
    image_urls: ["https://example.com/golden-gate.jpg"],
    hints: { bridge: "suspension", color: "red", fog: "frequent" }
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
    hints: { bridge: "arch", water: "harbor", opera_house: "nearby" }
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
    hints: { gate: "historic", columns: "sandstone", square: "wide" }
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
    hints: { square: "cobblestone", kremlin: "walls", domes: "colorful" }
  },
  {
    name: "Machu Picchu",
    country: "Peru",
    city: "Cusco",
    latitude: -13.1631,
    longitude: -72.545,
    difficulty: 5,
    category: "landmark",
    image_urls: ["https://example.com/machu-picchu.jpg"],
    hints: { ruins: "Inca", altitude: "high", terraces: "stone" }
  }
];

async function seedLocations() {
  try {
    console.log("🌍 Seeding sample locations...");
    await database.connect();
    
    const deleteQuery = "DELETE FROM locations WHERE 1=1";
    await database.query(deleteQuery);
    console.log("🗑️ Cleared existing locations");

    for (const location of sampleLocations) {
      const insertQuery = `
        INSERT INTO locations (name, country, city, coordinates, difficulty, category, image_urls, hints)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
      `;
      const coordinates = {
        type: "Point",
        coordinates: [location.longitude, location.latitude]
      };
      await database.query(insertQuery, [
        location.name,
        location.country,
        location.city,
        coordinates,
        location.difficulty,
        location.category,
        location.image_urls,
        location.hints
      ]);
      console.log(`✅ Added location: ${location.name}, ${location.country}`);
    }

    console.log(`🎉 Successfully seeded ${sampleLocations.length} locations!`);
    
    const categories = {};
    const difficulties = {};
    sampleLocations.forEach(loc => {
      categories[loc.category] = (categories[loc.category] || 0) + 1;
      difficulties[loc.difficulty] = (difficulties[loc.difficulty] || 0) + 1;
    });
    console.log("📊 Category distribution:", categories);
    console.log("📊 Difficulty distribution:", difficulties);

  } catch (error) {
    console.error("❌ Failed to seed locations:", error.message);
  } finally {
    process.exit(0);
  }
}

if (require.main === module) {
  seedLocations();
}

module.exports = { sampleLocations, seedLocations };