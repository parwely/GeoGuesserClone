require('dotenv').config();
const axios = require('axios');

const BASE_URL = 'http://localhost:3000/api';

async function testLocationAPI() {
  try {
    console.log('🗺️ Testing Location Management API...\n');

    // Test 1: Get random location
    console.log('1️⃣ Testing Random Location...');
    const randomResponse = await axios.get(`${BASE_URL}/locations/random`);
    
    console.log('✅ Random location retrieved!');
    console.log('Location:', randomResponse.data.data.locations[0].name);
    console.log('Country:', randomResponse.data.data.locations[0].country);
    console.log('Difficulty:', randomResponse.data.data.locations[0].difficultyName);
    console.log('');

    // Test 2: Get multiple random locations with filters
    console.log('2️⃣ Testing Filtered Random Locations...');
    const filteredResponse = await axios.get(`${BASE_URL}/locations/random?count=3&difficulty=2&category=urban`);
    
    console.log('✅ Filtered locations retrieved!');
    console.log('Count:', filteredResponse.data.data.count);
    console.log('Locations:', filteredResponse.data.data.locations.map(l => `${l.name}, ${l.country}`));
    console.log('');

    // Test 3: Get location by ID
    console.log('3️⃣ Testing Get Location by ID...');
    const locationId = randomResponse.data.data.locations[0].id;
    const locationResponse = await axios.get(`${BASE_URL}/locations/${locationId}`);
    
    console.log('✅ Location by ID retrieved!');
    console.log('ID:', locationResponse.data.data.location.id);
    console.log('Name:', locationResponse.data.data.location.name);
    console.log('Coordinates:', locationResponse.data.data.location.coordinates);
    console.log('');

    // Test 4: Get locations by difficulty
    console.log('4️⃣ Testing Locations by Difficulty...');
    const difficultyResponse = await axios.get(`${BASE_URL}/locations/difficulty/3?limit=2`);
    
    console.log('✅ Locations by difficulty retrieved!');
    console.log('Difficulty:', difficultyResponse.data.data.difficultyName);
    console.log('Count:', difficultyResponse.data.data.count);
    console.log('');

    // Test 5: Get locations by category
    console.log('5️⃣ Testing Locations by Category...');
    const categoryResponse = await axios.get(`${BASE_URL}/locations/category/landmark?limit=2`);
    
    console.log('✅ Locations by category retrieved!');
    console.log('Category:', categoryResponse.data.data.category);
    console.log('Count:', categoryResponse.data.data.count);
    console.log('');

    // Test 6: Search locations near coordinates (Paris)
    console.log('6️⃣ Testing Location Search Near Coordinates...');
    const nearResponse = await axios.get(`${BASE_URL}/locations/near/48.8566/2.3522?radius=500&limit=3`);
    
    console.log('✅ Nearby locations found!');
    console.log('Search center:', nearResponse.data.data.searchCenter);
    console.log('Radius:', nearResponse.data.data.radiusKm, 'km');
    console.log('Found:', nearResponse.data.data.count, 'locations');
    if (nearResponse.data.data.locations.length > 0) {
      console.log('Closest:', nearResponse.data.data.locations[0].name, 
                  `(${nearResponse.data.data.locations[0].distanceKm} km away)`);
    }
    console.log('');

    // Test 7: Get location statistics
    console.log('7️⃣ Testing Location Statistics...');
    const statsResponse = await axios.get(`${BASE_URL}/locations/stats/overview`);
    
    console.log('✅ Location statistics retrieved!');
    console.log('Total locations:', statsResponse.data.data.stats.total);
    console.log('By difficulty:', statsResponse.data.data.stats.byDifficulty);
    console.log('By category:', statsResponse.data.data.stats.byCategory);
    console.log('Top countries:', statsResponse.data.data.stats.byCountry.slice(0, 3));
    console.log('');

    // Test 8: Calculate distance between locations
    console.log('8️⃣ Testing Distance Calculation...');
    if (filteredResponse.data.data.locations.length >= 2) {
      const loc1 = filteredResponse.data.data.locations[0].id;
      const loc2 = filteredResponse.data.data.locations[1].id;
      const distanceResponse = await axios.get(`${BASE_URL}/locations/distance/${loc1}/${loc2}`);
      
      console.log('✅ Distance calculated!');
      console.log('Between:', distanceResponse.data.data.distance.location1);
      console.log('And:', distanceResponse.data.data.distance.location2);
      console.log('Distance:', distanceResponse.data.data.distance.distanceKm, 'km');
    } else {
      console.log('⏭️ Skipping distance test (need at least 2 locations)');
    }
    console.log('');

    // Test 9: Error handling - Invalid location ID
    console.log('9️⃣ Testing Error Handling...');
    try {
      await axios.get(`${BASE_URL}/locations/99999`);
      console.log('❌ Should have failed!');
    } catch (error) {
      console.log('✅ Invalid location ID correctly rejected!');
      console.log('Error:', error.response.data.error);
    }
    console.log('');

    console.log('🎉 All location API tests passed!');

  } catch (error) {
    console.error('❌ Location API test failed:', error.response?.data || error.message);
  }
}

if (require.main === module) {
  testLocationAPI();
}

module.exports = testLocationAPI;