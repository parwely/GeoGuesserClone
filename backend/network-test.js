require('dotenv').config();
const http = require('http');

console.log('🔍 Testing different IP addresses...');

async function testConnection(host, port = 3000) {
  return new Promise((resolve) => {
    const options = {
      hostname: host,
      port: port,
      path: '/health',
      method: 'GET',
      timeout: 2000
    };

    console.log(`Testing connection to ${host}:${port}...`);
    
    const req = http.request(options, (res) => {
      console.log(`✅ ${host}: Status ${res.statusCode}`);
      
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        try {
          const parsed = JSON.parse(data);
          console.log(`   Response: ${parsed.message}`);
          resolve({ success: true, host, status: res.statusCode });
        } catch (e) {
          console.log(`   Raw response: ${data.substring(0, 100)}`);
          resolve({ success: true, host, status: res.statusCode });
        }
      });
    });

    req.on('error', (error) => {
      console.log(`❌ ${host}: ${error.message}`);
      resolve({ success: false, host, error: error.message });
    });

    req.on('timeout', () => {
      console.log(`⏰ ${host}: Timeout`);
      req.destroy();
      resolve({ success: false, host, error: 'timeout' });
    });

    req.end();
  });
}

async function testAll() {
  const hosts = ['localhost', '127.0.0.1', '0.0.0.0'];
  
  for (const host of hosts) {
    await testConnection(host);
    console.log(''); // Empty line for readability
  }
  
  console.log('🎯 Network test completed');
}

testAll();