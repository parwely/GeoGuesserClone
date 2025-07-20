const authService = require('../services/authService');

// Middleware to verify JWT token
const authenticateToken = async (req, res, next) => {
  try {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN
    
    if (!token) {
      return res.status(401).json({ 
        error: 'Access denied',
        message: 'No token provided'
      });
    }
    
    const decoded = authService.verifyToken(token);
    
    // Get fresh user data
    const user = await authService.getUserById(decoded.userId);
    req.user = user;
    
    next();
    
  } catch (error) {
    console.error('❌ Token verification failed:', error.message);
    
    if (error.message === 'Token expired') {
      return res.status(401).json({ 
        error: 'Token expired',
        message: 'Please login again'
      });
    }
    
    return res.status(403).json({ 
      error: 'Invalid token',
      message: 'Please login again'
    });
  }
};

// Middleware to validate request data
const validateRequest = (validationFn) => {
  return (req, res, next) => {
    const errors = validationFn(req.body);
    
    if (errors.length > 0) {
      return res.status(400).json({
        error: 'Validation failed',
        messages: errors
      });
    }
    
    next();
  };
};

module.exports = {
  authenticateToken,
  validateRequest
};