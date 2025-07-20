const express = require('express');
const router = express.Router();
const authService = require('../services/authService');
const { authenticateToken, validateRequest } = require('../middleware/authMiddleware');
const database = require('../database/connection');

// User registration endpoint
router.post('/register', 
  validateRequest(authService.validateRegistrationData),
  async (req, res) => {
    try {
      const { username, email, password } = req.body;
      
      console.log(`🔐 Registration attempt: ${username} (${email})`);
      
      // Create user
      const user = await authService.createUser({ username, email, password });
      
      // Generate JWT token
      const token = authService.generateToken({ 
        userId: user.id, 
        username: user.username 
      });
      
      console.log(`✅ User registered successfully: ${username}`);
      
      res.status(201).json({
        success: true,
        message: 'User registered successfully',
        data: {
          user: {
            id: user.id,
            username: user.username,
            email: user.email,
            createdAt: user.created_at
          },
          token,
          expiresIn: process.env.JWT_EXPIRES_IN || '7d'
        }
      });
      
    } catch (error) {
      console.error('❌ Registration failed:', error.message);
      
      if (error.message.includes('already exists')) {
        return res.status(409).json({
          error: 'Registration failed',
          message: error.message
        });
      }
      
      res.status(500).json({
        error: 'Registration failed',
        message: 'Internal server error'
      });
    }
  }
);

// User login endpoint
router.post('/login', async (req, res) => {
  try {
    const { usernameOrEmail, password } = req.body;
    
    if (!usernameOrEmail || !password) {
      return res.status(400).json({
        error: 'Login failed',
        message: 'Username/email and password are required'
      });
    }
    
    console.log(`🔐 Login attempt: ${usernameOrEmail}`);
    
    // Authenticate user
    const user = await authService.authenticateUser({ usernameOrEmail, password });
    
    // Generate JWT token
    const token = authService.generateToken({ 
      userId: user.id, 
      username: user.username 
    });
    
    console.log(`✅ User logged in successfully: ${user.username}`);
    
    res.json({
      success: true,
      message: 'Login successful',
      data: {
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          totalScore: user.total_score,
          gamesPlayed: user.games_played,
          lastActive: user.last_active
        },
        token,
        expiresIn: process.env.JWT_EXPIRES_IN || '7d'
      }
    });
    
  } catch (error) {
    console.error('❌ Login failed:', error.message);
    
    if (error.message === 'User not found' || error.message === 'Invalid password') {
      return res.status(401).json({
        error: 'Login failed',
        message: 'Invalid username/email or password'
      });
    }
    
    res.status(500).json({
      error: 'Login failed',
      message: 'Internal server error'
    });
  }
});

// Token refresh endpoint
router.post('/refresh', authenticateToken, async (req, res) => {
  try {
    const user = req.user;
    
    // Generate new token
    const token = authService.generateToken({ 
      userId: user.id, 
      username: user.username 
    });
    
    console.log(`🔄 Token refreshed for user: ${user.username}`);
    
    res.json({
      success: true,
      message: 'Token refreshed successfully',
      data: {
        token,
        expiresIn: process.env.JWT_EXPIRES_IN || '7d'
      }
    });
    
  } catch (error) {
    console.error('❌ Token refresh failed:', error.message);
    res.status(500).json({
      error: 'Token refresh failed',
      message: 'Internal server error'
    });
  }
});

// Get current user info
router.get('/me', authenticateToken, async (req, res) => {
  try {
    const user = req.user;
    
    res.json({
      success: true,
      data: {
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          avatarUrl: user.avatar_url,
          totalScore: user.total_score,
          gamesPlayed: user.games_played,
          createdAt: user.created_at,
          lastActive: user.last_active
        }
      }
    });
    
  } catch (error) {
    console.error('❌ Get user info failed:', error.message);
    res.status(500).json({
      error: 'Failed to get user info',
      message: 'Internal server error'
    });
  }
});

// Logout endpoint (client-side token removal)
router.post('/logout', authenticateToken, async (req, res) => {
  try {
    const user = req.user;
    
    // Update last active time
    await database.query(
      'UPDATE users SET last_active = NOW() WHERE id = $1',
      [user.id]
    );
    
    console.log(`👋 User logged out: ${user.username}`);
    
    res.json({
      success: true,
      message: 'Logged out successfully'
    });
    
  } catch (error) {
    console.error('❌ Logout failed:', error.message);
    res.status(500).json({
      error: 'Logout failed',
      message: 'Internal server error'
    });
  }
});

module.exports = router;