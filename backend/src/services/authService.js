const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const database = require('../database/connection');

class AuthService {
  constructor() {
    this.jwtSecret = process.env.JWT_SECRET || 'your-secret-key';
    this.jwtExpiresIn = process.env.JWT_EXPIRES_IN || '7d';
    this.saltRounds = 12;
  }

  // Hash password using bcrypt
  async hashPassword(password) {
    try {
      const salt = await bcrypt.genSalt(this.saltRounds);
      return await bcrypt.hash(password, salt);
    } catch (error) {
      throw new Error('Password hashing failed');
    }
  }

  // Compare password with hash
  async comparePassword(password, hash) {
    try {
      return await bcrypt.compare(password, hash);
    } catch (error) {
      throw new Error('Password comparison failed');
    }
  }

  // Generate JWT token
generateToken(payload) {
  try {
    // Add a random nonce to ensure different tokens
    const tokenPayload = {
      ...payload,
      nonce: Math.random().toString(36).substring(2, 15)
    };
    
    return jwt.sign(tokenPayload, this.jwtSecret, { 
      expiresIn: this.jwtExpiresIn,
      issuer: 'geoguessr-clone',
      audience: 'geoguessr-users'
    });
  } catch (error) {
    throw new Error('Token generation failed');
  }
}

  // Verify JWT token
  verifyToken(token) {
    try {
      return jwt.verify(token, this.jwtSecret);
    } catch (error) {
      if (error.name === 'TokenExpiredError') {
        throw new Error('Token expired');
      } else if (error.name === 'JsonWebTokenError') {
        throw new Error('Invalid token');
      } else {
        throw new Error('Token verification failed');
      }
    }
  }

  // Create user in database
  async createUser(userData) {
    const { username, email, password } = userData;
    
    try {
      await database.connect();
      
      // Check if user already exists
      const existingUser = await database.query(
        'SELECT id FROM users WHERE username = $1 OR email = $2',
        [username, email]
      );
      
      if (existingUser.rows.length > 0) {
        throw new Error('User already exists with this username or email');
      }
      
      // Hash password
      const passwordHash = await this.hashPassword(password);
      
      // Insert new user
      const result = await database.query(`
        INSERT INTO users (username, email, password_hash, created_at, last_active)
        VALUES ($1, $2, $3, NOW(), NOW())
        RETURNING id, username, email, created_at
      `, [username, email, passwordHash]);
      
      return result.rows[0];
      
    } catch (error) {
      console.error('❌ User creation failed:', error.message);
      throw error;
    }
  }

  // Authenticate user
  async authenticateUser(loginData) {
    const { usernameOrEmail, password } = loginData;
    
    try {
      await database.connect();
      
      // Find user by username or email
      const result = await database.query(`
        SELECT id, username, email, password_hash, total_score, games_played, created_at, last_active
        FROM users 
        WHERE username = $1 OR email = $1
      `, [usernameOrEmail]);
      
      if (result.rows.length === 0) {
        throw new Error('User not found');
      }
      
      const user = result.rows[0];
      
      // Compare password
      const isValidPassword = await this.comparePassword(password, user.password_hash);
      
      if (!isValidPassword) {
        throw new Error('Invalid password');
      }
      
      // Update last active
      await database.query(
        'UPDATE users SET last_active = NOW() WHERE id = $1',
        [user.id]
      );
      
      // Remove password hash from user object
      delete user.password_hash;
      
      return user;
      
    } catch (error) {
      console.error('❌ User authentication failed:', error.message);
      throw error;
    }
  }

  // Get user by ID
  async getUserById(userId) {
    try {
      await database.connect();
      
      const result = await database.query(`
        SELECT id, username, email, avatar_url, total_score, games_played, created_at, last_active
        FROM users 
        WHERE id = $1
      `, [userId]);
      
      if (result.rows.length === 0) {
        throw new Error('User not found');
      }
      
      return result.rows[0];
      
    } catch (error) {
      console.error('❌ Get user failed:', error.message);
      throw error;
    }
  }

  // Validate input data
  validateRegistrationData(data) {
    const { username, email, password } = data;
    const errors = [];
    
    // Username validation
    if (!username || username.length < 3 || username.length > 50) {
      errors.push('Username must be 3-50 characters long');
    }
    
    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
      errors.push('Username can only contain letters, numbers, and underscores');
    }
    
    // Email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email || !emailRegex.test(email)) {
      errors.push('Valid email address is required');
    }
    
    // Password validation
    if (!password || password.length < 6) {
      errors.push('Password must be at least 6 characters long');
    }
    
    if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(password)) {
      errors.push('Password must contain at least one lowercase letter, one uppercase letter, and one number');
    }
    
    return errors;
  }
}

module.exports = new AuthService();