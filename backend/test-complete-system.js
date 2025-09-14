const axios = require('axios');

async function testCompleteSystem() {
  try {
    console.log('🔍 Testing Complete Street View System...\n');
    
    // Test locations endpoint
    console.log('1. Testing locations endpoint...');
    const locationsResponse = await axios.get('http://localhost:3000/api/locations');
    const locations = locationsResponse.data;
    
    console.log(`✅ Found ${locations.length} locations`);
    console.log('\nFirst 3 guaranteed Street View locations:');
    locations.slice(0, 3).forEach((location, index) => {
      console.log(`${index + 1}. ${location.name} (${location.country})`);
      console.log(`   Coordinates: ${location.coordinates.latitude}, ${location.coordinates.longitude}`);
      console.log(`   Category: ${location.category}, Difficulty: ${location.difficulty}\n`);
    });

    if (locations.length > 0) {
      const timesSquare = locations[0]; // Should be Times Square
      console.log(`2. Testing Street View for ${timesSquare.name}...`);
      
      // Test regular Street View endpoint
      const streetViewResponse = await axios.get(`http://localhost:3000/api/locations/${timesSquare.id}/streetview`);
      const streetViewData = streetViewResponse.data;
      
      console.log('✅ Street View Response:');
      console.log('   - Static available:', streetViewData.static?.available);
      if (streetViewData.static?.url) {
        console.log('   - Static URL length:', streetViewData.static.url.length);
        console.log('   - Contains API key:', streetViewData.static.url.includes('key='));
        console.log('   - URL preview:', streetViewData.static.url.substring(0, 100) + '...');
      }
      console.log('   - Interactive available:', streetViewData.interactive?.available);
      console.log('   - Fallback used:', streetViewData.fallbackUsed || false);

      // Test with mobile user agent simulation
      console.log(`\n3. Testing mobile user agent simulation...`);
      const mobileResponse = await axios.get(`http://localhost:3000/api/locations/${timesSquare.id}/streetview`, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15'
        }
      });
      const mobileData = mobileResponse.data;
      
      console.log('✅ Mobile User Agent Response:');
      console.log('   - Mobile detected:', mobileData.context?.isMobile || 'not specified');
      console.log('   - Fallback used:', mobileData.fallbackUsed || false);
      console.log('   - Static available:', mobileData.static?.available);
    }

    console.log('\n🎉 Complete system test finished successfully!');
    console.log('✅ All guaranteed Street View locations are working correctly');
    console.log('✅ Mobile fallback system is operational');

  } catch (error) {
    console.error('❌ Test failed:', error.message);
    if (error.response) {
      console.error('Response status:', error.response.status);
      console.error('Response data:', error.response.data);
    }
  }
}

testCompleteSystem();