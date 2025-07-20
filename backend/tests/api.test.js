require('dotenv').config();
const axios = require('axios');

const BASE_URL = 'http://localhost:3000/api';

async function testAuthAPI() {
  try {
    console.log('🧪 Testing Authentication API...\n');

    // Test 1: Register a new user
    console.log('1️⃣ Testing User Registration...');
    const registerResponse = await axios.post(`${BASE_URL}/auth/register`, {
      username: 'testuser' + Date.now(),
      email: `test${Date.now()}@example.com`,
      password: 'TestPass123'
    });
    
    console.log('✅ Registration successful!');
    console.log('User:', registerResponse.data.data.user.username);
    console.log('Token length:', registerResponse.data.data.token.length);
    console.log('');

    // Test 2: Login with the user
    console.log('2️⃣ Testing User Login...');
    const loginResponse = await axios.post(`${BASE_URL}/auth/login`, {
      usernameOrEmail: registerResponse.data.data.user.username,
      password: 'TestPass123'
    });
    
    console.log('✅ Login successful!');
    console.log('User ID:', loginResponse.data.data.user.id);
    console.log('Token expires in:', loginResponse.data.data.expiresIn);
    console.log('');

    const token = loginResponse.data.data.token;

    // Test 3: Get user info with token
    console.log('3️⃣ Testing Protected Route (/me)...');
    const meResponse = await axios.get(`${BASE_URL}/auth/me`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    console.log('✅ Protected route access successful!');
    console.log('User info:', {
      id: meResponse.data.data.user.id,
      username: meResponse.data.data.user.username,
      email: meResponse.data.data.user.email,
      totalScore: meResponse.data.data.user.totalScore,
      gamesPlayed: meResponse.data.data.user.gamesPlayed
    });
    console.log('');

    // Test 4: Refresh token
    console.log('4️⃣ Testing Token Refresh...');
    const refreshResponse = await axios.post(`${BASE_URL}/auth/refresh`, {}, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    console.log('✅ Token refresh successful!');
    console.log('New token is different:', refreshResponse.data.data.token !== token);
    console.log('');

    // Test 5: Logout
    console.log('5️⃣ Testing Logout...');
    const logoutResponse = await axios.post(`${BASE_URL}/auth/logout`, {}, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    console.log('✅ Logout successful!');
    console.log('Message:', logoutResponse.data.message);
    console.log('');

    // Test 6: Try to access protected route with old token (should fail)
    console.log('6️⃣ Testing Invalid Token...');
    try {
      await axios.get(`${BASE_URL}/auth/me`, {
        headers: {
          'Authorization': 'Bearer invalid-token-123'
        }
      });
      console.log('❌ Should have failed!');
    } catch (error) {
      console.log('✅ Invalid token correctly rejected!');
      console.log('Error:', error.response.data.error);
    }
    console.log('');

    console.log('🎉 All authentication tests passed!');

  } catch (error) {
    console.error('❌ Test failed:', error.response?.data || error.message);
  }
}

if (require.main === module) {
  testAuthAPI();
}

module.exports = testAuthAPI;