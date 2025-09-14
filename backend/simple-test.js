require('dotenv').config();

// Simple test to verify the server is accessible
const http = require('http');

console.log('🔍 Simple connectivity test...');

const options = {
  hostname: 'localhost',
  port: 3000,
  path: '/health',
  method: 'GET'
};

const req = http.request(options, (res) => {
  console.log('✅ Connection successful!');
  console.log('Status:', res.statusCode);
  console.log('Headers:', res.headers);
  
  let data = '';
  res.on('data', (chunk) => {
    data += chunk;
  });
  
  res.on('end', () => {
    console.log('Response body:', data);
    
    // If health check works, try locations endpoint
    if (res.statusCode === 200) {
      console.log('\n🔍 Testing locations endpoint...');
      
      const locOptions = {
        hostname: 'localhost',
        port: 3000,
        path: '/api/locations',
        method: 'GET'
      };
      
      const locReq = http.request(locOptions, (locRes) => {
        console.log('✅ Locations endpoint status:', locRes.statusCode);
        
        let locData = '';
        locRes.on('data', (chunk) => {
          locData += chunk;
        });
        
        locRes.on('end', () => {
          try {
            const locations = JSON.parse(locData);
            console.log('✅ Found', locations.length, 'locations');
            if (locations.length > 0) {
              console.log('First location:', locations[0].name, '(' + locations[0].country + ')');
              console.log('Coordinates:', locations[0].coordinates.latitude + ',' + locations[0].coordinates.longitude);
            }
          } catch (error) {
            console.log('Response data:', locData.substring(0, 200));
          }
        });
      });
      
      locReq.on('error', (error) => {
        console.error('❌ Locations request failed:', error.message);
      });
      
      locReq.end();
    }
  });
});

req.on('error', (error) => {
  console.error('❌ Health check failed:', error.message);
});

req.end();