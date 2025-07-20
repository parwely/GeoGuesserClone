//Authentication API tests

const request = require('supertest');
const app = require('../src/server');
const database = require('../src/database/connection');

describe('Authentication API', () => {
  beforeAll(async () => {
    await database.connect();
    // Clean up test data
    await database.query('DELETE FROM users WHERE email LIKE $1', ['%test%']);
  });

  afterAll(async () => {
    // Clean up test data
    await database.query('DELETE FROM users WHERE email LIKE $1', ['%test%']);
    await database.disconnect();
  });

  describe('POST /api/auth/register', () => {
    test('should register a new user successfully', async () => {
      const userData = {
        username: 'testuser123',
        email: 'test@example.com',
        password: 'TestPass123'
      };

      const response = await request(app)
        .post('/api/auth/register')
        .send(userData)
        .expect(201);

      expect(response.body.success).toBe(true);
      expect(response.body.data.user.username).toBe(userData.username);
      expect(response.body.data.token).toBeDefined();
      expect(response.body.data.user.email).toBe(userData.email);
    });

    test('should reject duplicate username', async () => {
      const userData = {
        username: 'testuser123', // Same as above
        email: 'different@example.com',
        password: 'TestPass123'
      };

      const response = await request(app)
        .post('/api/auth/register')
        .send(userData)
        .expect(409);

      expect(response.body.error).toBe('Registration failed');
    });

    test('should validate password requirements', async () => {
      const userData = {
        username: 'testuser456',
        email: 'test2@example.com',
        password: 'weak' // Too weak
      };

      const response = await request(app)
        .post('/api/auth/register')
        .send(userData)
        .expect(400);

      expect(response.body.error).toBe('Validation failed');
      expect(response.body.messages).toContain('Password must be at least 6 characters long');
    });
  });

  describe('POST /api/auth/login', () => {
    test('should login with valid credentials', async () => {
      const loginData = {
        usernameOrEmail: 'testuser123',
        password: 'TestPass123'
      };

      const response = await request(app)
        .post('/api/auth/login')
        .send(loginData)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.data.token).toBeDefined();
      expect(response.body.data.user.username).toBe('testuser123');
    });

    test('should reject invalid password', async () => {
      const loginData = {
        usernameOrEmail: 'testuser123',
        password: 'wrongpassword'
      };

      const response = await request(app)
        .post('/api/auth/login')
        .send(loginData)
        .expect(401);

      expect(response.body.error).toBe('Login failed');
    });

    test('should reject non-existent user', async () => {
      const loginData = {
        usernameOrEmail: 'nonexistent',
        password: 'TestPass123'
      };

      const response = await request(app)
        .post('/api/auth/login')
        .send(loginData)
        .expect(401);

      expect(response.body.error).toBe('Login failed');
    });
  });

  describe('GET /api/auth/me', () => {
    let authToken;

    beforeAll(async () => {
      // Login to get token
      const loginResponse = await request(app)
        .post('/api/auth/login')
        .send({
          usernameOrEmail: 'testuser123',
          password: 'TestPass123'
        });
      
      authToken = loginResponse.body.data.token;
    });

    test('should return user info with valid token', async () => {
      const response = await request(app)
        .get('/api/auth/me')
        .set('Authorization', `Bearer ${authToken}`)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.data.user.username).toBe('testuser123');
      expect(response.body.data.user.email).toBe('test@example.com');
    });

    test('should reject request without token', async () => {
      const response = await request(app)
        .get('/api/auth/me')
        .expect(401);

      expect(response.body.error).toBe('Access denied');
    });

    test('should reject request with invalid token', async () => {
      const response = await request(app)
        .get('/api/auth/me')
        .set('Authorization', 'Bearer invalid-token')
        .expect(403);

      expect(response.body.error).toBe('Invalid token');
    });
  });

  describe('POST /api/auth/refresh', () => {
    let authToken;

    beforeAll(async () => {
      // Login to get token
      const loginResponse = await request(app)
        .post('/api/auth/login')
        .send({
          usernameOrEmail: 'testuser123',
          password: 'TestPass123'
        });
      
      authToken = loginResponse.body.data.token;
    });

    test('should refresh token with valid token', async () => {
      const response = await request(app)
        .post('/api/auth/refresh')
        .set('Authorization', `Bearer ${authToken}`)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.data.token).toBeDefined();
      expect(response.body.data.token).not.toBe(authToken); // Should be different
    });
  });

  describe('POST /api/auth/logout', () => {
    let authToken;

    beforeAll(async () => {
      // Login to get token
      const loginResponse = await request(app)
        .post('/api/auth/login')
        .send({
          usernameOrEmail: 'testuser123',
          password: 'TestPass123'
        });
      
      authToken = loginResponse.body.data.token;
    });

    test('should logout successfully', async () => {
      const response = await request(app)
        .post('/api/auth/logout')
        .set('Authorization', `Bearer ${authToken}`)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.message).toBe('Logged out successfully');
    });
  });
});

// Add this test to verify JWT secret is working
describe('JWT Configuration', () => {
  test('should have a valid JWT secret configured', () => {
    expect(process.env.JWT_SECRET).toBeDefined();
    expect(process.env.JWT_SECRET.length).toBeGreaterThan(20);
  });

  test('should generate and verify tokens correctly', () => {
    const authService = require('../src/services/authService');
    
    const payload = { userId: 123, username: 'testuser' };
    const token = authService.generateToken(payload);
    
    expect(token).toBeDefined();
    expect(typeof token).toBe('string');
    
    const decoded = authService.verifyToken(token);
    expect(decoded.userId).toBe(payload.userId);
    expect(decoded.username).toBe(payload.username);
  });
});