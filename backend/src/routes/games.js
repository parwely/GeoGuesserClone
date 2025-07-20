const express = require('express');
const router = express.Router();

// Single player game
router.post('/single', (req, res) => {
  res.json({ 
    message: 'Create single player game - to be implemented',
    endpoint: 'POST /api/games/single'
  });
});

// Battle royale routes - fix the route order and syntax
router.post('/battle/create', (req, res) => {
  res.json({ 
    message: 'Create battle royale session - to be implemented',
    endpoint: 'POST /api/games/battle/create'
  });
});

// Fix: Use 'join' as a separate route to avoid conflicts
router.post('/battle/join', (req, res) => {
  const { code } = req.body; // Get code from request body instead of params
  res.json({ 
    message: `Join battle royale ${code || '[no code provided]'} - to be implemented`,
    endpoint: 'POST /api/games/battle/join'
  });
});

// Alternative: If you want to keep the parameter syntax, use a different pattern
router.post('/battle/:code/join', (req, res) => {
  const code = req.params.code;
  res.json({ 
    message: `Join battle royale ${code} - to be implemented`,
    endpoint: 'POST /api/games/battle/:code/join'
  });
});

// Game results - make sure these come after more specific routes
router.get('/:id', (req, res) => {
  const gameId = req.params.id;
  // Add validation to ensure id is numeric
  if (!/^\d+$/.test(gameId)) {
    return res.status(400).json({ error: 'Invalid game ID format' });
  }
  
  res.json({ 
    message: `Get game ${gameId} - to be implemented`,
    endpoint: 'GET /api/games/:id'
  });
});

router.put('/:id/result', (req, res) => {
  const gameId = req.params.id;
  // Add validation to ensure id is numeric
  if (!/^\d+$/.test(gameId)) {
    return res.status(400).json({ error: 'Invalid game ID format' });
  }
  
  res.json({ 
    message: `Update game ${gameId} result - to be implemented`,
    endpoint: 'PUT /api/games/:id/result'
  });
});

module.exports = router;